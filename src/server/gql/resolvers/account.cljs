(ns server.gql.resolvers.account
  (:require [server.framework.pipeline :as pp :refer-macros [resolver! ts-resolver!]]
            [server.framework.validations :as v]
            [server.framework.jwt :as jwt]
            [server.framework.graphql
             :refer [with-default-resolvers
                     validate-resolver-args!]]
            [server.domain.account :as account]
            [server.gql.shields :as shields]))

(def login-session-mutation-validator 
  (v/to-validator {:account.email [:not-blank :email]
                   :account.password [:not-blank]}))

(def register-session-mutation-validator 
  (v/to-validator {:account.email [:not-blank :email]
                   :account.password [:not-blank]
                   :account.username [:not-blank]}))

(def invalid-login-validator 
  (v/to-validator {:account.email [:invalid]
                   :account.password [:invalid]}))

(defn get-session [account]
  {:account account
   :token (jwt/sign {:data {:account (:id account)}})})

(def current-account
  (shields/has-current-account!
   (resolver! [value parent args context]
     (account/find-by-id (:system/db context) (:current-account context)))))

(def login
  (shields/no-current-account!
   (resolver! [value parent args context info]
     (validate-resolver-args! args login-session-mutation-validator)
     (account/find-by-email-password (:system/db context) (:account args))
     (if value
       (get-session value)
       (validate-resolver-args! args invalid-login-validator)))))

(def register
  (shields/no-current-account!
   (resolver! [value parent args context info]
     (validate-resolver-args! args register-session-mutation-validator)
     (account/create-account! (:system/db context) (:account args))
     (get-session value))))


(def resolvers
  {:query {:current-account current-account}
   :account (-> {}
                (with-default-resolvers :id :email :username))})
