(ns server.gql.shields
  (:require [server.framework.pipeline :as pp :refer-macros [shield!]]))

(defn no-current-account! [resolver]
  (shield! [value _ _ context]
    (not (:current-account context))
    resolver))

(defn has-current-account! [resolver]
  (shield! [value _ _ context]
    (boolean (:current-account context))
    resolver))

(defn wrap-resolvers [resolvers mapping]
  (reduce-kv
   (fn [m path shield-fn]
     (update-in m path shield-fn))
   resolvers
   mapping))
