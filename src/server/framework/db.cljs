(ns server.framework.db
  (:require ["pg" :refer [Pool Client types]]
            [oops.core :refer [ocall oget oget+]]
            [promesa.core :as p]
            [taoensso.timbre :as timbre
             :refer-macros [log  trace  debug  info  warn  error  fatal  report
                            logf tracef debugf infof warnf errorf fatalf reportf
                            spy get-env]]
            [camel-snake-kebab.core :refer [->kebab-case-keyword ->snake_case_string]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [server.framework.db.errors :as errors]
            [server.framework.db.schema :refer-macros [defschema]]))

(defschema dbschema)

(def TYPANALYZE 20)
(ocall types :setTypeParser
       TYPANALYZE
       (fn [val]
         (let [as-float (js/parseFloat val)]
           ;; The reason I need to do this now is that querying for a "count" is
           ;; returning a string. Looking at the metadata it's a data type of 20,
           ;; which is "analyze".
           ;;
           ;; https://doxygen.postgresql.org/include_2catalog_2pg__type_8h.html#ab5abe002baf3cb0ccf3f98008c72ca8a
           ;;
           ;; Javascript numeric parsing is wonky, though.
           ;;
           ;; (js/parseInt "8Ricardo") is 8.
           ;; (js/parseFloat "8.5.9Tomato") is 8.5
           ;;
           ;; ಠ_ಠ
           ;;
           ;; Hopefully we won't get back any TYPANALYZE result that starts with a
           ;; number but is not actually numeric. Requires more experimentation.
           (cond
             (nil? val) nil
             (js/isNaN as-float) val
             (number? as-float) as-float
             :default val))))

(def TYPUUIDOID 2950)
(ocall types :setTypeParser TYPUUIDOID #(cljs.core/uuid %))


(defn start-pool [connection-string]
  (let [pool (Pool. #js{:connectionString connection-string})]
    (ocall pool :on "error" #(fatal "PG Connection error" %1 %2))
    (ocall pool :on "connect" #(info "PG Connection"))
    (ocall pool :on "acquire" #(info "PG Connection acquired"))
    (ocall pool :on "remove" #(info "PG Connection removed"))
    pool))

(defn end-pool [pool]
  (ocall @pool :end))

(defn checkout-client [conn]
  (p/promise
   (fn [resolve reject]
     (ocall conn :connect
            (fn [err client done-cb]
              (if err
                (reject err)
                (resolve {:client client
                          :done-cb done-cb})))))))

(defn get-field-data [field]
  {:name               (oget field :name)
   :table-id           (oget field :tableID)
   :column-id          (oget field :columnID)
   :data-type-id       (oget field :dataTypeID)
   :data-type-size     (oget field :dataTypeSize)
   :data-type-modifier (oget field :dataTypeModifier)
   :format             (oget field :format)})

(defn extract-data [fields row]
  (reduce
   (fn [acc f]
     (let [name (:name f)
           val (transform-keys ->kebab-case-keyword (js->clj (oget+ row name)))]
       (assoc acc (->kebab-case-keyword name) val)))
   {} fields))

(defn unpack-results [res]
  (let [fields (map get-field-data (oget res :?fields))]
    (map #(extract-data fields %) (oget res :?rows))))

(defn query [conn query-string & params]
  (->> (p/promise
        (fn [resolve reject]
          (let [cb (fn [err res]
                     (if err
                       (reject err)
                       (resolve res)))]
            (info "PG Query" query-string params)
            (if params
              (ocall conn :query 
                     #js {:text query-string
                          :values (clj->js (transform-keys ->snake_case_string params))}
                     cb)
              (ocall conn :query query-string cb)))))
       (p/map unpack-results)
       (p/error (fn [e]
                  (throw (errors/format e))))))

(defn query-one [conn query-string & params]
  (->> (apply query conn query-string params)
       (p/map first)))

(defn ts-begin! [conn]
  (query conn "BEGIN"))

(defn ts-rollback! [conn]
  (query conn "ROLLBACK"))

(defn ts-commit! [conn]
  (query conn "COMMIT"))

(defn ts-savepoint! [conn savepoint]
  (query conn (str "SAVEPOINT " savepoint)))

(defn ts-savepoint-rollback! [conn savepoint]
  (query conn (str "ROLLBACK TO SAVEPOINT " savepoint)))
