(ns test.helpers
   (:require [promesa.core :as p :refer-macros [alet]]
             [server.framework.db :refer [query]]
             [clojure.string :as str]
             [server.db :refer [db]]
             [server.framework.pipeline :as pp :refer-macros [pipeline!]]
             [cljs.test :refer [is]]))

(defn get-tables-query [conn]
  (query conn "SELECT tablename FROM pg_tables WHERE schemaname = 'public'"))

(defn truncate-tables-query [conn tables]
  (let [q (map #(str "TRUNCATE " (:tablename %) " RESTART IDENTITY CASCADE;") tables)]
    (query conn (str/join "\n" q))))

(defn truncate-tables! [conn]
  (->> (get-tables-query conn)
       (p/map #(truncate-tables-query conn %))))

(defn run-test! [done test-pipeline]
  (let [wrap-pipeline
        (pipeline! [value]
          (pp/wrap-ignore (truncate-tables! @db))
          test-pipeline
          (done)
          (rescue! [err]
            (println err)
            (is (= false true))
            (done)))]
    (wrap-pipeline nil {})))
