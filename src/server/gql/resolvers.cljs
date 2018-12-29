(ns server.gql.resolvers
  (:require [server.framework.pipeline :as pp :refer-macros [resolver! pipeline!]]
            [server.framework.graphql :refer [with-default-resolvers validate!]]
            [promesa.core :as p]
            [server.db :refer [db]]
            [server.gql.shields :as shields]
            [server.framework.jwt :as jwt]
            [server.framework.util :refer [deep-merge]]
            [server.gql.resolvers.account]
            [server.gql.resolvers.session]
            [server.gql.resolvers.organization]
            [server.gql.resolvers.project]
            [server.gql.resolvers.flow]
            [server.gql.resolvers.flow-node]
            [server.gql.resolvers.flow-screen-hotspot]
            [server.gql.resolvers.flow-switch-option]))

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
    :mutation {}}
   
   server.gql.resolvers.account/resolvers
   server.gql.resolvers.session/resolvers
   server.gql.resolvers.organization/resolvers
   server.gql.resolvers.project/resolvers
   server.gql.resolvers.flow/resolvers
   server.gql.resolvers.flow-node/resolvers
   server.gql.resolvers.flow-screen-hotspot/resolvers
   server.gql.resolvers.flow-switch-option/resolvers))

