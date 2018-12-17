(ns server.framework.graphql.context
  (:require [promesa.core :as p]))

(defn add-store [context]
  (let [store (or (:system/store context) {})]
    (cond
      (and store (satisfies? IAtom store)) context
      (boolean store) (assoc context :system/store (atom store))
      :else (assoc context :system/store (atom {})))))

(defn wrap-context
  ([context] (wrap-context context {}))
  ([context data]
   (fn [req]
     (let [context-fn? (fn? context)
           context-pipeline? (:pipeline? (meta context))
           c (cond
               (and context-fn? context-pipeline?) (context nil {:context req})
               context-fn? (context req)
               :else context)]
       (->> (p/promise c)
            (p/map (fn [resolved-context]
                     (-> data
                         (merge resolved-context)
                         add-store))))))))
