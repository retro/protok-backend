(ns server.framework.pipeline
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clojure.walk :refer [prewalk]]))

(defn extract-resolver-parts [args body]
  (let [last-block (last body)
        has-rescue-block? (and (seq? last-block) (= "rescue!" (name (first last-block))))
        [begin-body rescue] (if has-rescue-block? [(drop-last body) (last body)] [body nil])
        [_ rescue-args & rescue-body] rescue]
    {:begin-args args
     :begin-body begin-body
     :rescue-args rescue-args
     :rescue-body rescue-body}))

(defn expand-body [args body]
  (into [] (map (fn [f] `(fn ~args ~f)) body)))

(defn count-body-args [args]
  (inc (count args))) ;; value is a default arg so we increment to account for it

(defn prepare-body-args [args max-args-count]
  (let [args-count (count args)]
    (if (= args-count max-args-count)
      args
      (vec (concat args (repeat (- max-args-count args-count) '_))))))

(defn begin-forms [acc potential-begin-args {:keys [begin-args begin-body]}]
  (let [begin-args-count (count-body-args potential-begin-args)] 
    (if (< begin-args-count (count begin-args))
      (throw (ex-info (apply str "Pipeline accepts at most " begin-args-count " argument(s): " (str/join ", " (map name potential-begin-args))) {}))
      (assoc acc :begin (expand-body (prepare-body-args begin-args begin-args-count) begin-body)))))

(defn rescue-forms [acc potential-begin-args {:keys [begin-args rescue-args rescue-body]}]
  (let [begin-args-count (count-body-args potential-begin-args)]
    (if (or (nil? rescue-args) (nil? rescue-body))
      acc
      (if (not= 1 (count rescue-args))
        (throw (ex-info "Pipeline catch block takes exactly one argument: error" {}))
        (assoc acc :rescue (expand-body (into [] (concat (prepare-body-args begin-args begin-args-count) rescue-args)) rescue-body))))))

(defn make-resolver [args] args)
(defn make-ts-resolver [args] args)
(defn make-shield [args] args)
(defn make-pipeline [args] args)

(def resolver-args [:parent :args :context :info])

(defn prepare-resolver [args body]
  (let [resolver-parts (extract-resolver-parts args body)]
    `(server.framework.pipeline/make-resolver
      ~(-> {:args resolver-args}
           (begin-forms resolver-args resolver-parts)
           (rescue-forms resolver-args resolver-parts)))))

(defn prepare-ts-resolver [args body]
  (let [resolver-parts (extract-resolver-parts args body)]
    `(server.framework.pipeline/make-ts-resolver
      ~(-> {:args resolver-args}
           (begin-forms resolver-args resolver-parts)
           (rescue-forms resolver-args resolver-parts)))))

(defn shield-deps [args]
  (let [processed 
        (map-indexed
         (fn [idx arg]
           (cond
             (and (= idx 1) (not= '_ arg)) :parent
             (and (= idx 2) (not= '_ arg)) :args
             (and (= idx 3) (not= '_ arg)) :context
             :else nil))
         args)]
    (set (remove nil? processed))))

(defn prepare-shield [args body]
  (let [shield-parts (drop-last body)
        resolver-part (last body)
        expanded-args (prepare-body-args args (count-body-args resolver-args))]
    `(server.framework.pipeline/make-shield
      ~(-> {:id (hash [(ns-name *ns*) shield-parts]) ;; If we're in the same namespace and have the same shield body, use the same shield ID
            :deps (shield-deps expanded-args)
            :args resolver-args}
           (begin-forms resolver-args {:begin-args args :begin-body shield-parts})
           (assoc :resolver resolver-part)))))

(defn prepare-pipeline [args body]
  (let [resolver-parts (extract-resolver-parts args body)
        pipeline-args [:context]]
    `(server.framework.pipeline/make-pipeline
      ~(-> {:args pipeline-args}
           (begin-forms pipeline-args resolver-parts)
           (rescue-forms pipeline-args resolver-parts)))))

(defmacro resolver! [args & body]
  (prepare-resolver args (or body `())))

(defmacro ts-resolver! [args & body]
  (prepare-ts-resolver args (or body `())))

(defmacro shield! [args & body]
  (prepare-shield args (or body `())))

(defmacro pipeline! [args & body]
  (prepare-pipeline args (or body `())))
