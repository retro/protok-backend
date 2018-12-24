(ns server.domain.organization
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as hp]
            [server.framework.honeysql :refer [query query-one]]
            [promesa.core :as p :refer-macros [alet]]
            [server.framework.batcher :as b]
            [server.framework.batcher.batch-by-id :refer [->BatchById]]))

(defn create-organization! [conn {:keys [name]}]
  (query-one
   conn
   (-> (h/insert-into :organizations)
       (h/values [{:name name}])
       (hp/returning :*))))

(defn update-organization! [conn organization]
  (let [id (:id organization)
        data (dissoc organization :id)]
    (query-one
     conn
     (-> (h/update :organizations)
         (h/sset data)
         (h/where [:= :id id])
         (hp/returning :*)))))

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

(defn find-organization-memberships [conn account-id]
  (query
   conn
   (sql/build :select [:organization-id :member-role]
              :from :organization-members
              :where [:= :account-id account-id])))

(defn find-organization-membership [conn organization-id account-id]
  (query-one
   conn
   (sql/build :select [:member-role]
              :from :organization-members
              :where [:and
                      [:= :account-id account-id]
                      [:= :organization-id organization-id]])))

(defn find-by-id [conn id]
  (b/fetch (->BatchById conn (sql/build :select [:id :name] :from :organizations) id)))
