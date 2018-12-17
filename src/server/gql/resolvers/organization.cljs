(ns server.gql.resolvers.organization
  (:require [server.framework.pipeline :as pp :refer-macros [resolver! ts-resolver!]]
            [server.framework.validations :as v]
            [server.framework.jwt :as jwt]
            [server.framework.graphql
             :refer [with-default-resolvers
                     validate!]]
            [server.domain.organization :as organization]
            [server.gql.shields :as shields]
            [camel-snake-kebab.core :refer [->SCREAMING_SNAKE_CASE]]))

(def create-organization-validator
  (v/to-validator {:name [:not-blank]}))

(def create-organization
  (shields/has-current-account!
   (ts-resolver! [value parent args context]
     (validate! (:organization args) create-organization-validator)
     (organization/create-organization! (:system/db context) (get-in args [:organization :name]))
     (pp/sideffect! (organization/create-organization-member! (:system/db context) (:id value) (:current-account context) "owner")))))

(def organization-memberships
  (shields/has-current-account!
   (resolver! [value parent args context]
     (organization/find-organization-memberships (:system/db context) (:current-account context)))))

(def organization-from-membership
  (shields/has-current-account!
   (resolver! [value parent args context]
     (organization/find-by-id (:system/db context) (:organization-id parent)))))

(def member-role
  (shields/has-current-account!
   (resolver! [value parent args context]
     (->SCREAMING_SNAKE_CASE (:member-role parent)))))

(def resolvers
  {:organization            (with-default-resolvers :id :name)
   :account                 {:organization-memberships organization-memberships}
   :organization-membership {:organization organization-from-membership
                             :member-role  member-role}
   :mutation                {:create-organization create-organization}})

