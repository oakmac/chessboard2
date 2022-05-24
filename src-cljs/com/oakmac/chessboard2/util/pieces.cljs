(ns com.oakmac.chessboard2.util.pieces
  (:require
    [com.oakmac.chessboard2.util.base58 :refer [random-base58]]))

(defn random-piece-id []
  (str "piece-" (random-base58)))

;; FIXME: move to different ns
(defn random-item-id []
  (str "item-" (random-base58)))
