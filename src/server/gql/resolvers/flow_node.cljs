(ns server.gql.resolvers.flow-node
  (:require [server.framework.pipeline :as pp :refer-macros [resolver! ts-resolver!]]
            [server.framework.graphql :refer [with-default-resolvers wrap-resolvers]]
            [server.gql.shields :as shields]
            [server.domain.flow-node :as flow-node]
            [server.domain.flow-node.queries :as queries]
            [server.gql.resolvers.shared :refer [args-validator!]]
            [camel-snake-kebab.core :refer [->SCREAMING_SNAKE_CASE_STRING]]))

(defn create-for [resolvers entity-type]
  (let [create! (queries/make-create entity-type)
        resolver (ts-resolver! [value parent args context info]
                   (create! (:system/db context) (:input args) (:selection info)))]
    (assoc-in resolvers [:mutation (keyword (str "create-" (name entity-type)))] resolver)))

(defn update-for [resolvers entity-type]
  (let [update! (queries/make-update entity-type)
        resolver (ts-resolver! [value parent args context info]
                   (update! (:system/db context) (:input args) (:selection info)))]
    (assoc-in resolvers [:mutation (keyword (str "update-" (name entity-type)))] resolver)))

(def flow-nodes-by-flow-id
  (shields/has-current-account!
   (resolver! [value parent args context info]
     (flow-node/find-all-by-flow-id (:system/db context) (:id parent) (:selection info)))))

(def flow-node-type
  (resolver! [value parent]
    (->SCREAMING_SNAKE_CASE_STRING (:type parent))))

(defn flow-node-resolve-type [node]
  (case (:type node)
    :event    "FlowEvent"
    :screen   "FlowScreen"
    :switch   "FlowSwitch"
    :flow-ref "FlowFlowRef"
    nil))

(def resolvers
  (-> {:flow-event    (-> {:type flow-node-type}
                          (with-default-resolvers :id :is-entrypoint :name :description))
       :flow-screen   (-> {:type flow-node-type}
                          (with-default-resolvers :id :is-entrypoint :name :description))
       :flow-switch   (-> {:type flow-node-type}
                          (with-default-resolvers :id :is-entrypoint :name :description))
       :flow-flow-ref (-> {:type flow-node-type}
                          (with-default-resolvers :id :is-entrypoint))
       :flow-node     (-> {:private/resolve-type flow-node-resolve-type}
                          (with-default-resolvers :id))
       :flow          {:nodes flow-nodes-by-flow-id}}
      (create-for :flow-event)
      (create-for :flow-screen)
      (create-for :flow-switch)
      (create-for :flow-flow-ref)
      (update-for :flow-event)
      (update-for :flow-screen)
      (update-for :flow-switch)
      (update-for :flow-flow-ref)))
