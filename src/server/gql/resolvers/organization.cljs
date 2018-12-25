(ns server.gql.resolvers.organization
  (:require [server.framework.pipeline :as pp :refer-macros [resolver! ts-resolver! shield!]]
            [server.framework.validations :as v]
            [server.framework.graphql
             :refer [with-default-resolvers
                     wrap-resolvers
                     validate!]]
            [server.domain.organization :as organization]
            [server.gql.shields :as shields]
            [camel-snake-kebab.core :refer [->SCREAMING_SNAKE_CASE]]
            [server.gql.resolvers.crud :refer [crud-for]]
            [server.gql.resolvers.shared :refer [args-validator!]]))

(defn can-update-organization! [resolver]
  (shield! [value _ args context]
    (organization/can-update? (:system/db context) (get-in args [:input :id]) (:current-account context))
    resolver))

(defn can-delete-organization! [resolver]
  (shield! [value _ args context]
    (organization/can-delete? (:system/db context) (:id args) (:current-account context))
    resolver))

(defn can-access-organization! [resolver]
  (shield! [value _ args context]
    (organization/can-access? (:system/db context) (:id args) (:current-account context))
    resolver))

(def validate-create!
  (args-validator! {:input.name [:not-blank]}))

(def validate-update!
  (args-validator! {:input.name [:not-blank]
                    :input.id [:not-blank]}))

(def create-organization
  (-> (ts-resolver! [value parent args context info]
        (organization/create! (:system/db context) (:input args) (:selection info))
        (pp/sideffect! (organization/create-organization-member! (:system/db context) (:id value) (:current-account context) "owner")))
      validate-create!
      shields/has-current-account!))

(def organization-memberships
  (shields/has-current-account!
   (resolver! [value parent args context]
     (organization/find-organization-memberships (:system/db context) (:current-account context)))))

(def organization-from-parent
  (resolver! [value parent args context]
    (organization/find-by-id (:system/db context) (:organization-id parent))))

(def member-role
  (resolver! [value parent args context]
    (when-let [member-role (:member-role parent)]
      (->SCREAMING_SNAKE_CASE member-role))))

(def membership-from-organization
  (resolver! [value parent args context]
    (organization/find-organization-membership
     (:system/db context)
     (:id parent)
     (:current-account context))))

(def resolvers
  (-> {:organization            (-> {:membership membership-from-organization}
                                    (with-default-resolvers :id :name))
       :account                 {:organization-memberships organization-memberships}
       :project                 {:organization organization-from-parent}
       :organization-membership {:organization organization-from-parent
                                 :member-role  member-role}
       :mutation                {:create-organization create-organization}}
      (crud-for :organization :except [:create])
      (wrap-resolvers {[:query :fetch-organization]     can-access-organization!
                       [:mutation :update-organization] [can-update-organization! validate-update!]
                       [:mutation :delete-organization] can-delete-organization!})))

