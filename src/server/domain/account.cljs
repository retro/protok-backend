(ns server.domain.account
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as hp]
            [server.framework.honeysql :refer [query query-one]]
            [promesa.core :as p :refer-macros [alet]]
            [server.framework.bcrypt :as bcrypt]
            [server.framework.batcher :as b]
            [server.framework.batcher.batch-by-id :refer [->BatchById]]))

(defn create-account! [conn user-data]
  (alet [{:keys [email password]} user-data
         hashed-password (p/await (bcrypt/hash-password password))]
    (query-one
     conn
     (-> (h/insert-into :accounts)
         (h/values [{:email email :password hashed-password}])
         (hp/returning :*)))))

(defn find-by-email-password [conn {:keys [email password]}]
  (alet [acc (p/await (query-one
                       conn
                       (sql/build :select [:id :email :password :username] :from :accounts :where [:= :email email])))
         password-valid? (p/await (bcrypt/verify-password (:password acc) password))]
    (when password-valid?
      (dissoc acc :password))))

(defn find-by-id [conn id]
  (b/fetch (->BatchById conn (sql/build :select [:id :email :username] :from :accounts) id)))
