(ns com.oakmac.chessboard2.util.functions)

(defn defer
  [f]
  (js/setTimeout
    (fn [] (f))
    1))
