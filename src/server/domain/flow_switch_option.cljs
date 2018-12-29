(ns server.domain.flow-switch-option
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as hp]
            [server.framework.honeysql :refer [query query-one sanitize-fields sanitize-map]]
            [promesa.core :as p :refer-macros [alet]]
            [server.framework.batcher :as b]
            [server.framework.batcher.batch-all-by-field :refer [->BatchAllByField]]
            [server.framework.batcher.batch-by-field :refer [->BatchByField]]
            [server.domain.shared :refer [ensure-selected]]
            [server.domain.flow-node.queries :as flow-node-queries]
            [clojure.set :as set]))

(defn process-selection [selection]
  (->> (ensure-selected selection :id :flow-switch-id)
       (sanitize-fields :flow-switch-options)))

(defn create!
  ([conn input] (create! conn input :*))
  ([conn input selection]
   (alet [flow-node-id (:flow-node-id input)
          input' (sanitize-map :flow-switch-options input)
          flow-switch-id (p/await (flow-node-queries/fetch-flow-switch-id-from-id conn flow-node-id))]
     (when flow-switch-id
       (let [sql (-> (h/insert-into :flow-switch-options)
                     (h/values [(assoc input' :flow-switch-id flow-switch-id)])
                     (hp/returning (process-selection selection)))]
         (->> (query-one conn sql)
              (p/map (fn [result]
                       (-> result
                           (assoc :flow-node-id flow-node-id))))))))))

(defn update-sql [input selection]
  (let [id (:id input)
        input' (dissoc input :id)]
    (-> (h/update :flow-switch-options)
        (h/sset (sanitize-map :flow-switch-options input))
        (h/where [:= :id id])
        (hp/returning (process-selection selection)))))

(defn update!
  ([conn input] (update! conn input :*))
  ([conn input selection]
   (alet [updated (p/await (query-one conn (update-sql input selection)))
          flow-switch-id (:flow-switch-id updated)]
     (when updated
       (->> (flow-node-queries/fetch-id-from-flow-switch-id conn flow-switch-id)
            (p/map (fn [flow-node-id]
                     (-> updated
                         (assoc :flow-node-id flow-node-id)))))))))

(defn delete-by-id!
  ([conn id & args]
    (->> (query-one
          conn
          (-> (h/delete-from :flow-switch-options)
              (h/where [:= :id id])
              (hp/returning :id)))
         (p/map boolean))))

(defn find-by-id-sql [id selection]
  (sql/build :select (process-selection selection) 
             :from :flow-switch-options
             :where [:= :id id]))

(defn process-find-selection [selection]
  (->> (ensure-selected selection :id :flow-switch-id)
       process-selection
       (sanitize-fields :flow-switch-options)
       (mapv #(keyword (str "flow-switch-options." (name %))))))

(defn base-find-query [selection]
  (let [all-selections (vec (concat (process-find-selection selection)
                                    [:flow-nodes.flow-switch-id [:flow-nodes.id :flow-node-id]]))]
    (sql/build :select all-selections 
               :from :flow-switch-options
               :left-join [:flow-nodes [:= :flow-switch-options.flow-switch-id :flow-nodes.flow-switch-id]])))

(defn find-by-id
  ([conn id] (find-by-id conn id :*))
  ([conn id selection]
   (b/fetch (->BatchByField conn (base-find-query selection) :flow-switch-options.id id))))

(defn find-all-by-flow-node-id
  ([conn flow-node-id] (find-all-by-flow-node-id conn flow-node-id :*))
  ([conn flow-node-id selection]
   (b/fetch (->BatchAllByField conn (base-find-query selection) [:flow-nodes.id :flow-node-id] flow-node-id))))
