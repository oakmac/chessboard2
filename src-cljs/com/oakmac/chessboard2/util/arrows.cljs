(ns com.oakmac.chessboard2.util.arrows
  (:require
    [com.oakmac.chessboard2.util.math :refer [half hypotenuse]]
    [com.oakmac.chessboard2.util.squares :refer [square->dimensions]]))

(defn position
  "Returns a Map of Arrow positioning information"
  [{:keys [board-width end orientation size start] :as _arrow-config}]
  (let [square-width (/ board-width 8)
        start-dims (square->dimensions start board-width orientation)
        end-dims (square->dimensions end board-width orientation)
        start-x-css (:center-left start-dims)
        start-y-css (:center-top start-dims)
        end-x-css (:center-left end-dims)
        end-y-css (:center-top end-dims)
        dx (- end-x-css start-x-css)
        dy (- end-y-css start-y-css)
        arrow-width (* square-width size 0.8)
        arrow-height (* square-width size)
        top-offset (- (/ arrow-height 2))
        line-thickness (/ arrow-height 3)
        border-radius (half line-thickness)
        line-length (+ (hypotenuse dy dx)
                       (* -1 arrow-width)
                       (* 2 border-radius))
        angle (+ (js/Math.atan (/ dy dx))
                 (if (< dx 0) js/Math.PI 0))]
      {:angle angle
       :arrow-height arrow-height
       :arrow-width arrow-width
       ;; push the arrow so that the center of the rounded edge is in the center of the square
       :arrow-margin-left (* -1 border-radius)
       :border-radius border-radius
       :line-length line-length

       :line-thickness line-thickness
       :start-x-css start-x-css
       :start-y-css start-y-css

       :start-x-css-pct (* (/ start-x-css board-width) 100)
       :start-y-css-pct (* (/ start-y-css board-width) 100)

       :top-offset top-offset
       :top-offset-pct (* (/ top-offset board-width) 100)}))

       ; :line-length-pct (* (/ line-length board-width) 100)
       ; :left-pct (:center-left-pct start-dims)
       ; :top-pct (:center-top-pct start-dims)}))
