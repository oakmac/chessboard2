(ns com.oakmac.chessboard2.util.logging)

(defn warn-log
  [& args]
  (apply js/console.warn (conj args "[Chessboard2]")))
