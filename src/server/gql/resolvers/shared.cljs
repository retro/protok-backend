(ns server.gql.resolvers.shared
  (:require [server.framework.validations :as v]
            [server.framework.pipeline :as pp :refer-macros [resolver! ts-resolver!]]
            [server.framework.graphql :refer [validate!]]))

(defn args-validator! [config]
  (let [v (v/to-validator config)]
    (fn [resolver]
      (resolver! [value parent args context]
        (validate! args v)
        resolver))))
