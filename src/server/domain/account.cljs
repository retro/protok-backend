(ns server.domain.account
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as hp]
            [server.framework.honeysql :refer [query query-one]]
            [promesa.core :as p :refer-macros [alet]]
            [server.framework.bcrypt :as bcrypt]
            [server.framework.batcher :as b]
            [server.framework.batcher.batch-by-id :refer [->BatchById]]))

(defn get-selected-fields [fields]
  (cond
    (= ::* fields) [:id :email :username]
    (vector? fields) fields
    :else [fields]))

(defn create-account! [conn email]
  (query-one
   conn
   (-> (h/insert-into :accounts)
       (h/values [{:email email :username email}])
       (hp/returning :*))))

(defn find-by-id
  ([conn id] (find-by-id conn id :id))
  ([conn id fields]
   (b/fetch (->BatchById conn (sql/build :select (get-selected-fields fields) :from :accounts) id))))

(defn find-by-email
  ([conn email] (find-by-email conn email [:id]))
  ([conn email fields]
   (query-one
    conn
    (sql/build :select (get-selected-fields fields) :from :accounts :where [:= :email email]))))

(defn find-or-create-by-email! [conn email]
  (alet [acc (p/await (find-by-email conn email [:id :email]))]
    (if acc
      acc
      (create-account! conn email))))

