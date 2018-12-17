(ns server.gql.type-defs
  (:require [mount.core :refer-macros [defstate]]
            [oops.core :refer [ocall]]
            ["fs" :as fs]))

(defn read-type-defs []
  (ocall fs :readFileSync "./resources/schema.graphql" #js {:encoding "utf8"}))

(defstate type-defs
  :start (read-type-defs))
