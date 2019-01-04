(ns server.gql.resolvers.flow
  (:require [server.framework.pipeline :as pp :refer-macros [resolver! ts-resolver!]]
            [server.framework.graphql :refer [with-default-resolvers wrap-resolvers]]
            [server.gql.shields :as shields]
            [server.domain.flow :as flow]
            [server.gql.resolvers.crud :refer [crud-for]]
            [server.gql.resolvers.shared :refer [args-validator!]]))

(def validate-create!
  (args-validator! {:input.name [:not-blank]
                    :input.project-id [:not-blank]}))

(def validate-update!
  (args-validator! {:input.name [:not-blank]
                    :input.id [:not-blank]}))

(defn make-flow-from-parent [field] 
  (shields/has-current-account!
   (resolver! [value parent args context info]
     (flow/find-by-id (:system/db context) (get parent field) (:selection info)))))

(def flow-from-parent-flow-id (make-flow-from-parent :flow-id))

(def flows-by-project-id
  (shields/has-current-account!
   (resolver! [value parent args context info]
     (flow/find-all-by-project-id (:system/db context) (:id parent) (:selection info)))))

(def resolvers
  (-> {:flow          (with-default-resolvers :id :name)
       :project       {:flows flows-by-project-id}
       :flow-node     {:flow flow-from-parent-flow-id}
       :flow-screen   {:flow flow-from-parent-flow-id}
       :flow-event    {:flow flow-from-parent-flow-id}
       :flow-switch   {:flow flow-from-parent-flow-id}
       :flow-flow-ref {:flow        flow-from-parent-flow-id
                       :target-flow (make-flow-from-parent :target-flow-id)}}
      (crud-for :flow)
      (wrap-resolvers {[:mutation :create-flow] [shields/has-current-account! validate-create!]
                       [:mutation :update-flow] [shields/has-current-account! validate-update!]
                       [:mutation :delete-flow] shields/has-current-account!
                       [:query :fetch-flow]     shields/has-current-account!})))
