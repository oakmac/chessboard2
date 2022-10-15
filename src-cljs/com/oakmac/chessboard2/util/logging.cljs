(ns com.oakmac.chessboard2.util.logging)

(defn error-log
  [& args]
  (apply js/console.error (conj args "[Chessboard2]")))

(defn warn-log
  [& args]
  (apply js/console.warn (conj args "[Chessboard2]")))
