(ns com.oakmac.chessboard2.constants)

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
