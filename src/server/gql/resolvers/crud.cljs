(ns server.gql.resolvers.crud
  (:require [clojure.set :as set]
            ["inflected" :as Inflected]
            [oops.core :refer [ocall]]
            [server.framework.pipeline :as pp :refer-macros [resolver!]]
            [server.framework.graphql :refer [validate!]]
            [server.domain.queries :refer [make-create make-update make-find-by-id make-delete-by-id]]))

(defn get-table-name [entity-type]
  (keyword (ocall Inflected :pluralize (name entity-type))))

(defn create-for [resolvers entity-type]
  (let [et-name (name entity-type)
        assoc-path [:mutation (keyword (str "create-" et-name))]
        create-fn (make-create (get-table-name entity-type))]
    (assoc-in resolvers assoc-path
              (resolver! [value parent args context info]
                (create-fn (:system/db context) (:input args) (:selection info))))))

(defn read-for [resolvers entity-type]
  (let [et-name (name entity-type)
        assoc-path [:query (keyword (str "fetch-" et-name))]
        find-by-id-fn (make-find-by-id (get-table-name entity-type))]
    (assoc-in resolvers assoc-path
              (resolver! [value parent args context info]
                (find-by-id-fn (:system/db context) (:id args) (:selection info))))))

(defn update-for [resolvers entity-type]
  (let [et-name (name entity-type)
        assoc-path [:mutation (keyword (str "update-" et-name))]
        update-fn (make-update (get-table-name entity-type))]
    (assoc-in resolvers assoc-path
              (resolver! [value parent args context info]
                (update-fn (:system/db context) (:input args) (:selection info))))))

(defn delete-for [resolvers entity-type]
  (let [et-name (name entity-type)
        assoc-path [:mutation (keyword (str "delete-" et-name))]
        delete-by-id-fn (make-delete-by-id (get-table-name entity-type))]
    (assoc-in resolvers assoc-path
              (resolver! [value parent args context info]
                (delete-by-id-fn (:system/db context) (:id args) (:selection info))))))

(defn crud-for [resolvers entity-type & {:keys [only except]}]
  (let [actions (-> (if only (set only) #{:create :read :update :delete})
                    (set/difference (set except)))]
    (cond-> resolvers
      (contains? actions :create) (create-for entity-type)
      (contains? actions :read)   (read-for entity-type)
      (contains? actions :update) (update-for entity-type)
      (contains? actions :delete) (delete-for entity-type))))
