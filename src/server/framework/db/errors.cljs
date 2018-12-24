(ns server.framework.db.errors
  (:require [oops.core :refer [oget oset!]]
            ["indefinite" :as indefinite]
            [clojure.string :as str]))

;; Partially ported from https://github.com/Shyp/go-dberror/blob/master/error.go

(defn a-or-an
  ([string] (a-or-an string true))
  ([string capitalize?]
   (indefinite string #js {:capitalize capitalize? :caseInsensitive true})))

(def regexes
  {:column-finder            #"Key \((.+)\)="
   :value-finder             #"Key \(.+\)=\((.+)\)"
   :foreign-key-table-finder #"(?i)not present in table \"(.+)\""
   :parent-table-finder      #"(?i)update or delete on table \"([^\"]+)\""})

(defn finder [regex detail]
  (let [[_ val] (re-find regex detail)]
    val))

(def find-column (partial finder (:column-finder regexes)))
(def find-value (partial finder (:value-finder regexes)))
(def find-foreign-table-key (partial finder (:foreign-key-table-finder regexes)))
(def find-parent-table (partial finder (:parent-table-finder regexes)))

(defn format-numeric-value-out-of-range [e]
  (let [message (oget e :message)
        new-message (str/replace-first message #"out of range" "too large or too small")]
    (oset! e :message new-message)))

(defn format-invalid-text-representation [e]
  (let [message (oget e :message)
        new-message (-> (if (str/includes? message "input syntax for type")
                          message
                          (str/replace-first message #"input syntax for" "input syntax for type"))
                        (str/replace-first #"input value for enum" "")
                        (str/replace-first #"invalid" "Invalid"))]
    (oset! e :message new-message)))

(defn format-not-null-violation [e]
  (let [column (oget e :column)
        message (str "No " column " was provided. Please provide " (a-or-an column false))]
    (oset! e :message message)))

(defn format-foreign-key-violation [e]
  (let [detail (oget e :detail)
        message (oget e :message)
        column (or (find-column detail) "value")
        foreign-key-table (find-foreign-table-key detail)
        table-part (if foreign-key-table "in the parent table" (str "in the " foreign-key-table " table"))
        value (find-value detail)
        parent-table (find-parent-table detail)
        table (oget e "table")
        new-message
        (cond
          (str/includes? message "update or delete")
          (str "Can't update or delete " parent-table " records because the " parent-table " " column " (" value ") is still referenced by the " table " table")

          (nil? value)
          (str "Can't save to " table " because the " column " isn't present " table-part)
          
          :else
          (str "Can't save to " table " because the " column " (" value ") isn't present " table-part))]
    (oset! e :message new-message)))

(defn format-unique-key-violation [e]
  (let [detail (oget e :detail)
        value (find-value detail)
        column (or (find-column detail) "value")
        message (if value
                  (a-or-an (str column " already exists with this value (" value ")"))
                  (a-or-an (str column " already exists with that value")))]
    (oset! e :message message)))

;; Future possible implementations
(defn format-check-violation [e]
  e)

(defn format-lock-not-available [e]
  e)

(def error-formatters
  {"22003" format-numeric-value-out-of-range
   "22P02" format-invalid-text-representation
   "23502" format-not-null-violation
   "23503" format-foreign-key-violation
   "23505" format-unique-key-violation
   "23514" format-check-violation
   "55P03" format-lock-not-available})

(defn format [error]
  (if-let [formatter (error-formatters (oget error :?code))]
    (formatter error)
    error))
