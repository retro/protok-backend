(ns server.db
  (:require [mount.core :refer-macros [defstate]]
            [server.framework.db :refer [start-pool end-pool]]
            [server.config :refer [env]]))

(defstate db
  :start (start-pool (@env :database-url))
  :stop (end-pool db))
