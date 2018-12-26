(ns server.domain.shared)

(defn ensure-selected [selection & fields]
  (if (= :* selection)
    selection
    (concat (flatten [selection]) fields)))
