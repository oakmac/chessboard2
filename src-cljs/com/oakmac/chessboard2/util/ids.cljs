(ns com.oakmac.chessboard2.util.ids
  (:require
    [com.oakmac.chessboard2.util.base58 :refer [random-base58]]))

(defn random-id
  [prefix]
  (str prefix "-" (random-base58)))
