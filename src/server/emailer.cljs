(ns server.emailer
  (:require [mount.core :refer-macros [defstate]]
            [cljs.core.async :refer [chan close! <! alts! put!]]
            [server.config :refer [env]]
            [taoensso.timbre :as timbre :refer-macros [fatal info]]
            ["@sendgrid/mail" :as sendgrid]
            ["preview-email" :as preview-email]
            [oops.core :refer [ocall]])
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]))

(defn start-emailer []
  (ocall sendgrid :setApiKey (:sendgrid-api-key @env))
  (let [emailer-ch (chan)]
    (go-loop []
      (let [email (<! emailer-ch)]
        (when email
          (if (= "production" (:node-env @env))
            (ocall sendgrid :send (clj->js email))
            (preview-email (clj->js email)))
          (recur))))
    emailer-ch))

(defn stop-emailer [emailer]
  (close! @emailer))

(defstate emailer
  :start (start-emailer)
  :stop (stop-emailer emailer))

(defn send-email! [props]
  (put! @emailer props))
