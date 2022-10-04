(ns com.oakmac.chessboard2.constants
  (:require
    [com.oakmac.chessboard2.util.fen :refer [fen->position]]))

(def start-fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR")

(def start-position
  (fen->position start-fen))

;; TODO: adjust these times
(def animate-speed-strings->times
  {"superfast" 40
   "super fast" 40
   "fast" 80
   "slow" 300
   "superslow" 800
   "super slow" 800})

(def animate-speed-strings
  (set (keys animate-speed-strings->times)))
