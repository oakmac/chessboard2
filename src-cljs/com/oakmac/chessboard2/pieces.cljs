(ns com.oakmac.chessboard2.pieces
  (:require
    [shadow.resource :as rc]))

(def wikipedia-theme
  {"bB" (rc/inline "pieces/wikipedia/bB.svg")
   "bK" (rc/inline "pieces/wikipedia/bK.svg")
   "bN" (rc/inline "pieces/wikipedia/bN.svg")
   "bP" (rc/inline "pieces/wikipedia/bP.svg")
   "bQ" (rc/inline "pieces/wikipedia/bQ.svg")
   "bR" (rc/inline "pieces/wikipedia/bR.svg")

   "wB" (rc/inline "pieces/wikipedia/wB.svg")
   "wK" (rc/inline "pieces/wikipedia/wK.svg")
   "wN" (rc/inline "pieces/wikipedia/wN.svg")
   "wP" (rc/inline "pieces/wikipedia/wP.svg")
   "wQ" (rc/inline "pieces/wikipedia/wQ.svg")
   "wR" (rc/inline "pieces/wikipedia/wR.svg")})
