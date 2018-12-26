(ns server.domain.flow
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as hp]
            [server.framework.honeysql :refer [query query-one sanitize-fields]]
            [promesa.core :as p :refer-macros [alet]]
            [server.framework.batcher :as b]
            [server.framework.batcher.batch-all-by-field :refer [->BatchAllByField]]))

(defn find-all-by-project-id
  ([conn project-id] (find-all-by-project-id conn project-id :*))
  ([conn project-id selection]
   (let [selection' (if (vector? selection) (conj selection :project-id) selection)]
     (b/fetch
      (->BatchAllByField conn (sql/build :select (sanitize-fields :flows selection') :from :flows) :project-id project-id)))))
