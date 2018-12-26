(ns server.domain.flow-node.queries
  (:require [honeysql.helpers :as h]
            [honeysql.core :as sql]
            [honeysql-postgres.helpers :as hp]
            [server.framework.honeysql :refer [query query-one sanitize-fields sanitize-map]]
            [promesa.core :as p :refer-macros [alet]]
            [promesa.async-cljs :refer-macros [async]]
            [server.domain.shared :refer [ensure-selected]]
            ["inflected" :as Inflected]
            [oops.core :refer [ocall]]))

(defn get-table [entity-type]
  (keyword (ocall Inflected :pluralize (name entity-type))))

(defn get-foreign-key [entity-type]
  (keyword (str (name entity-type) "-id")))

(defn get-node-type [node]
  (cond 
    (:flow-event-id node)    :event
    (:flow-screen-id node)   :screen
    (:flow-switch-id node)   :switch
    (:flow-flow-ref-id node) :flow-ref
    :else                    nil))

(defn remove-entrypoint-for-flow! [conn flow-id]
  (query
   conn
   (-> (h/update :flow-nodes)
       (h/sset {:is-entrypoint false})
       (h/where [:= :flow-id flow-id]))))

(defn create-node! [conn input]
  (let [input' (update input :is-entrypoint #(or % false))]
    (async
     (p/await (when (:is-entrypoint input') (remove-entrypoint-for-flow! conn (:flow-id input))))
     (query-one
      conn
      (-> (h/insert-into :flow-nodes)
          (h/values [(sanitize-map :flow-nodes input')])
          (hp/returning :*))))))

(defn create-node-child! [conn table input selection]
  (query-one
   conn
   (-> (h/insert-into table)
       (h/values [(sanitize-map table input)])
       (hp/returning (sanitize-fields table selection)))))

(defn make-create [entity-type]
  (let [table (get-table entity-type)
        foreign-key (get-foreign-key entity-type)]
    (fn create! 
      ([conn input] (create! conn input :*))
      ([conn input selection]
       (alet [{:keys [is-entrypoint flow-id]} input
              child-selection (ensure-selected selection :id)
              child-input (dissoc input :is-entrypoint :id :flow-id)
              child (p/await (create-node-child! conn table child-input child-selection))
              node (p/await (create-node! conn {foreign-key (:id child) 
                                                :flow-id flow-id
                                                :is-entrypoint is-entrypoint}))]
         (-> child
             (assoc :type (get-node-type node))
             (merge (select-keys node [:id :flow-id :is-entrypoint]))))))))

(defn find-node-by-id-and-child [conn foreign-key id]
  (query-one
   conn 
   (sql/build :select :* 
              :from :flow-nodes
              :where [:and 
                      [:= :id id]
                      [:<> foreign-key nil]])))

(defn update-or-get-node! [conn foreign-key input]
  (alet [id (:id input)
         input' (dissoc input :id)
         existing-node (p/await (find-node-by-id-and-child conn foreign-key id))]
    (if (empty? input')
      existing-node
      (when existing-node
        (async
         (p/await
          (when (:is-entrypoint input')
            (query
             conn
             (-> (h/update :flow-nodes)
                 (h/sset {:is-entrypoint false})
                 (h/where [:<> :id id])))))
         (query-one
          conn
          (-> (h/update :flow-nodes)
              (h/sset input')
              (h/where [:and 
                        [:= :id id]
                        [:<> foreign-key nil]])
              (hp/returning :*))))))))

(defn update-child! [conn table id input selection]
  (query-one
   conn
   (-> (h/update table)
       (h/sset (sanitize-map table input))
       (h/where [:= :id id])
       (hp/returning (sanitize-fields table selection)))))

(defn make-update [entity-type]
  (let [table (get-table entity-type)
        foreign-key (get-foreign-key entity-type)]
    (fn update! 
      ([conn input] (update! conn input :*))
      ([conn input selection]
       (alet [node-input (select-keys input [:id :is-entrypoint])
              child-selection (ensure-selected selection :id)
              child-input (dissoc input :is-entrypoint :id)
              node (p/await (update-or-get-node! conn foreign-key node-input))]
         (when node
           (->> (update-child! conn table (get node foreign-key) child-input child-selection)
                (p/map (fn [child]
                         (-> child
                             (assoc :type (get-node-type node))
                             (merge (select-keys node [:id :flow-id :is-entrypoint]))))))))))))
