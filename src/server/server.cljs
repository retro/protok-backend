(ns server.server
  (:require [mount.core :refer-macros [defstate]]
            [server.config :refer [env]]
            [oops.core :refer [ocall oget]]
            [taoensso.timbre :as timbre :refer-macros [fatal info]]
            [server.gql.resolvers :refer [resolvers resolve-context]]
            [server.gql.type-defs :refer [type-defs]]
            [server.framework.graphql :refer [default-wrap-resolvers]]
            [server.framework.graphql.context :refer [wrap-context]]
            [server.db :refer [db]]
            ["apollo-server-express" :refer [ApolloServer]]
            ["express" :as express]
            ["compression" :as compression]
            ["express-bearer-token" :as express-bearer-token]
            ["cors" :as cors]))


(defn format-error [e]
  (let [errors (oget e :?extensions.?errors)]
    #js {:path (oget e :path)
         :message (oget e :message)
         :errors errors}))

(defn init-gql-server []
  (ApolloServer. #js {:typeDefs @type-defs
                      :resolvers (default-wrap-resolvers resolvers)
                      :context (wrap-context resolve-context {:system/db @db})
                      :formatError format-error}))


(defn start-server []
  (let [app (express)
        gql-server (init-gql-server)
        port (or (@env :port) 3001)]

    (ocall app :use (compression))
    (ocall app :use (express-bearer-token))
 
    (ocall gql-server :applyMiddleware #js {:app app :path "/graphql" :cors true}) 

    (ocall app :listen port #(info "Server starting on port:" port))))

(defn stop-server [state]
  (when-let [server @state]
    (ocall server :close #(info "Server stopped"))))

(defstate server
  :start (start-server)
  :stop (stop-server server))
