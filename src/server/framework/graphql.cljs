(ns server.framework.graphql
  (:require [clojure.walk :refer [postwalk]]
            [camel-snake-kebab.core :refer [->camelCase ->PascalCase ->kebab-case-keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [promesa.core :as p]
            [oops.core :refer [oget oset!]]
            [server.framework.util :refer [jsx->clj]]
            [server.framework.pipeline :as r]
            [cljs.core :refer [IAtom]]))

(defn throw-validation-errors [errors]
  (let [e (js/Error. "Invalid Data.")]
    (oset! e :!extensions.!errors (clj->js errors))
    (throw e)))

(defn validate-resolver-args! [args validator]
  (let [errors (validator args)]
    (if (and (not (nil? errors)) (not (empty? errors)))
      (throw-validation-errors errors)
      r/ignore)))

(defn default-field-resolver [field parent args context info]
  (let [val (get parent field)]
    (if (fn? val)
      (val (js->clj args :keywordize-keys true) context info)
      val)))

(defn with-default-resolvers [& args]
  (let [[resolvers fields] (if (map? (first args)) [(first args) (rest args)] [{} args])]
    (reduce
     (fn [acc f]
       (if (acc f)
         acc
         (assoc acc f (partial default-field-resolver f))))
     resolvers fields)))

(defn wrap-resolver-fns [resolvers]
  (postwalk
   (fn [node]
     (let [node-fn? (fn? node)
           node-resolver? (:pipeline? (meta node))]
       (cond
         (and node-fn? node-resolver?)
         (fn [parent args context info]
           (node nil {:parent parent
                      :args (transform-keys ->kebab-case-keyword (jsx->clj args))
                      :context context
                      :info info}))

         node-fn?
         (fn [parent args context info]
           (node parent (transform-keys ->kebab-case-keyword (jsx->clj args)) context info))

         :else node)))
   resolvers))

(defn update-type-resolvers-names [type-resolvers]
  (reduce-kv
   (fn [acc k v]
     (let [resolver-name (->camelCase (name k))]
       (assoc acc resolver-name v)))
   {} type-resolvers))

(defn update-names [resolvers]
  (reduce-kv
   (fn [acc k v]
     (let [type-name (->PascalCase (name k))]
       (assoc acc type-name (update-type-resolvers-names v))))
   {} resolvers))

(defn wrap-resolvers [resolvers]
  (-> resolvers
      wrap-resolver-fns
      update-names
      clj->js))

