(ns com.oakmac.chessboard2.util.squares
  (:require
    [com.oakmac.chessboard2.util.base58 :refer [random-base58]]
    [com.oakmac.chessboard2.util.board :refer [file->idx rank->idx]]
    [com.oakmac.chessboard2.util.math :refer [half]]))

;; TODO: create this dynamically
(def black-rank->idx
  {"1" 0
   "2" 1
   "3" 2
   "4" 3
   "5" 4
   "6" 5
   "7" 6
   "8" 7})

(def black-file->idx
  {"a" 7
   "b" 6
   "c" 5
   "d" 4
   "e" 3
   "f" 2
   "g" 1
   "h" 0})

(defn- random-square-id []
  (str "square-" (random-base58)))

(def alphas (vec (.split "abcdefghijklmnopqrstuvwxyz" "")))

(def alpha->idx
  (zipmap alphas
          (range 0 (count alphas))))

(def reverse-alpha->idx
  (zipmap (reverse alphas)
          (range 0 (count alphas))))

(defn idx->alpha
  [idx]
  (nth alphas idx))

(defn squares-for-board
  "returns a collection of all the squares on a board"
  [num-rows num-cols]
  (for [row-idx (range 0 num-rows)
        col-idx (range 0 num-cols)]
    (str (idx->alpha row-idx) (inc col-idx))))

(defn create-random-square-ids
  "returns a map of square -> square-id"
  [num-rows num-cols]
  (reduce
    (fn [square-el-ids square]
      (assoc square-el-ids square (random-square-id)))
    {}
    (squares-for-board num-rows num-cols)))

(defn square->xy
  "Converts an alphanumeric square to a {:x :y} map"
  ([sq]
   (square->xy sq "white"))
  ([sq orientation]
   (let [sq-arr (.split sq "")
         file (aget sq-arr 0)
         rank (aget sq-arr 1)]
     (if (= orientation "white")
       {:x (get alpha->idx file)
        :y (get rank->idx rank)}
       {:x (get black-file->idx file)
        :y (get black-rank->idx rank)}))))

(defn square->distance
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

;; FIXME: remove multi-arity here, should always pass in orientation
(defn square->dimensions
  "Returns the dimensions of a square on the board, relative to the top left corner."
  ([square board-width]
   (square->dimensions square board-width "white"))
  ([square board-width orientation]
   (let [{:keys [x y]} (square->xy square orientation)
         ;; FIXME: need to support boards with variable number of height / width squares
         ;; ie: a 4x6 square board
         square-width (/ board-width 8)
         left (* x square-width)
         top (* y square-width)]
     {:center-left (+ left (half square-width))
      :center-top (+ top (half square-width))
      :left left
      :top top})))

(defn squares->rect-dimensions
  "Given two squares, draw a rectangle around them and return the dimensions"
  [corner1 corner2 board-width]
  (let [dims1 (square->dimensions corner1 board-width)
        dims2 (square->dimensions corner2 board-width)
        max-left (max (:left dims1) (:left dims2))
        max-top (max (:top dims1) (:top dims2))
        min-left (min (:left dims1) (:left dims2))
        min-top (min (:top dims1) (:top dims2))
        ;; TODO: this will need to be variable
        square-height (/ board-width 8)
        square-width (/ board-width 8)]
     {:height (+ (- max-top min-top) square-height)
      :width (+ (- max-left min-left) square-width)
      :left min-left
      :top min-top}))
