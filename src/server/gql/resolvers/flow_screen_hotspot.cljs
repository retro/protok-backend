(ns server.gql.resolvers.flow-screen-hotspot
  (:require [server.framework.pipeline :as pp :refer-macros [resolver! ts-resolver!]]
            [server.framework.graphql :refer [with-default-resolvers wrap-resolvers]]
            [server.domain.flow-screen-hotspot :as flow-screen-hotspot]
            [server.gql.shields :as shields]))

(def create-flow-screen-hotspot
  (shields/has-current-account!
   (ts-resolver! [value parent args context info]
     (flow-screen-hotspot/create! (:system/db context) (:input args) (:selection info)))))

(def update-flow-screen-hotspot
  (ts-resolver! [value parent args context info]
    (flow-screen-hotspot/update! (:system/db context) (:input args) (:selection info))))

(def delete-flow-screen-hotspot
  (resolver! [value parent args context info]
    (flow-screen-hotspot/delete-by-id! (:system/db context) (:id args) (:selection info))))

(def fetch-flow-screen-hotspot
  (resolver! [value parent args context info]
    (flow-screen-hotspot/find-by-id (:system/db context) (:id args) (:selection info))))

(def fetch-flow-screen-hotspots-from-parent-node
  (resolver! [value parent args context info]
    (flow-screen-hotspot/find-all-by-flow-node-id (:system/db context) (:id parent) (:selection info))))

(def resolvers
  {:mutation                        {:create-flow-screen-hotspot create-flow-screen-hotspot
                                     :update-flow-screen-hotspot update-flow-screen-hotspot
                                     :delete-flow-screen-hotspot delete-flow-screen-hotspot}
   :query                           {:fetch-flow-screen-hotspot fetch-flow-screen-hotspot}
   :flow-screen                     {:hotspots fetch-flow-screen-hotspots-from-parent-node}
   :flow-screen-hotspot             (with-default-resolvers :id :name :description :coordinates :dimensions)
   :flow-screen-hotspot-dimensions  (with-default-resolvers :width :height)
   :flow-screen-hotspot-coordinates (with-default-resolvers :top :right :bottom :left)})
