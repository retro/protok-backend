(ns server.domain.flow-node
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as hp]
            [server.framework.honeysql :refer [query query-one sanitize-fields]]
            [promesa.core :as p :refer-macros [alet]]
            [promesa.async-cljs :refer-macros [async]]
            [server.framework.batcher :as b]
            [server.framework.batcher.batch-all-by-field :refer [->BatchAllByField]]
            [server.framework.batcher.batch-by-field :refer [->BatchByField]]
            [server.domain.shared :refer [ensure-selected]]
            [server.domain.queries :refer [make-find-by-id]]
            [server.domain.flow-node.queries :refer [get-node-type]]
            [clojure.string :as str]))

(def join-separator "-00-")

(defn prepare-join-selection [table selection]
  (let [table-name (name table)
        selection' (->> (ensure-selected selection :id)
                        (sanitize-fields table))]
    (mapv (fn [f]
            (let [field-name (name f)]
              [(keyword (str table-name "." field-name)) (str table-name join-separator field-name)]))
          selection')))

(defn concat-selections [& selections]
  (into [] (reduce (fn [acc s] (concat acc s)) [] selections)))

(defn process-node-result [result]
  (when result
    (let [node-attrs     
          (select-keys 
           result 
           [:id
            :is-entrypoint
            :flow-id
            :flow-screen-id
            :flow-event-id
            :flow-switch-id
            :flow-flow-ref-id])

          node-type (get-node-type result)

          children-attrs 
          (reduce-kv
           (fn [m field v]
             (let [field-name (name field)]
               (if (str/includes? field-name join-separator)
                 (let [[table attr] (str/split field-name join-separator)]
                   (assoc-in m [(keyword table) (keyword attr)] v))
                 m)))
           {}
           result)]
      (-> (case node-type
            :event    (:flow-events children-attrs)
            :screen   (:flow-screens children-attrs)
            :switch   (:flow-switches children-attrs)
            :flow-ref (:flow-flow-refs children-attrs)
            {})
          (assoc :type node-type)
          (merge node-attrs)))))

(defn find-base-query [selection]
  (let [all-selections (concat-selections
                        [:flow-nodes.*]
                        (prepare-join-selection :flow-events selection)
                        (prepare-join-selection :flow-screens selection)
                        (prepare-join-selection :flow-switches selection)
                        (prepare-join-selection :flow-flow-refs selection))]
    (sql/build :select all-selections 
               :from :flow-nodes
               :left-join [:flow-events    [:= :flow-nodes.flow-event-id :flow-events.id]
                           :flow-screens   [:= :flow-nodes.flow-screen-id :flow-screens.id]
                           :flow-switches  [:= :flow-nodes.flow-switch-id :flow-switches.id]
                           :flow-flow-refs [:= :flow-nodes.flow-flow-ref-id :flow-flow-refs.id]])))

(defn find-all-by-flow-id
  ([conn flow-id] (find-all-by-flow-id conn flow-id :*))
  ([conn flow-id selection]
   (let [sql (find-base-query selection)]
     (->> (b/fetch (->BatchAllByField conn sql :flow-id flow-id))
          (p/map #(mapv process-node-result %))))))

(defn find-by-id
  ([conn id] (find-by-id conn id :*))
  ([conn id selection]
   (let [sql (find-base-query selection)]
     (->> (b/fetch (->BatchByField conn sql :flow-nodes.id id))
          (p/map process-node-result)))))

(defn delete-by-id!
  ([conn id & args]
   (->> (query-one
         conn
         (-> (h/delete-from :flow-nodes)
             (h/where [:= :id id])
             (hp/returning :id)))
        (p/map boolean))))
