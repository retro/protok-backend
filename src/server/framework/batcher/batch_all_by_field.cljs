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
       (let [[_ field-name] (if (vector? field) field [nil field])
             key [field-name (res field-name)]
             items (or (acc key) [])]
         (assoc acc key (conj items res))))
     {} results))

  (-entity-key [this]
    (let [[_ field-name] (if (vector? field) field [nil field])]
      [field-name value]))
  
  (-fetch [this]
    (let [[field-path] (if (vector? field) field [field])]
      (query conn (h/where base-query [:= field-path value]))))
  
  (-fetch-multi [this recs]
    (let [values (set (map :value recs))
          [field-path] (if (vector? field) field [field])]
      (query conn (h/where base-query [:in field-path values])))))
