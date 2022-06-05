(ns com.oakmac.chessboard2.util.math)

(defn hypotenuse
  [a b]
  (js/Math.sqrt
    (+ (js/Math.pow a 2)
       (js/Math.pow b 2))))
