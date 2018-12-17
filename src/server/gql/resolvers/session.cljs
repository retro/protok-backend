(ns server.gql.resolvers.session
  (:require [server.framework.pipeline :as pp :refer-macros [resolver! ts-resolver!]]
            [server.framework.validations :as v]
            [server.framework.jwt :as jwt]
            [server.framework.graphql
             :refer [with-default-resolvers
                     validate!]]
            [server.domain.account :as account]
            [server.domain.session :as session]
            [server.emails :as emails]
            [server.emailer :as emailer]
            [server.gql.shields :as shields]
            [promesa.core :as p]))

(def request-login-code-validator 
  (v/to-validator {:email [:not-blank :email]}))

(def login-with-code-validator
  (v/to-validator {:email [:not-blank :email]
                   :code [:not-blank]}))

(def invalid-login-with-code-validator
  (v/to-validator {:email [:invalid]
                   :code [:invalid]}))

(defn get-session [account]
  {:account account
   :token (jwt/sign {:data {:account (:id account)}})})

(def request-login-code 
  (shields/no-current-account!
   (resolver! [value parent args context info]
     (validate! args request-login-code-validator)
     (->> (account/find-or-create-by-email! (:system/db context) (:email args))
          (p/map (fn [acc] {:account acc})))
     (->> (session/create-login-code! (:system/db context) (get-in value [:account :id]))
          (p/map #(assoc value :code (:code %))))
     (emailer/send-email! (emails/create-request-login-code (get-in value [:account :email]) (:code value)))
     (rescue! [error]
       false))))

(def login-with-code
  (shields/no-current-account!
   (resolver! [value parent args context info]
     (validate! args login-with-code-validator)
     (session/get-account-from-email-and-login-code! (:system/db context) (:email args) (:code args))
     (if value
       (get-session value)
       (validate! {} invalid-login-with-code-validator)))))

(def resolvers
  {:session  (with-default-resolvers :token :account)
   :mutation {:request-login-code request-login-code
              :login-with-code login-with-code}})
