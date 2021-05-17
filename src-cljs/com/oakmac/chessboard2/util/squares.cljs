(ns com.oakmac.chessboard2.util.squares
  (:require
    [clojure.string :as str]
    [com.oakmac.chessboard2.util.base58 :refer [random-base58]]))

(defn- random-square-id []
  (str "square-" (random-base58)))

(def alphas (vec (.split "abcdefghijklmnopqrstuvwxyz" "")))

(defn idx->alpha
  [idx]
  (nth alphas idx))

(defn create-square-el-ids
  "FIXME: write me"
  [num-rows num-cols]
  (reduce
    (fn [square-el-ids square]
      (assoc square-el-ids square (random-square-id)))
    {}
    (for [row-idx (range 0 num-rows)
          col-idx (range 0 num-cols)]
      (str (idx->alpha row-idx) (inc col-idx)))))
