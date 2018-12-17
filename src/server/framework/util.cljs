(ns server.framework.util)

(defn jsx->clj
  "Recursively transforms JavaScript arrays into ClojureScript
  vectors, and JavaScript objects into ClojureScript maps.  With
  option ':keywordize-keys true' will convert object fields from
  strings to keywords."
  ([x] (jsx->clj x :keywordize-keys true))
  ([x & opts]
   (let [{:keys [keywordize-keys]} opts
         keyfn (if keywordize-keys keyword str)
         f (fn thiprotokn [x]
             (cond
               (satisfies? IEncodeClojure x)
               (-js->clj x (apply array-map opts))

               (seq? x)
               (doall (map thiprotokn x))

               (coll? x)
               (into (empty x) (map thiprotokn x))

               (array? x)
               (vec (map thiprotokn x))
               
               (= (goog/typeOf x) "object")
               (into {} (for [k (js-keys x)]
                          [(keyfn k) (thiprotokn (aget x k))]))

               :else x))]
     (f x))))

(defn dissoc-namespaced-keys [data dissoc-namespace]
  (reduce-kv
   (fn [m k v]
     (if (= (name dissoc-namespace) (namespace k))
       m
       (assoc m k v)))
   {} data))

;;found here: https://github.com/metosin/ring-swagger/blob/1c5b8ab7ad7a5735624986bbb6b288aaf168d407/src/ring/swagger/common.clj#L53-L73

(defn deep-merge
  "Recursively merges maps.
   If the first parameter is a keyword it tells the strategy to
   use when merging non-map collections. Options are
   - :replace, the default, the last value is used
   - :into, if the value in every map is a collection they are concatenated
     using into. Thus the type of (first) value is maintained."
  {:arglists '([strategy & values] [values])}
  [& values]
  (let [[values strategy] (if (keyword? (first values))
                            [(rest values) (first values)]
                            [values :replace])]
    (cond
      (every? map? values)
      (apply merge-with (partial deep-merge strategy) values)

      (and (= strategy :into) (every? coll? values))
      (reduce into values)

      :else
      (last values))))
