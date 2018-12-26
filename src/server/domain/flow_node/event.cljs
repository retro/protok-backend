(ns server.domain.flow-node.event
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as hp]
            [server.framework.honeysql :refer [query query-one sanitize-fields]]
            [promesa.core :as p :refer-macros [alet]]
            [server.domain.shared :refer [ensure-selected]]))



(defn create-node! [conn]
  (query-one
   conn
   (-> (h/insert-into :flow-nodes))))

(defn create!
  ([conn input] (create! conn input :*))
  ([conn input selection]
   (let [is-entrypoint (:is-entrypoint input)
         selection' (ensure-selected selection :id)])))
