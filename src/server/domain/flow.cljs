(ns server.domain.flow
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as hp]
            [server.framework.honeysql :refer [query query-one sanitize-fields]]
            [promesa.core :as p :refer-macros [alet]]
            [server.framework.batcher :as b]
            [server.framework.batcher.batch-all-by-field :refer [->BatchAllByField]]
            [server.domain.shared :refer [ensure-selected]]
            [server.domain.queries :refer [make-find-by-id]]))

(def find-by-id (make-find-by-id :flows))

(defn find-all-by-project-id
  ([conn project-id] (find-all-by-project-id conn project-id :*))
  ([conn project-id selection]
   (let [selection' (->> (ensure-selected selection :project-id)
                         (sanitize-fields :flows))
         sql (sql/build :select selection' :from :flows)]
     (b/fetch
      (->BatchAllByField conn sql :project-id project-id)))))
