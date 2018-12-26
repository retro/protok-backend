(ns server.framework.jwt
  (:require ["jsonwebtoken" :as jwt]
            [server.config :refer [env]]
            [oops.core :refer [ocall oget]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [camel-snake-kebab.core :refer [->kebab-case-keyword ->camelCaseString]]
            [clojure.walk :refer [postwalk]]))

(defn verify [token]
  (->> (js->clj (ocall jwt :verify token (@env :jwt-signing-key)))
       (transform-keys ->kebab-case-keyword)))

(defn sign [payload]
  (ocall jwt :sign
         (clj->js (transform-keys ->camelCaseString payload))
         (@env :jwt-signing-key)
         #js {:expiresIn "2w"}))

(defn get-from-context [context]
  (oget context :?req.?token))
