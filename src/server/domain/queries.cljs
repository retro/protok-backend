(ns server.domain.queries
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as hp]
            [server.framework.honeysql :refer [query query-one sanitize-map sanitize-fields]]
            [promesa.core :as p :refer-macros [alet]]
            [server.framework.batcher :as b]
            [server.framework.batcher.batch-by-field :refer [->BatchByField]]))

(defn make-create [table]
  (fn create!
    ([conn input] (create! conn input :*))
    ([conn input selection]
       (query-one
        conn
        (-> (h/insert-into table)
            (h/values [(-> (sanitize-map table input)
                           (dissoc :id))])
            (hp/returning (sanitize-fields table selection)))))))

(defn make-update [table]
  (fn update!
    ([conn input] (update! conn input :*))
    ([conn input selection]
       (let [id (:id input)]
         (query-one
          conn
          (-> (h/update table)
              (h/sset (-> (sanitize-map table input)
                          (dissoc :id)))
              (h/where [:= :id id])
              (hp/returning (sanitize-fields table selection))))))))

(defn make-find-by-id [table]
  (fn find-by-id 
    ([conn id] (find-by-id conn id :*))
    ([conn id selection]
      (b/fetch (->BatchByField conn (sql/build :select (sanitize-fields table selection) :from table) :id id)))))

(defn make-delete-by-id [table]
  (fn delete-by-id!
    ([conn id] (delete-by-id! conn id :*))
    ([conn id selection]
     (query-one
      conn
      (-> (h/delete-from table)
          (h/where [:= :id id])
          (hp/returning (sanitize-fields table selection)))))))
