(ns com.oakmac.chessboard2.util.squares
  (:require
    [com.oakmac.chessboard2.util.base58 :refer [random-base58]]
    [com.oakmac.chessboard2.util.board :refer [file->idx rank->idx]]))

(defn- random-square-id []
  (str "square-" (random-base58)))

(def alphas (vec (.split "abcdefghijklmnopqrstuvwxyz" "")))

(defn idx->alpha
  [idx]
  (nth alphas idx))

(defn- squares-for-board*
  "returns a collection of all the squares on a board"
  [num-rows num-cols]
  (for [row-idx (range 0 num-rows)
        col-idx (range 0 num-cols)]
    (str (idx->alpha row-idx) (inc col-idx))))

(def squares-for-board (memoize squares-for-board*))

(defn create-square-el-ids
  "TODO: write doc string here"
  [num-rows num-cols]
  (reduce
    (fn [square-el-ids square]
      (assoc square-el-ids square (random-square-id)))
    {}
    (squares-for-board num-rows num-cols)))

(defn- square->xy*
  "Converts an alphanumeric square to a {:x :y} map"
  [sq]
  (let [sq-arr (.split sq "")
        file (aget sq-arr 0)
        rank (aget sq-arr 1)]
    {:x (get file->idx file)
     :y (get rank->idx rank)}))

(def square->xy (memoize square->xy*))

(defn square->distance*
  "returns the distance between two squares"
  [squareA squareB]
  (if (= squareA squareB)
    0
    (let [sq-a-xy (square->xy squareA)
          sq-b-xy (square->xy squareB)
          x-delta (js/Math.abs (- (:x sq-a-xy) (:x sq-b-xy)))
          y-delta (js/Math.abs (- (:y sq-a-xy) (:y sq-b-xy)))]
      (if (>= x-delta y-delta)
        x-delta
        y-delta))))

(def square->distance (memoize square->distance*))

(defn square->dimensions
  [square board-width]
  (let [square-arr (.split square "")
        file (aget square-arr 0)
        rank (aget square-arr 1)
        file-idx (get file->idx file)
        rank-idx (get rank->idx rank)
        square-width (/ board-width 8)]
    {:left (* file-idx square-width)
     :top (* rank-idx square-width)}))
