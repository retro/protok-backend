(ns server.framework.batcher.core)

(defprotocol IBatch
  (-batch-key [this])
  (-entity-key [this])
  (-unpack [this results])
  (-fetch [this])
  (-fetch-multi [this recs]))
