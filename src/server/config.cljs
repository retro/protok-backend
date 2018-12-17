(ns server.config
  (:require ["dotenv" :as dotenv]
            [oops.core :refer [ocall oget oget+]]
            [camel-snake-kebab.core :refer [->kebab-case]]
            [clojure.string :as str]
            [mount.core :refer [defstate]]))

(defn get-env []
  (let [env-keys (ocall js/Object :keys (oget js/process :env))]
    (reduce 
     (fn [acc k]
       (if (str/includes? k ".")
         acc
         (let [val (oget+ js/process [:env k])
               path (map (comp keyword ->kebab-case) (str/split k #"__"))]
           (assoc-in acc path val))))
     {} env-keys)))

(defstate env :start (get-env))
