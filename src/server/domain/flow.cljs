(ns server.domain.flow
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as hp]
            [server.framework.honeysql :refer [query query-one]]
            [promesa.core :as p :refer-macros [alet]]
            [server.framework.batcher :as b]
            [server.framework.batcher.batch-by-id :refer [->BatchById]]))

(defn create-flow! [conn data]
  (query-one
   conn
   (-> (h/insert-into :flows)
       (h/values [data])
       (hp/returning :*))))

(defn update-flow! [conn flow]
  (let [id (:id flow)
        data (dissoc flow :id)]
    (query-one
     conn
     (-> (h/update :flows)
         (h/sset data)
         (h/where [:= :id id])
         (hp/returning :*)))))

(defn find-by-id [conn id]
  (b/fetch (->BatchById conn (sql/build :select [:id :name :project-id] :from :flows) id)))

(defn find-by-project-id [conn project-id]
  (query
   conn
   (sql/build :select [:id :name :project-id]
              :from :flows
              :where [:= :project-id project-id])))
