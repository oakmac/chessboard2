(ns com.oakmac.chessboard2.util.board
  "board constants"
  (:require
    [com.oakmac.chessboard2.util.fen :refer [fen->position]]))

(def file->idx
  {"a" 0
   "b" 1
   "c" 2
   "d" 3
   "e" 4
   "f" 5
   "g" 6
   "h" 7})

(def rank->idx
  {"8" 0
   "7" 1
   "6" 2
   "5" 3
   "4" 4
   "3" 5
   "2" 6
   "1" 7})

(def default-num-cols 8)
(def default-num-rows 8)

(def start-position-fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR")
(def start-position (fen->position start-position-fen))
