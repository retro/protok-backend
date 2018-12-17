(ns server.framework.batcher.batch-by-id
  (:require [server.framework.honeysql :refer [query query-one]]
            [honeysql.helpers :as h]
            [server.framework.batcher.core :refer [IBatch]]))

(defrecord BatchById [conn base-query id]
  IBatch
  (-batch-key [this]
    [base-query conn])

  (-unpack [this results]
    (reduce
     (fn [acc res]
       (assoc acc (:id res) res))
     {} results))

  (-entity-key [this]
    id)

  (-fetch [this]
    (query-one conn (h/where base-query [:= :id id])))
  
  (-fetch-multi [this recs]
    (let [ids (set (map :id recs))]
      (query conn (h/where base-query [:in :id ids])))))
