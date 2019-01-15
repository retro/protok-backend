(ns server.domain.flow-screen-hotspot
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

(def nested-mapping
  {[:dimensions :width]   :d-width
   [:dimensions :height]  :d-height
   [:coordinates :left]   :c-left
   [:coordinates :right]  :c-right
   [:coordinates :top]    :c-top
   [:coordinates :bottom] :c-bottom})


(defn process-input [input]
  (reduce-kv
   (fn [m k v]
     (if-let [val (get-in m k)]
       (assoc m v val)
       m))
   input
   nested-mapping))

(defn process-output [output]
  (let [nested-mapping-inverse (set/map-invert nested-mapping)]
    (reduce-kv
     (fn [m k v]
       (assoc-in m v (output k)))
     output
     nested-mapping-inverse)))

(defn process-selection [selection]
  (->> (ensure-selected selection :id :flow-screen-id)
       (replace {:coordinates.top    :c-top
                 :coordinates.right  :c-right
                 :coordinates.bottom :c-bottom
                 :coordinates.left   :c-left
                 :dimensions.width   :d-width
                 :dimensions.height  :d-height})
       (sanitize-fields :flow-screen-hotspots)))

(defn create!
  ([conn input] (create! conn input :*))
  ([conn input selection]
   (alet [flow-node-id (:flow-node-id input)
          input' (sanitize-map :flow-screen-hotspots (process-input input))
          flow-screen-id (p/await (flow-node-queries/fetch-flow-screen-id-from-id conn flow-node-id))]
     (when flow-screen-id
       (let [sql (-> (h/insert-into :flow-screen-hotspots)
                     (h/values [(assoc input' :flow-screen-id flow-screen-id)])
                     (hp/returning (process-selection selection)))]
         (->> (query-one conn sql)
              (p/map (fn [result]
                       (-> result
                           process-output
                           (assoc :flow-node-id flow-node-id))))))))))

(defn update-sql [input selection]
  (let [id (:id input)
        input' (dissoc input :id)]
    (-> (h/update :flow-screen-hotspots)
        (h/sset (sanitize-map :flow-screen-hotspots (process-input input)))
        (h/where [:= :id id])
        (hp/returning (process-selection selection)))))

(defn update!
  ([conn input] (update! conn input :*))
  ([conn input selection]
   (alet [updated (p/await (query-one conn (update-sql input selection)))
          flow-screen-id (:flow-screen-id updated)]
     (when updated
       (->> (flow-node-queries/fetch-id-from-flow-screen-id conn flow-screen-id)
            (p/map (fn [flow-node-id]
                     (-> updated
                         process-output
                         (assoc :flow-node-id flow-node-id)))))))))

(defn delete-by-id!
  ([conn id & args]
    (->> (query-one
          conn
          (-> (h/delete-from :flow-screen-hotspots)
              (h/where [:= :id id])
              (hp/returning :id)))
         (p/map boolean))))

(defn find-by-id-sql [id selection]
  (sql/build :select (process-selection selection) 
             :from :flow-screen-hotspots
             :where [:= :id id]))

(defn process-find-selection [selection]
  (->> (ensure-selected selection :id :flow-screen-id)
       process-selection
       (sanitize-fields :flow-screen-hotspots)
       (mapv #(keyword (str "flow-screen-hotspots." (name %))))))

(defn base-find-query [selection]
  (let [all-selections (vec (concat (process-find-selection selection)
                                    [:flow-nodes.flow-screen-id [:flow-nodes.id :flow-node-id]]))]
    (sql/build :select all-selections 
               :from :flow-screen-hotspots
               :left-join [:flow-nodes [:= :flow-screen-hotspots.flow-screen-id :flow-nodes.flow-screen-id]]
               :order-by [:flow-screen-hotspots.c-top :flow-screen-hotspots.c-left])))

(defn find-by-id
  ([conn id] (find-by-id conn id :*))
  ([conn id selection]
   (->> (b/fetch (->BatchByField conn (base-find-query selection) :flow-screen-hotspots.id id))
        (p/map process-output))))

(defn find-all-by-flow-node-id
  ([conn flow-node-id] (find-all-by-flow-node-id conn flow-node-id :*))
  ([conn flow-node-id selection]
   (->> (b/fetch (->BatchAllByField conn (base-find-query selection) [:flow-nodes.id :flow-node-id] flow-node-id))
        (p/map #(mapv process-output %)))))
