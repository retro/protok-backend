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
   (resolver! [value parent args context info]
     (account/find-by-id (:system/db context) (:current-account context) (:selection info)))))

(def account-from-organization-membership
  (resolver! [value parent args context info]
    (account/find-by-id (:system/db context) (:account-id parent))))

(def resolvers
  {:query {:current-account current-account}
   :organization-membership {:account account-from-organization-membership}
   :account (-> {}
                (with-default-resolvers :id :email :username))})
