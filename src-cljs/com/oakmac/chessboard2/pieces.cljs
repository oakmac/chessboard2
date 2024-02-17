(ns com.oakmac.chessboard2.pieces
  (:require
    [goog.crypt.base64 :as base64]
    [shadow.resource :as rc]))

(def wikipedia-theme
  {"bB" (base64/encodeString (rc/inline "pieces/wikipedia/bB.svg"))
   "bK" (base64/encodeString (rc/inline "pieces/wikipedia/bK.svg"))
   "bN" (base64/encodeString (rc/inline "pieces/wikipedia/bN.svg"))
   "bP" (base64/encodeString (rc/inline "pieces/wikipedia/bP.svg"))
   "bQ" (base64/encodeString (rc/inline "pieces/wikipedia/bQ.svg"))
   "bR" (base64/encodeString (rc/inline "pieces/wikipedia/bR.svg"))

   "wB" (base64/encodeString (rc/inline "pieces/wikipedia/wB.svg"))
   "wK" (base64/encodeString (rc/inline "pieces/wikipedia/wK.svg"))
   "wN" (base64/encodeString (rc/inline "pieces/wikipedia/wN.svg"))
   "wP" (base64/encodeString (rc/inline "pieces/wikipedia/wP.svg"))
   "wQ" (base64/encodeString (rc/inline "pieces/wikipedia/wQ.svg"))
   "wR" (base64/encodeString (rc/inline "pieces/wikipedia/wR.svg"))})
