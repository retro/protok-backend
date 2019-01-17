(ns server.domain.organization
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as hp]
            [server.framework.honeysql :refer [query query-one sanitize-map sanitize-fields]]
            [promesa.core :as p :refer-macros [alet]]
            [server.domain.queries :refer [make-create make-update make-find-by-id]]
            [server.domain.account :as account]))

(def create! (make-create :organizations))
(def find-by-id (make-find-by-id :organizations))

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

(defn create-organization-member-by-email!
  ([conn organization-id email]
   (alet [account (p/await (account/find-or-create-by-email! conn email))]
     (create-organization-member! conn organization-id (:id account)))))

(defn find-account-organization-memberships [conn account-id]
  (query
   conn
   (sql/build :select [:organization-id :member-role]
              :from :organization-members
              :where [:= :account-id account-id])))

(defn find-organization-membership [conn organization-id account-id]
  (query-one
   conn
   (sql/build :select [:member-role :account-id]
              :from :organization-members
              :where [:and
                      [:= :account-id account-id]
                      [:= :organization-id organization-id]])))

(defn find-organization-memberships [conn organization-id]
  (query
   conn
   (sql/build :select [:member-role :account-id]
              :from :organization-members
              :where [:= :organization-id organization-id])))

(defn can-update? [conn organization-id account-id]
  (when (and organization-id account-id)
    (alet [membership (p/await (find-organization-membership conn organization-id account-id))]
      (contains? #{"owner" "admin"} (:member-role membership)))))

(defn can-delete? [conn organization-id account-id]
  (when (and organization-id account-id)
    (alet [membership (p/await (find-organization-membership conn organization-id account-id))]
      (= "owner" (:member-role membership)))))

(defn can-access? [conn organization-id account-id]
  (when (and organization-id account-id)
    (alet [membership (p/await (find-organization-membership conn organization-id account-id))]
      (boolean membership))))
