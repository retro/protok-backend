(ns server.gql.resolvers
  (:require [server.framework.pipeline :as pp :refer-macros [resolver! pipeline!]]
            [server.framework.graphql :refer [with-default-resolvers validate-resolver-args!]]
            [promesa.core :as p]
            [server.db :refer [db]]
            [server.gql.shields :as shields]
            [server.framework.jwt :as jwt]
            [server.framework.util :refer [deep-merge]]
            [server.gql.resolvers.account]
            [server.gql.resolvers.session]))

(def resolve-context
  (pipeline! [value ctx]
    (jwt/get-from-context ctx)
    (if value
      (pipeline! [value ctx]
        (jwt/verify value)
        {:current-account (get-in value [:data :account])})
      {})
    (rescue! [error]
      {})))


(def resolvers
  (deep-merge
   {:query    {}
    :mutation {}
    :session  (with-default-resolvers :token :account)}
   
   server.gql.resolvers.account/resolvers
   server.gql.resolvers.session/resolvers))
