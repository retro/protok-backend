(ns test.core-test
  (:require [shadow.test.node]
            [mount.core :as mount]
            [server.server]))

(mount/in-cljc-mode)

(defn main []
  (mount/start)
  (shadow.test.node/main))
