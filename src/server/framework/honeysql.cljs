(ns server.framework.honeysql
  (:require [server.framework.db :as db]
            [honeysql.core :as sql]
            [honeysql-postgres.format]
            [promesa.core :as p]))

(defn query
  ([conn hsql] (query conn hsql nil))
  ([conn hsql params]
   (let [formatted
         (if params
           (sql/format hsql :parameterizer :postgresql)
           (sql/format hsql :params params :parameterizer :postgresql))]
     (apply db/query conn formatted))))

(defn query-one
  ([conn hsql] (query-one conn hsql nil))
  ([conn hsql params]
   (->> (query conn hsql params)
        (p/map first))))
