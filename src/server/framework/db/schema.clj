(ns server.framework.db.schema
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [clojure.java.jdbc :as jdbc]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]))

(defn- create-uri [url] (java.net.URI. url))

(defn- parse-username-and-password [db-uri]
  (clojure.string/split (.getUserInfo db-uri) #":"))

(defn- subname [db-uri]
  (format "//%s:%s%s" (.getHost db-uri) (.getPort db-uri) (.getPath db-uri)))

(defn jdbc-connection-string
  "Converts Heroku's DATABASE_URL to a JDBC-friendly connection string"
  [heroku-database-url]
  (let [db-uri (create-uri heroku-database-url)
        [username password] (parse-username-and-password db-uri)]
    (format "jdbc:postgresql:%s?user=%s&password=%s"
            (subname db-uri)
            username
            password)))

(defn get-database-url []
  (System/getenv "DATABASE_URL"))

(defn get-jdbc-connection-string []
  (-> (get-database-url)
      (jdbc-connection-string)))

(def get-all-tables-query
  "SELECT
    table_name as table
  FROM
    information_schema.tables
  WHERE
    table_type = 'BASE TABLE'
  AND
    table_schema = 'public';")

(defn make-get-table-columns-query [table]
  (str "SELECT
          column_name as column
        FROM
          information_schema.columns
        WHERE
          table_name = '" table "';"))

(defn get-schema []
  (let [db-spec {:connection-uri  (get-jdbc-connection-string)}
        tables (map :table (jdbc/query db-spec get-all-tables-query))]
    (->> (map 
          (fn [t]
            (let [q (make-get-table-columns-query t)
                  columns (jdbc/query db-spec q)]
              [(->kebab-case-keyword t)
               (set (map #(->kebab-case-keyword (:column %)) columns))]))
          tables)
         (into {}))))

(defmacro defschema [var-name]
  (let [schema (get-schema)]
    `(def ~var-name ~schema)))
