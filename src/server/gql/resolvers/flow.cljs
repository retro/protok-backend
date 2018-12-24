(ns server.gql.resolvers.flow
  (:require [server.framework.pipeline :as pp :refer-macros [resolver! ts-resolver!]]
            [server.framework.validations :as v]
            [server.framework.jwt :as jwt]
            [server.framework.graphql
             :refer [with-default-resolvers
                     validate!]]
            [server.gql.shields :as shields]
            [server.domain.flow :as flow]
            [camel-snake-kebab.core :refer [->SCREAMING_SNAKE_CASE]]))

(def create-flow-validator
  (v/to-validator {:name [:not-blank]
                   :project-id [:not-blank]}))

(def update-flow-validator
  (v/to-validator {:name [:not-blank]
                   :id [:not-blank]}))

(def create-flow
  (shields/has-current-account!
   (ts-resolver! [value parent args context]
     (:flow args)
     (validate! value create-flow-validator)
     (flow/create-flow! (:system/db context) value))))

(def update-flow
  (shields/has-current-account!
   (resolver! [value parent args context]
     (:flow args)
     (validate! value update-flow-validator)
     (flow/update-flow! (:system/db context) value))))

(def flow-by-id
  (shields/has-current-account!
   (resolver! [value parent args context]
     (flow/find-by-id (:system/db context) (:id args)))))

(def flows-by-project-id
  (shields/has-current-account!
   (resolver! [value parent args context]
     (flow/find-by-project-id (:system/db context) (:id parent)))))

(def resolvers
  {:flow  (with-default-resolvers :id :name)
   :project {:flows flows-by-project-id}
   :mutation {:create-flow create-flow
              :update-flow update-flow}
   :query    {:fetch-flow-by-id flow-by-id}})
