(ns server.domain.project
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as hp]
            [server.framework.honeysql :refer [query query-one sanitize-fields]]
            [promesa.core :as p :refer-macros [alet]]
            [server.domain.queries :refer [make-find-by-id]]))

(def find-by-id (make-find-by-id :projects))

(defn find-by-organization-id
  ([conn organization-id] (find-by-organization-id conn organization-id :*))
  ([conn organization-id selection]
   (query
    conn
    (sql/build :select (sanitize-fields :projects selection) 
               :from :projects
               :where [:= :organization-id organization-id]))))
