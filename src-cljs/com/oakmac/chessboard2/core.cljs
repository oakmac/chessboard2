(ns com.oakmac.chessboard2.core
  (:require
    [goog.object :as gobj]))

; (def some-state (atom {}))

(defn test1 []
  ; (swap! some-state assoc "foo" "bar")
  (js/console.log "chessboard2 !"))

(when js/window
  (gobj/set js/window "Chessboard2" (js-obj "test1" test1)))
