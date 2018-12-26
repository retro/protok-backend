(ns server.gql.type-defs
  (:require [mount.core :refer-macros [defstate]]
            [oops.core :refer [ocall]]
            ["apollo-server-express" :refer [gql]]
            ["fs" :as fs]))

(defn read-type-defs []
  (gql (ocall fs :readFileSync "./resources/schema.graphql" #js {:encoding "utf8"})))

(defstate type-defs
  :start (read-type-defs))
