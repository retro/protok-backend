(ns server.framework.graphql
  (:require [clojure.walk :refer [postwalk]]
            [camel-snake-kebab.core :refer [->camelCase ->camelCaseString ->PascalCase ->kebab-case-keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [promesa.core :as p]
            [oops.core :refer [oget oset!]]
            [server.framework.util :refer [jsx->clj]]
            [server.framework.pipeline :as pp :refer-macros [resolver!]]
            [cljs.core :refer [IAtom]]
            ["graphql-list-fields" :as glf]
            [clojure.string :as str]))

(defn process-field-selection [field]
  (if (str/includes? field ".")
    (let [first-part (first (str/split field "."))]
      [field (str first-part "_id")])
    field))

(defn process-fields-selection [selection]
  (->> selection
       (mapv process-field-selection)
       flatten
       (mapv ->kebab-case-keyword)))

(defn throw-validation-errors [errors]
  (let [e (js/Error. "Invalid Data.")]
    (oset! e :!extensions.!errors (clj->js errors))
    (throw e)))

(defn validate! [args validator]
  (let [errors (validator args)]
    (if (and (not (nil? errors)) (not (empty? errors)))
      (throw-validation-errors errors)
      pp/ignore)))

(defn make-default-field-resolver [field]
  (resolver! [value parent context info]
    (let [val (get parent field)]
      (if (fn? val)
        (val parent context info)
        val))))

(defn with-default-resolvers [& args]
  (let [[resolvers fields] (if (map? (first args)) [(first args) (rest args)] [{} args])]
    (reduce
     (fn [acc f]
       (if (acc f)
         acc
         (assoc acc f (make-default-field-resolver f))))
     resolvers fields)))

(defn process-info [info]
  {:original info
   :selection (process-fields-selection (glf info))})

(defn process-resolver-args [args]
  (if (= 3 (count args))
    (let [[parent context info] args]
      {:parent parent
       :context context
       :info (process-info info)})
    (let [[parent args context info] args]
      {:parent parent
       :args (transform-keys ->kebab-case-keyword (jsx->clj args))
       :context context
       :info (process-info info)})))

(defn wrap-resolver-fns [resolvers]
  (postwalk
   (fn [node]
     (let [node-fn? (fn? node)
           node-meta (meta node)
           node-resolver? (:pipeline? node-meta)]
       (cond
         (and node-fn? node-resolver?)
         (fn [& resolver-args]
           (node nil (process-resolver-args resolver-args)))

         node-fn?
         (fn [& resolver-args]
           (let [{:keys [parent args context info]} (process-resolver-args resolver-args)]
             (let [[parent args context info] resolver-args] ;; Normal resolver
               (node parent args context info))))

         :else node)))
   resolvers))

(defn update-type-resolvers-names [type-resolvers]
  (reduce-kv
   (fn [acc k v]
     (let [resolver-name (str (when (= "private" (namespace k)) "__")
                              (->camelCaseString k))]
       (assoc acc resolver-name v)))
   {} type-resolvers))

(defn update-names [resolvers]
  (reduce-kv
   (fn [acc k v]
     (let [type-name (->PascalCase (name k))]
       (assoc acc type-name (update-type-resolvers-names v))))
   {} resolvers))

(defn default-wrap-resolvers [resolvers]
  (-> resolvers
      wrap-resolver-fns
      update-names
      clj->js))

(defn apply-wrappers [wrappers val]
  (reduce
   (fn [acc wrapper]
     (wrapper acc))
   val
   (reverse wrappers)))

(defn wrap-resolvers [resolvers mapping]
  (reduce-kv
   (fn [m path wrappers]
     (let [wrappers' (if (coll? wrappers) wrappers [wrappers])]
       (update-in m path #(apply-wrappers wrappers' %))))
   resolvers
   mapping))
