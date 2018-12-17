(ns server.domain.login-code
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as hp]
            [server.framework.honeysql :refer [query query-one]]
            [promesa.core :as p :refer-macros [alet]]
            [server.domain.account :as account]))

(defn create-login-code! [conn account-id]
  (query-one
   conn
   (-> (h/insert-into :login-codes)
       (h/values [{:account-id account-id}])
       (hp/returning :*))))

(defn get-account-from-email-and-login-code! [conn email code]
  (alet [acc (p/await (account/find-by-email conn email :server.domain.account/*))]
    (when acc
      (->> (query-one
            conn
            (-> (h/update :login-codes)
                (h/sset {:is-spent true})
                (h/where [:and
                          [:= :is-spent false]
                          [:= :code code]
                          [:= :account-id (:id acc)]])
                (hp/returning :*)))
           (p/map #(when % acc))))))
