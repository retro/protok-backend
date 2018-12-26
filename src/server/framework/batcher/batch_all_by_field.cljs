(ns server.framework.batcher.batch-all-by-field
  (:require [server.framework.honeysql :refer [query query-one]]
            [honeysql.helpers :as h]
            [server.framework.batcher.core :refer [IBatch]]))

(defrecord BatchAllByField [conn base-query field value]
  IBatch
  (-batch-key [this]
    [base-query field conn])

  (-unpack [this results]
    (reduce
     (fn [acc res]
       (let [key [field (res field)]
             items (or (acc key) [])]
         (assoc acc key (conj items res))))
     {} results))

  (-entity-key [this]
    [field value])

  (-fetch [this]
    (query conn (h/where base-query [:= field value])))
  
  (-fetch-multi [this recs]
    (let [field-vals (set (map :value recs))]
      (query conn (h/where base-query [:in field field-vals])))))
