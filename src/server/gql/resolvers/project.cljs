(ns server.gql.resolvers.project
  (:require [server.framework.pipeline :as pp :refer-macros [resolver! ts-resolver!]]
            [server.framework.graphql :refer [with-default-resolvers wrap-resolvers]]
            [server.gql.shields :as shields]
            [server.domain.project :as project]
            [server.gql.resolvers.crud :refer [crud-for]]
            [server.gql.resolvers.shared :refer [args-validator!]]))

(def validate-create!
  (args-validator! {:input.name [:not-blank]
                    :input.organization-id [:not-blank]}))

(def validate-update!
  (args-validator! {:input.name [:not-blank]
                    :input.id [:not-blank]}))

(def projects-by-organization-id
  (shields/has-current-account!
   (resolver! [value parent args context info]
     (project/find-all-by-organization-id (:system/db context) (:id parent) (:selection info)))))

(def project-from-parent
  (shields/has-current-account!
   (resolver! [value parent args context info]
     (project/find-by-id (:system/db context) (:project-id parent) (:selection info)))))

(def resolvers
  (-> {:project      (with-default-resolvers :id :name)
       :organization {:projects projects-by-organization-id}
       :flow         {:project project-from-parent}}
      (crud-for :project)
      (wrap-resolvers {[:mutation :create-project] [shields/has-current-account! validate-create!]
                       [:mutation :update-project] [shields/has-current-account! validate-update!]
                       [:mutation :delete-project] shields/has-current-account!
                       [:query :fetch-project]     shields/has-current-account!})))
