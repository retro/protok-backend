(ns server.gql.resolvers.account
  (:require [server.framework.pipeline :as pp :refer-macros [resolver! ts-resolver!]]
            [server.framework.validations :as v]
            [server.framework.jwt :as jwt]
            [server.framework.graphql
             :refer [with-default-resolvers
                     validate!]]
            [server.domain.account :as account]
            [server.gql.shields :as shields]))

(def current-account
  (shields/has-current-account!
   (resolver! [value parent args context]
     (account/find-by-id (:system/db context) (:current-account context) :server.domain.account/*))))

(def resolvers
  {:query {:current-account current-account}
   :account (-> {}
                (with-default-resolvers :id :email :username))})
