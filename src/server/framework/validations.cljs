(ns server.framework.validations
  (:require [clojure.string :as str]
            [server.framework.validator :as v]
            [oops.core :refer [ocall]]
            [clojure.walk :refer [postwalk]]))

(def email-regex #"^([^\x00-\x20\x22\x28\x29\x2c\x2e\x3a-\x3c\x3e\x40\x5b-\x5d\x7f-\xff]+|\x22([^\x0d\x22\x5c\x80-\xff]|\x5c[\x00-\x7f])*\x22)(\x2e([^\x00-\x20\x22\x28\x29\x2c\x2e\x3a-\x3c\x3e\x40\x5b-\x5d\x7f-\xff]+|\x22([^\x0d\x22\x5c\x80-\xff]|\x5c[\x00-\x7f])*\x22))*\x40([^\x00-\x20\x22\x28\x29\x2c\x2e\x3a-\x3c\x3e\x40\x5b-\x5d\x7f-\xff]+|\x5b([^\x0d\x5b-\x5d\x80-\xff]|\x5c[\x00-\x7f])*\x5d)(\x2e([^\x00-\x20\x22\x28\x29\x2c\x2e\x3a-\x3c\x3e\x40\x5b-\x5d\x7f-\xff]+|\x5b([^\x0d\x5b-\x5d\x80-\xff]|\x5c[\x00-\x7f])*\x5d))*$")

(def url-regex #"https?://(www\.)?[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\b([-a-zA-Z0-9@:%_\+.~#?&//=]*)")

(def states
  #{"AL", "AK", "AS", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FL", "GA", "GU", "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD", "MH", "MA", "MI", "FM", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "MP", "OH", "OK", "OR", "PW", "PA", "PR", "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VA", "VI", "WA", "WV", "WI", "WY"})


(defn zero-count? [v]
  (if (satisfies? ICounted v)
    (zero? (count v))
    false))

(defn not-blank? [v _ _]
  (cond
    (nil? v) false
    (= "" v) false
    (zero-count? v) false
    :else true))

(defn url? [v _ _]
  (not (nil? (re-matches url-regex (str v)))))

(defn email? [v _ _] 
  (not (nil? (re-matches email-regex (str v)))))

(defn bool? [v _ _]
  (if (nil? v)
    true
    (or (= true v) (= false v))))

(defn numeric? [v _ _]
  (if (nil? v)
    true
    (re-matches #"^\d+$" v)))

(defn ok-password? [v _ _]
  (if (seq v)
    (< 7 (count v))
    true))

(defn valid-state? [v _ _]
  (if (seq v)
    (contains? states (str/upper-case v))
    true))

(defn valid-zipcode? [v _ _]
  (if (seq v)
    (not (nil? (re-matches #"(^\d{5}$)|(^\d{5}-\d{4}$)" (str v))))
    true))

(defn make-confirmation-validator [path1 path2]
  (fn [_ data _]
    (let [val1 (get-in data (flatten [path1]))
          val2 (get-in data (flatten [path2]))]
      (if (some nil? [val1 val2])
        true
        (= val1 val2)))))

(def password-confirmed? (make-confirmation-validator :password :password-confirmation))
(def email-confirmed? (make-confirmation-validator :email :email-confirmation))

(def invalid (constantly false))

(def default-validations
  {:not-blank             {:message   "Value can't be blank"
                           :validator not-blank?}
   :bool                  {:message   "Value must be true or false"
                           :validator bool?}
   :url                   {:message   "Value is not a valid URL"
                           :validator url?}
   :email                 {:message   "Value is not a valid email"
                           :validator email?} 
   :email-confirmation    {:message   "Email doesn't match email confirmation"
                           :validator email-confirmed?}
   :password-confirmation {:message   "Passwords don't match"
                           :validator password-confirmed?}
   :ok-password           {:message   "Password must have at least 8 characters"
                           :validator ok-password?}
   :numeric               {:message   "Value is not a number"
                           :validator numeric?}
   :valid-state           {:message   "Not a valid US state"
                           :validator valid-state?}
   :valid-zipcode         {:message   "Not a valid Zipcode"
                           :validator valid-zipcode?}
   :invalid               {:message   "Invalid value was entered"
                           :validator invalid}})

(def validations$ (atom default-validations))

(defn register-validation! [key validator]
  (swap! validations$ assoc key validator))

(defn get-validator-message [validation-key]
  (or (get-in @validations$ [validation-key :message])
      "Value failed validation."))

(defn to-validator
  "Helper function that extracts the validator definitions."
  [config]
  (let [v-fn (v/validator
              (reduce-kv
               (fn [m attr v]
                 (let [vs (map (fn [k] 
                                 (if-let [validator (get-in @validations$ [k :validator])]
                                   [k validator]
                                   (throw (ex-info (str "Validator " k " doesn't exist") {:validator k})))) v)]
                   (assoc m attr vs))) 
               {} config))]
    (fn [data]
      (let [errors (v-fn data)]
        (postwalk (fn [node]
                    (if-let [errors (get node :$errors$)]
                      (assoc-in node [:$errors$ :messages] (map (fn [e] (get-validator-message e)) (:failed errors)))
                      node)) errors)))))
