(ns server.framework.bcrypt
  (:require ["bcryptjs" :as bcrypt]
            [promesa.core :as p]
            [oops.core :refer [ocall]]))

;; Ported from FeathersJS https://github.com/feathersjs/authentication-local/blob/master/lib/utils/hash.js

(def bcrypt-work-factor-base 12)
(def bcrypt-date-base 1483228800000)
(def bcrypt-work-increase-interval 47300000000)

(defn hash-password [value]
  (p/promise
   (fn [resolve reject]
     (let [bcrypt-current-date (ocall js/Date :now)
           bcrypt-work-increase (max 0 (ocall js/Math :floor (/ (- bcrypt-current-date bcrypt-date-base) bcrypt-work-increase-interval)))
           bcrypt-work-factor (min 19 (+ bcrypt-work-factor-base bcrypt-work-increase))]
       (ocall bcrypt :genSalt bcrypt-work-factor
              (fn [err salt]
                (if err
                  (reject err)
                  (ocall bcrypt :hash value salt
                         (fn [err hashed-value]
                           (if err
                             (reject err)
                             (resolve hashed-value)))))))))))

(defn verify-password [password-hash password]
  (p/promise
   (fn [resolve reject]
     (ocall bcrypt :compare password password-hash
            (fn [err res]
              (cond
                (boolean err) (resolve false)
                (not res) (resolve false)
                :else (resolve true)))))))
