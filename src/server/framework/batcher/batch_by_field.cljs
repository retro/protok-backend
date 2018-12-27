(ns server.framework.batcher.batch-by-field
  (:require [server.framework.honeysql :refer [query query-one]]
            [honeysql.helpers :as h]
            [server.framework.batcher.core :refer [IBatch]]))

(defrecord BatchByField [conn base-query field value]
  IBatch
  (-batch-key [this]
    [base-query field conn])

  (-unpack [this results]
    (reduce
     (fn [acc res]
       (assoc acc [field (res field)] res))
     {} results))

  (-entity-key [this]
    [field value])

  (-fetch [this]
    (query-one conn (h/where base-query [:= field value])))
  
  (-fetch-multi [this recs]
    (let [values (set (map :value recs))]
      (query conn (h/where base-query [:in field values])))))
