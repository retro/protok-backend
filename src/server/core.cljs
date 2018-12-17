(ns server.core
  (:require [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as timbre :refer-macros [fatal info]]
            [server.server]))

(mount/in-cljc-mode)

(defn stop []
  (info "Stopping")
  (mount/stop))

(defn start []
  (info "Starting")
  (mount/start))

(defn main []
  (start))
