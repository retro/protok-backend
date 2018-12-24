(ns server.gql.resolvers.project
  (:require [server.framework.pipeline :as pp :refer-macros [resolver! ts-resolver!]]
            [server.framework.validations :as v]
            [server.framework.jwt :as jwt]
            [server.framework.graphql
             :refer [with-default-resolvers
                     validate!]]
            [server.gql.shields :as shields]
            [server.domain.project :as project]
            [camel-snake-kebab.core :refer [->SCREAMING_SNAKE_CASE]]))

(def create-project-validator
  (v/to-validator {:name [:not-blank]
                   :organization-id [:not-blank]}))

(def update-project-validator
  (v/to-validator {:name [:not-blank]
                   :id [:not-blank]}))

(def create-project
  (shields/has-current-account!
   (ts-resolver! [value parent args context]
     (:project args)
     (validate! value create-project-validator)
     (project/create-project! (:system/db context) value))))

(def update-project
  (shields/has-current-account!
   (resolver! [value parent args context]
     (:project args)
     (validate! value update-project-validator)
     (project/update-project! (:system/db context) value))))

(def project-by-id
  (shields/has-current-account!
   (resolver! [value parent args context]
     (project/find-by-id (:system/db context) (:id args)))))

(def projects-by-organization-id
  (shields/has-current-account!
   (resolver! [value parent args context]
     (project/find-by-organization-id (:system/db context) (:id parent)))))

(def resolvers
  {:project  (with-default-resolvers :id :name)
   :organization {:projects projects-by-organization-id}
   :mutation {:create-project create-project
              :update-project update-project}
   :query    {:fetch-project-by-id project-by-id}})
