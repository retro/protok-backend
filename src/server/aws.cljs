(ns server.aws
  (:require [mount.core :refer [defstate]]
            [server.config :refer [env]]
            ["aws-sdk" :refer [S3]]))

(defn create-s3-client []
  (let [{:keys [aws-access-key aws-secret-key s-3-bucket]} @env] 
    (S3. #js {:accessKeyId aws-access-key
              :secretAccessKey aws-secret-key
              :params #js {:Bucket s-3-bucket}})))

(defstate s3-client :start (create-s3-client))
