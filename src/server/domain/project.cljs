(ns server.domain.project
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as hp]
            [server.framework.honeysql :refer [query query-one sanitize-fields]]
            [promesa.core :as p :refer-macros [alet]]
            [server.domain.queries :refer [make-find-by-id]]
            [server.framework.batcher :as b]
            [server.framework.batcher.batch-all-by-field :refer [->BatchAllByField]]))

(def find-by-id (make-find-by-id :projects))

(defn find-all-by-organization-id
  ([conn organization-id] (find-all-by-organization-id conn organization-id :*))
  ([conn organization-id selection]
   (let [selection' (if (vector? selection) (conj selection :organization-id) selection)]
     (b/fetch
      (->BatchAllByField conn (sql/build :select (sanitize-fields :projects selection') :from :projects) :organization-id organization-id)))))
