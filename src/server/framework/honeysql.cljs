(ns server.framework.honeysql
  (:require [server.framework.db :as db]
            [honeysql.core :as sql]
            [honeysql-postgres.format]
            [promesa.core :as p]
            [clojure.walk :refer [prewalk]]))

(defn fix-returning-clause [hsql]
  ;; figure out why this is needed
  (prewalk
   (fn [v]
     (if (and (map? v) (:returning v))
       (assoc v :returning (into [] (flatten (:returning v))))
       v))
   hsql))

(defn query
  ([conn hsql] (query conn hsql nil))
  ([conn hsql params]
   (let [hsql' (fix-returning-clause hsql)
         formatted
         (if params
           (sql/format hsql' :parameterizer :postgresql)
           (sql/format hsql' :params params :parameterizer :postgresql))]
     (apply db/query conn formatted))))

(defn query-one
  ([conn hsql] (query-one conn hsql nil))
  ([conn hsql params]
   (->> (query conn hsql params)
        (p/map first))))

(defn sanitize-fields [table fields]
  (if (= :* fields)
    :*
    (let [table-fields (get db/dbschema table)
          fields' (-> (if (vector? fields) fields [fields])
                      (conj :id)
                      set)]
      (filterv #(contains? table-fields %) fields'))))

(defn sanitize-map [table data]
  (select-keys data (get db/dbschema table)))
