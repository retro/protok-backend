(ns server.domain.account
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as hp]
            [server.framework.honeysql :refer [query query-one]]
            [promesa.core :as p :refer-macros [alet]]
            [server.domain.queries :refer [make-create make-update make-find-by-id]]
            [server.framework.batcher :as b]))

(def create! (make-create :accounts))
(def update! (make-update :accounts))
(def find-by-id (make-find-by-id :accounts))

(def sanitize-fields (partial server.framework.honeysql/sanitize-fields :accounts))

(defn find-by-email
  ([conn email] (find-by-email conn email :id))
  ([conn email selection]
   (query-one
    conn
    (sql/build :select (sanitize-fields selection) :from :accounts :where [:= :email email]))))

(defn find-or-create-by-email!
  ([conn email] (find-or-create-by-email! conn email [:id :email]))
  ([conn email selection]
   (alet [acc (p/await (find-by-email conn email selection))]
     (if acc
       acc
       (create! conn {:email email :username email} selection)))))

