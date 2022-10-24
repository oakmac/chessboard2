(ns com.oakmac.chessboard2.util.arrows
  (:require
    [com.oakmac.chessboard2.util.math :refer [half hypotenuse]]
    [com.oakmac.chessboard2.util.squares :refer [square->dimensions]]))

(defn position
  "Returns a Map of Arrow positioning information"
  [{:keys [board-width end orientation size start] :as _arrow-config}]
  (let [
        {start-x :center-left-pct
         start-y :center-top-pct} (square->dimensions start board-width orientation)
        {end-x :center-left-pct
         end-y :center-top-pct} (square->dimensions end board-width orientation)
        dx (- end-x start-x)
        dy (- end-y start-y)
        ;; FIXME allow for variable size boards, ie: 6x4 square board
        square-width 12.5 ;1/8
        head-height (* square-width size)
        base-offset-pct-of-board (/ head-height 6)
        head-width-pct-of-board (* head-height 0.8)
        arrow-width (+ (hypotenuse dy dx)
                       base-offset-pct-of-board)
        angle (+ (js/Math.atan (/ dy dx))
                 (if (< dx 0) js/Math.PI 0))
        head-width (/ (* head-width-pct-of-board 100) arrow-width)
        base-offset (/ (* (- base-offset-pct-of-board) 100) arrow-width)
        line-width (- 100 head-width)
      ]
      (.log js/console base-offset)
      {:angle angle
       :arrow-height head-height
       :arrow-width arrow-width

       :head-width head-width
       :line-width line-width
       :start-x start-x
       :start-y start-y
       :base-offset base-offset}))
