(ns server.server
  (:require [mount.core :refer-macros [defstate]]
            [server.config :refer [env]]
            ["graphql-yoga" :refer [GraphQLServer]]
            [oops.core :refer [ocall oget]]
            [taoensso.timbre :as timbre :refer-macros [fatal info]]
            [server.gql.resolvers :refer [resolvers resolve-context]]
            [server.gql.type-defs :refer [type-defs]]
            [server.framework.graphql :refer [wrap-resolvers]]
            [server.framework.graphql.context :refer [wrap-context]]
            [server.db :refer [db]]
            ["express-bearer-token" :as express-bearer-token]
            ["cors" :as cors]))

(defn start-server []
  (let [ref (volatile! nil)
        server (GraphQLServer.
                #js {:typeDefs @type-defs
                     :resolvers (wrap-resolvers resolvers)
                     :context (wrap-context resolve-context
                                            {:system/db @db})})
        port (or (@env :port) 3001)
        endpoint "/graphql"
        playground "/playground"]

    (ocall server :express.use (express-bearer-token))
    (ocall server :express.use (cors))
    
    (-> (ocall server :start #js {:port port
                                  :endpoint endpoint
                                  :playground playground 
                                  :formatError (fn [e]
                                                 (let [errors (oget e :?extensions.?errors)]
                                                   #js {:path (oget e :path)
                                                        :message (oget e :message)
                                                        :errors errors}))})
        (ocall :then
               (fn [s]
                 (info "Server started on port:" port)
                 (vreset! ref s))
               #(fatal %)))
    ref))

(defn stop-server [state]
  (when-let [s @state]
    (when-let [r @s]
      (ocall r :close #(info "Server Stopped")))))

(defstate server
  :start (start-server)
  :stop (stop-server server))
