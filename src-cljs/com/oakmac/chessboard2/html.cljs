(ns com.oakmac.chessboard2.html
  (:require
    [com.oakmac.chessboard2.css :as css]
    [com.oakmac.chessboard2.pieces :refer [wikipedia-theme]]
    [com.oakmac.chessboard2.util.arrows :as arrow-util]
    [com.oakmac.chessboard2.util.ids :refer [random-id]]
    [com.oakmac.chessboard2.util.squares :refer [idx->alpha square->dimensions squares->rect-dimensions]]
    [com.oakmac.chessboard2.util.template :refer [template]]
    [goog.crypt.base64 :as base64]))

(def use-css-arrow? true)

(defn Circle
  [{:keys [board-width color id opacity orientation size square] :as cfg}]
  (let [square-dims (square->dimensions square board-width orientation)
        square-width (/ board-width 8) ;; FIXME: need to support variable number of squares here
        circle-width (* size square-width)]
    (template
      (str
        "<div class='item-18a5b circle-a0266' id='{id}'"
          "style='"
            "background-color:{color};"
            "height:{height}px;"
            "left:{left}px;"
            "opacity:{opacity};"
            "top:{top}px;"
            "width:{width}px;"
          "'></div>")
      {:color color
       :height circle-width
       :id id
       :left (- (:center-left square-dims) (/ circle-width 2))
       :opacity opacity
       :top (- (:center-top square-dims) (/ circle-width 2))
       :width circle-width})))

(defn ArrowCSS
  [{:keys [color id opacity] :as cfg}]
  (let [position-info (arrow-util/position cfg)]
    (template
      (str
        "<div class='item-18a5b arrow-bc3c7' id='{id}'"
          "style='"
            "top: {top-offset}px;"
            "opacity: {opacity};"
            "transform:"
              "translate({start-x-css}px, {start-y-css}px)"
              "rotate({angle}rad);'>"
          "<div class='arrow-line-a8dce' style='"
            "background-color: {color};"
            "width: {line-length}px;"
            "height: {line-thickness}px;"
            "margin-left: {arrow-margin-left}px;"
            "border-top-left-radius: {border-radius}px;"
            "border-bottom-left-radius: {border-radius}px;"
            "'>"
          "</div>"
          "<div class='arrow-head-38dfa' style='"
            "background-color: {color};"
            "height: {arrow-height}px;"
            "width: {arrow-width}px;"
            "'>"
          "</div>"
        "</div>")
      (merge
        position-info
        {:color color
         :id id
         :opacity opacity}))))

;; TODO: deprecate this approach
(defn ArrowSVG
  [{:keys [board-width color end id opacity _size start]}]
  (let [{:keys [height width left top]} (squares->rect-dimensions start end board-width)
        start-dims (square->dimensions start board-width)
        end-dims (square->dimensions end board-width)
        start-x (- (:center-left start-dims) left)
        start-y (- (:center-top start-dims) top)
        end-x (- (:center-left end-dims) left)
        end-y (- (:center-top end-dims) top)
        marker-id (random-id "marker")]
    (template
      (str
        "<div class='item-18a5b arrow-bc3c7' id='{id}'"
            " style='left:{left}px; top:{top}px;'>"
        "<svg width='{width}' height='{height}'>"
          "<defs>"
            "<marker id='{marker-id}' viewBox='0 0 10 10' refX='5' refY='5'"
                   " markerWidth='6' markerHeight='3'"
                   " orient='auto-start-reverse'>"
               "<path d='M 0 0 L 10 5 L 0 10 z' fill='{color}'></path>"
            "</marker>"
          "</defs>"
          "<line x1='{start-x}' y1='{start-y}' x2='{end-x}' y2='{end-y}'"
             " stroke='{color}' stroke-opacity='{opacity}' stroke-width='10'"
             " stroke-linecap='round' marker-end='url(#{marker-id})'></line>"
        "</svg>"
        "</div>")
      {:color color
       :end-x end-x
       :end-y end-y
       :height height
       :id id
       :marker-id marker-id
       :left left
       :opacity opacity
       :start-x start-x
       :start-y start-y
       :top top
       :width width})))

(defn Arrow [arrow-config]
  (if use-css-arrow?
    (ArrowCSS arrow-config)
    (ArrowSVG arrow-config)))

;; TODO: they need the ability to override this
;; should be able to put random things on the board, like a toaster SVG
(defn piece->imgsrc
  [piece]
  (base64/encodeString (get wikipedia-theme (name piece))))

;; FIXME: need alt text here for the image
(defn Piece
  [{:keys [board-orientation board-width _color id hidden? piece piece-square-pct square _width]}]
  (let [{:keys [left top]} (square->dimensions square board-width board-orientation)
        square-width (/ board-width 8)
        piece-pct (* 100 piece-square-pct)]
   (str
     "<div class='piece-349f8' id='" id "'"
       " style='left:" left "px; top:" top "px; width: " square-width "px; height: " square-width "px;"
       (when hidden? "opacity: 0;")
       "'>"
     ;; FIXME: this needs to be customizable for the user
     "<img src='data:image/svg+xml;base64," (piece->imgsrc piece) "' alt='' style='height: " piece-pct "%; width: " piece-pct "%;' />"
     "</div>")))

(defn Square
  [{:keys [color coord id]}]
  (let [classes (str "square-4b72b "
                  (if (= color "white") "white-3b784" "black-b7cb6"))]
    (str "<div class='" classes "' id='" id "' data-square-coord='" coord "'></div>")))

; (defn Squares
;   [{:keys [board-height num-rows num-cols square-el-ids items-container-id] :as opts}]
;   (let [html (atom "")
;         white? (atom true)]
;     (doseq [rank-idx (reverse (range 0 num-rows))]
;       (swap! html str (str "<div class='rank-98fa8' data-rank-idx='" (inc rank-idx) "'>"))
;       (doseq [col-idx (range 0 num-cols)]
;         (let [coord (str (idx->alpha col-idx) (inc rank-idx))]
;           (swap! html str (Square {:coord coord
;                                    :color (if @white? "white" "black")
;                                    :id (get square-el-ids coord)}))
;           (swap! white? not)))
;       (swap! html str (str "</div>"))
;       (swap! white? not))
;     @html))

;; TODO: this function is a hot mess; refactor to something more functional / elegant
(defn BoardContainer
  [{:keys [board-height container-id num-rows num-cols orientation square-el-ids items-container-id] :as _opts}]
  (str
    "<div class=chessboard-21da3 id=" container-id ">"
    "<div class=board-container-41a68 style='height: " board-height "px; width: " board-height "px;'>"
    "<div id='" items-container-id "' class=items-container-c9182 style='height:0'></div>"
    "<div class='" css/squares " "
      (if (= orientation "white")
        css/orientation-white
        css/orientation-black)
      "' style='height:" board-height "px;'>"
    ; (Squares opts)
    (let [html (atom "")
          white? (atom true)]
      (doseq [rank-idx (reverse (range 0 num-rows))]
        (swap! html str (str "<div class='rank-98fa8' data-rank-idx='" (inc rank-idx) "'>"))
        (doseq [col-idx (range 0 num-cols)]
          (let [coord (str (idx->alpha col-idx) (inc rank-idx))]
            (swap! html str (Square {:coord coord
                                     :color (if @white? "white" "black")
                                     :id (get square-el-ids coord)}))
            (swap! white? not)))
        (swap! html str (str "</div>"))
        (swap! white? not))
      @html)
    "</div>"   ;; end .squares-2dea6
    "</div>"   ;; end .board-container-41a68
    "</div>")) ;; end .chessboard-21da3
