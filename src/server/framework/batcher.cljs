(ns server.framework.batcher
  (:require [mount.core :refer-macros [defstate]]
            [cljs.core.async :refer [chan close! <! alts! put!]]
            [promesa.core :as p]
            [server.framework.batcher.core :as core])
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]))

(defn immediate-chan []
  (let [ch (chan)]
    (js/setImmediate #(close! ch))
    ch))

(defn add-to-batch [queue req]
  (if (satisfies? core/IBatch req)
    (let [batch-key (core/-batch-key req)
          reqs (or (get queue batch-key) [])]
      (assoc queue batch-key (conj reqs req)))
    (throw (ex-info "Batched request must implement IBatch protocol" {:request req}))))

(defn run-batched [queue]
  (doseq [[k batch-queue] queue]
    (if (= 1 (count batch-queue))
      (let [req (first batch-queue)]
        (->> (core/-fetch req)
             (p/map #(put! (:res-ch req) [:ok %]))
             (p/error #(put! (:res-ch req) [:error %]))))
      (let [runner (first batch-queue)]
        (->> (core/-fetch-multi runner batch-queue)
             (p/map (fn [res]
                      (let [unpacked (core/-unpack runner res)]
                        (doseq [req batch-queue]
                          (let [entity-key (core/-entity-key req)
                                res-ch (:res-ch req)]
                            (put! res-ch [:ok (get unpacked entity-key)]))))))
             (p/error (fn [err]
                        (doseq [req batch-queue]
                          (put! (:res-ch req) [:error err])))))))))

(defn start-batcher []
  (let [batcher-ch (chan)]
    (go-loop [queue {}]
      (let [immediate-ch (immediate-chan)
            [val ch] (alts! [batcher-ch immediate-ch])]
        (cond
          (and val (= batcher-ch ch))
          (recur (add-to-batch queue val))
          
          (= immediate-ch ch)
          (do (run-batched queue)
              (recur {}))
          
          :else nil)))
    batcher-ch))

(defn stop-batcher [batcher]
  (close! batcher))

(defstate batcher
  :start (start-batcher)
  :stop (stop-batcher batcher))

(defn fetch [req]
  (let [res-ch (chan)]
    (put! @batcher (assoc req :res-ch res-ch))
    (p/promise
     (fn [resolve reject]
       (go
         (let [[status res] (<! res-ch)]
           (close! res-ch)
           (if (= :ok status)
             (resolve res)
             (reject res))))))))
