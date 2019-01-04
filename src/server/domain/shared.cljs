(ns server.domain.shared)

(defn ensure-selected [selection & fields]
  (if (= :* selection)
    [selection]
    (flatten (concat [selection] fields))))
