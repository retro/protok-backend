(ns test.helpers
  (:require [server.framework.pipeline]))

(defmacro defasynctest [name & body] 
  (let [done-name (gensym "done")
        pipeline (server.framework.pipeline/prepare-pipeline '[value] body)]
    `(cljs.test/deftest ~name
       (cljs.test/async 
        ~done-name
        (run-test! ~done-name ~pipeline)))))

