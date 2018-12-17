(ns server.gql.resolvers.organization
  (:require [server.framework.pipeline :as pp :refer-macros [resolver! ts-resolver!]]
            [server.framework.validations :as v]
            [server.framework.jwt :as jwt]
            [server.framework.graphql
             :refer [with-default-resolvers
                     validate!]]
            [server.domain.organization :as organization]
            [server.gql.shields :as shields]))

(def create-organization-validator
  (v/to-validator {:name [:not-blank]}))

(def create-organization
  (shields/has-current-account!
   (ts-resolver! [value parent args context]
     (validate! (:organization args) create-organization-validator)
     (organization/create-organization! (:system/db context) (get-in args [:organization :name]))
     (pp/mute! (organization/create-organization-member! (:system/db context) (:id value) (:current-account context) "owner")))))

(def resolvers
  {:organization (with-default-resolvers :id :name)
   :mutation {:create-organization create-organization}})
