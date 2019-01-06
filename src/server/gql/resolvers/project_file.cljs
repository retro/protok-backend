(ns server.gql.resolvers.project-file
  (:require [server.framework.pipeline :as pp :refer-macros [resolver! ts-resolver!]]
            [server.framework.graphql :refer [with-default-resolvers wrap-resolvers]]
            [server.gql.shields :as shields]
            [server.domain.project-file :as project-file]
            [server.gql.resolvers.crud :refer [crud-for]]
            [server.gql.resolvers.shared :refer [args-validator!]]
            [clojure.string :as str]
            [oops.core :refer [ocall+]]
            [server.config :refer [env]]
            [server.aws :refer [s3-client]]
            [server.domain.shared :refer [ensure-selected]]))

(defn get-put-url [project-file]
  (let [filename-parts [(:s-3-folder @env)
                        (:project-id project-file)
                        (:id project-file)
                        (:filename project-file)]
        key (str/join "/" filename-parts)]
    (ocall+ @s3-client :getSignedUrl "putObject"
            #js {:Key key
                 :Bucket (:s-3-bucket @env)
                 :ContentType (:mime-type project-file)
                 :Expires (* 60 60)})))

(defn get-server-filename [project-file]
  (when-let [filename (:filename project-file)]
    (str/join "/" [(:s-3-folder @env)
                   (:project-id project-file)
                   (:id project-file)
                   filename])))

(def validate-create!
  (args-validator! {:input.filename [:not-blank]
                    :input.project-id [:not-blank]}))

(def validate-update!
  (args-validator! {:input.id [:not-blank]}))

(def project-files-by-project-id
  (shields/has-current-account!
   (resolver! [value parent args context info]
     (project-file/find-all-by-project-id (:system/db context) (:id parent) (ensure-selected (:selection info) :filename :project-id :mime-type)))))

(def project-file-from-parent
  (shields/has-current-account!
   (resolver! [value parent args context info]
     (project-file/find-by-id (:system/db context) (:project-file-id parent) (ensure-selected (:selection info) :filename :project-id :mime-type)))))

(def upload-url
  (resolver! [value parent args context info]
    (get-put-url parent)))

(def server-filename
  (resolver! [value parent args context info]
    (get-server-filename parent)))

(def resolvers
  (-> {:project-file (-> {:upload-url upload-url
                          :server-filename server-filename}
                         (with-default-resolvers :id :filename))
       :project      {:project-files project-files-by-project-id}
       :flow-screen  {:project-file project-file-from-parent}}
      (crud-for :project-file :ensure-selected [:filename :project-id :mime-type])
      (wrap-resolvers {[:mutation :create-project-file] [shields/has-current-account! validate-create!]
                       [:mutation :update-project-file] [shields/has-current-account! validate-update!]
                       [:mutation :delete-project-file] shields/has-current-account!
                       [:query :fetch-project-file]     shields/has-current-account!})))
