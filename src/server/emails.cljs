(ns server.emails
  (:require ["mjml" :as mjml]
            ["html-to-text" :as html-to-text]
            [oops.core :refer [ocall oget+]]
            [hiccups.runtime])
  (:require-macros [hiccups.core :as hiccups :refer [html]]))

(defn prepare-email [props body]
  (let [h (oget+ (mjml (html body)) :?html)
        t (ocall html-to-text :fromString h)]
    (assoc props
           :html h
           :text t)))

(defn create-request-login-code [email code]
  (prepare-email
   {:to email
    :from "app@protok.app"
    :subject "Protok Login Code"}
   [:mjml
    [:mj-body
     [:mj-section
      [:mj-column
       [:mj-text "Your login code is " code]]]]]))
