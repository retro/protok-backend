(ns server.gql.resolvers.flow-switch-option
  (:require [server.framework.pipeline :as pp :refer-macros [resolver! ts-resolver!]]
            [server.framework.graphql :refer [with-default-resolvers wrap-resolvers]]
            [server.domain.flow-switch-option :as flow-switch-option]
            [server.gql.shields :as shields]))

(def create-flow-switch-option
  (shields/has-current-account!
   (ts-resolver! [value parent args context info]
     (flow-switch-option/create! (:system/db context) (:input args) (:selection info)))))

(def update-flow-switch-option
  (ts-resolver! [value parent args context info]
    (flow-switch-option/update! (:system/db context) (:input args) (:selection info))))

(def delete-flow-switch-option
  (resolver! [value parent args context info]
    (flow-switch-option/delete-by-id! (:system/db context) (:id args) (:selection info))))

(def fetch-flow-switch-option
  (resolver! [value parent args context info]
    (flow-switch-option/find-by-id (:system/db context) (:id args) (:selection info))))

(def fetch-flow-switch-options-from-parent-node
  (resolver! [value parent args context info]
    (flow-switch-option/find-all-by-flow-node-id (:system/db context) (:id parent) (:selection info))))

(def resolvers
  {:mutation           {:create-flow-switch-option create-flow-switch-option
                        :update-flow-switch-option update-flow-switch-option
                        :delete-flow-switch-option delete-flow-switch-option}
   :query              {:fetch-flow-switch-option fetch-flow-switch-option}
   :flow-switch        {:options fetch-flow-switch-options-from-parent-node}
   :flow-switch-option (with-default-resolvers :id :name :description)})
