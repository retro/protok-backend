(ns server.domain.project-file
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as hp]
            [server.framework.honeysql :refer [query query-one sanitize-fields]]
            [promesa.core :as p :refer-macros [alet]]
            [server.domain.queries :refer [make-find-by-id]]
            [server.framework.batcher :as b]
            [server.framework.batcher.batch-all-by-field :refer [->BatchAllByField]]
            [server.domain.shared :refer [ensure-selected]]))

(def find-by-id (make-find-by-id :project-files))

(defn find-all-by-project-id
  ([conn project-id] (find-all-by-project-id conn project-id :*))
  ([conn project-id selection]
   (let [selection' (->> (ensure-selected selection :project-id)
                         (sanitize-fields :project-files)) 
         sql (sql/build :select selection' :from :project-files)]
     (b/fetch
      (->BatchAllByField conn sql :project-id project-id)))))
