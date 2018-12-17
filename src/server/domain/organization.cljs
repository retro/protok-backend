(ns server.domain.organization
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as hp]
            [server.framework.honeysql :refer [query query-one]]
            [promesa.core :as p :refer-macros [alet]]
            [server.framework.batcher :as b]
            [server.framework.batcher.batch-by-id :refer [->BatchById]]))

(defn create-organization! [conn name]
  (query-one
   conn
   (-> (h/insert-into :organizations)
       (h/values [{:name name}])
       (hp/returning :*))))

(defn create-organization-member!
  ([conn organization-id account-id]
   (create-organization-member! conn organization-id account-id "member"))
  ([conn organization-id account-id member-role]
   (query-one
    conn
    (-> (h/insert-into :organization-members)
        (h/values [{:account-id account-id
                    :organization-id organization-id
                    :member-role member-role}])
        (hp/returning :*)))))
