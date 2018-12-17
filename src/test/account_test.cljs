(ns test.account-test
  (:require [cljs.test :refer [deftest is async]]
            [server.db :refer [db]]
            [promesa.core :as p]
            [test.helpers :refer-macros [defasynctest]]
            [ajax.core :refer [POST]]))
(def q
"mutation {
  register(account: {email: \"konjevic@gmail.com\", password: \"1234567890\"}) {
    token
    account {
      id
      email
    }
  }
}
")

(defn make-req []
  (p/promise
   (fn [resolve reject]
     (POST "http://localhost:3000/graphql"
           {:response-format :json
            :format :json
            :params {:operationName nil
                     :variables {}
                     :query q}
            :handler #(resolve %)
            :error-handler #(reject %)}))))

(defasynctest bar
  (make-req)
  (println "A")
  (is (= 2 3))) 
