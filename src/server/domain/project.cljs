(ns server.domain.project
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as hp]
            [server.framework.honeysql :refer [query query-one]]
            [promesa.core :as p :refer-macros [alet]]
            [server.framework.batcher :as b]
            [server.framework.batcher.batch-by-id :refer [->BatchById]]))

(defn create-project! [conn data]
  (query-one
   conn
   (-> (h/insert-into :projects)
       (h/values [data])
       (hp/returning :*))))

(defn update-project! [conn project]
  (let [id (:id project)
        data (dissoc project :id)]
    (query-one
     conn
     (-> (h/update :projects)
         (h/sset data)
         (h/where [:= :id id])
         (hp/returning :*)))))

(defn find-by-id [conn id]
  (b/fetch (->BatchById conn (sql/build :select [:id :name :organization-id] :from :projects) id)))

(defn find-by-organization-id [conn organization-id]
  (query
   conn
   (sql/build :select [:id :name :organization-id]
              :from :projects
              :where [:= :organization-id organization-id])))
