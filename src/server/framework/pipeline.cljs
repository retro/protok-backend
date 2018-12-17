(ns server.framework.pipeline
  (:require [cljs.core.async :refer [<! chan put!]]
            [promesa.core :as p]
            [server.framework.db :as db]
            ["bluebird" :as Promise]
            [oops.core :refer [ocall]]
            [server.framework.util :refer [dissoc-namespaced-keys]])
  (:require-macros [cljs.core.async.macros :refer [go-loop]]))

(defrecord Error [type message payload cause])

(def ignore ::ignore)

(defn ^:private error! [type payload]
  (->Error type nil payload nil))

(defprotocol ISideffect
  (call! [this value args]))

(defn ^:private process-error [err]
  (cond
    (instance? Error err) err
    :else (->Error :default nil err nil)))

(defn ^:private promise? [val]
  (= (ocall Promise :resolve val) val))

(defn wrap-ignore [val]
  (->> (p/promise val)
       (p/map (fn [res] ignore))))

(def mute! wrap-ignore)

(defn ^:private promise->chan [promise]
  (let [promise-chan (chan)]
    (->> promise
         (p/map (fn [v] (put! promise-chan (if (nil? v) ::nil v))))
         (p/error (fn [e] (put! promise-chan (process-error e)))))
    promise-chan))

(def resolver-errors
  {:async-sideffect "Returning sideffects from promises is not permitted. It is possible that application state was modified in the meantime"})

(defn ^:private action-ret-val [action value args args-list error]
  (let [positional-args (map #(get args %) args-list)]
    (try
      (let [ret-val (if (nil? error)
                      (apply action (concat [value] positional-args))
                      (apply action (concat [value] positional-args [error])))]
        (if (:pipeline? (meta ret-val))
          {:value (ret-val value args)
           :promise? true}
          {:value ret-val
           :promise? (promise? ret-val)}))
      (catch :default err
        (if (= ::resolver-error (:type (.-data err)))
          (throw err)
          {:value (process-error err)
           :promise? false})))))

(defn ^:private extract-nil [value]
  (if (= ::nil value) nil value))

(defn call-sideffect [sideffect value args]
  (try
    {:value (call! sideffect value args)
     :error? false}
    (catch :default err
      {:value err
       :error? true})))

(defn ^:private generic-run-pipeline [resolver value args args-list]
  (let [{:keys [begin rescue]} resolver]
    (p/promise
     (fn [resolve reject] 
       (go-loop [block :begin
                 actions begin
                 prev-value value
                 error nil]
         (if (not (seq actions))
           (resolve prev-value)
           (let [next (first actions)
                 {:keys [value promise?]} (action-ret-val next prev-value args args-list error)
                 sideffect? (satisfies? ISideffect value)]
             (let [resolved-value (if promise? (extract-nil (<! (promise->chan value))) value)
                   error? (instance? Error resolved-value)]
               (when (and promise? sideffect?)
                 (throw (ex-info (:async-sideffect resolver-errors) {:type ::resolver-error})))
               (if sideffect?
                 (let [{:keys [value error?]} (call-sideffect resolved-value value args)
                       resolved-value (if (promise? value) (<! (promise->chan value)) value)]
                   (cond
                     (and error? (= block :begin))
                     (if (seq rescue)
                       (recur :rescue rescue prev-value value)
                       (reject (or (:payload value) value)))
                     
                     (and error? (= block :rescue))
                     (reject (or (:payload value) value))
                     
                     :else
                     (recur block (rest actions) prev-value error)))
                 (cond 
                   (= ::break resolved-value)
                   (resolve ::break)

                   (and error? (= block :begin))
                   (if (seq rescue)
                     (recur :rescue rescue prev-value (or (:payload resolved-value) resolved-value))
                     (reject (or (:payload resolved-value) resolved-value)))
                   
                   (and error? (= block :rescue)
                        (not= error resolved-value))
                   (reject (or (:payload resolved-value) resolved-value))

                   (and error? (= block :rescue)
                        (= error resolved-value))
                   (reject (or (:payload error) error))

                   :else
                   (recur block
                          (rest actions)
                          (if (= resolved-value ignore) prev-value resolved-value)
                          error)))))))))))

(defn wrap-transaction-fn [transaction-fn]
  (fn [& args]
    (wrap-ignore (apply transaction-fn args))))

(defn run-ts-resolver [resolver value args args-list]
  (let [context (:context args)]
    (if (:transaction? context)
      (let [{:keys [begin rescue]} resolver
            client (:system/db context)
            begin-savepoint (str (gensym "savepoint"))
            rescue-savepoint (str (gensym "savepoint"))
            ts-savepoint! (wrap-transaction-fn db/ts-savepoint!)
            ts-savepoint-rollback! (wrap-transaction-fn db/ts-savepoint-rollback!)
            transacted-begin (concat [#(ts-savepoint! client begin-savepoint)] begin)
            transacted-rescue (when (seq rescue)
                                (concat [#(ts-savepoint-rollback! client begin-savepoint)
                                         #(ts-savepoint! client rescue-savepoint)] rescue))
            resolver-promise (generic-run-pipeline
                              {:begin transacted-begin :rescue transacted-rescue}
                              value
                              args
                              args-list)]
        (if (seq rescue)
          (->> resolver-promise
               (p/error (fn [e]
                          (->> (db/ts-savepoint-rollback! client rescue-savepoint)
                               (p/map #(throw e))))))
          resolver-promise))


      (->> (db/checkout-client (:system/db context))
           (p/map
            (fn [{:keys [client done-cb]}]
              (let [{:keys [begin rescue]} resolver
                    ts-begin! (wrap-transaction-fn db/ts-begin!)
                    ts-commit! (wrap-transaction-fn db/ts-commit!)
                    ts-rollback! (wrap-transaction-fn db/ts-rollback!)
                    wrapped-done-cb #(wrap-ignore (done-cb))
                    transacted-begin (concat [#(ts-begin! client)] begin [#(ts-commit! client) wrapped-done-cb])
                    transacted-rescue (when (seq rescue)
                                        (concat [#(ts-rollback! client) #(ts-begin! client)] rescue [#(ts-commit! client) wrapped-done-cb]) )]
                (->> (generic-run-pipeline
                      {:begin transacted-begin :rescue transacted-rescue}
                      value
                      (-> args
                          (assoc-in [:context :system/db] client)
                          (assoc-in [:context :transaction?] true))
                      args-list)
                     (p/error (fn [e]
                                (->> (db/ts-rollback! client)
                                     (p/map (fn [_]
                                              (done-cb)
                                              (throw e))))))))))))))

(defn shield-cache-key [{:keys [id deps]} {:keys [parent args context]}]
  (hash [id
         (when (contains? deps :parent) parent)
         (when (contains? deps :args) args)
         (when (contains? deps :context) (dissoc-namespaced-keys context :system))]))

(defn get-shield-promise [shield value args args-list]
  (let [cache-key (shield-cache-key shield args)
        store (get-in args [:context :system/store])
        existing-shield (or (get-in @store [:shield cache-key]))]
    (if existing-shield
      existing-shield
      (let [running-shield (generic-run-pipeline shield value args args-list)]
        (swap! store assoc-in [:shield cache-key] running-shield)
        running-shield))))

(defn run-shield [shield value args args-list]
  (let [shield-promise (get-shield-promise shield value args args-list)]
    (->> shield-promise
         (p/map (fn [res]
                  (if res
                    (let [resolver (:resolver shield)
                          val (cond
                                (and (fn? resolver) (:pipeline? (meta resolver)))
                                (resolver nil args)
                                
                                (fn? resolver) (resolver (:parent args) (:args args) (:context args) (:info args))

                                :else (get-in args [:parent resolver]))]
                      (if (fn? val)
                        (val (:parent args) (:args args) (:context args) (:info args))
                        val))
                    (throw (js/Error. "Not Authorized!"))))))))

(defn ^:private make-shield [shield]
  (with-meta #(run-shield shield %1 %2 (:args shield)) {:pipeline? true}))

(defn ^:private make-resolver [resolver]
  (with-meta #(generic-run-pipeline resolver %1 %2 (:args resolver)) {:pipeline? true}))

(defn ^:private make-ts-resolver [resolver]
  (with-meta #(run-ts-resolver resolver %1 %2 (:args resolver)) {:pipeline? true}))

(defn ^:private make-pipeline [pipeline]
  (with-meta #(generic-run-pipeline pipeline %1 %2 (:args pipeline)) {:pipeline? true}))
