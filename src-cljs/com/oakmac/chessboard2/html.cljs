(ns com.oakmac.chessboard2.html
  "functions that return raw HTML"
  (:require
    [com.oakmac.chessboard2.css :as css]
    [com.oakmac.chessboard2.feature-flags :as flags]
    [com.oakmac.chessboard2.pieces :refer [wikipedia-theme]]
    [com.oakmac.chessboard2.util.arrows :as arrow-util]
    [com.oakmac.chessboard2.util.math :refer [half]]
    [com.oakmac.chessboard2.util.squares :refer [idx->alpha square->dimensions]]
    [com.oakmac.chessboard2.util.template :refer [template]]
    [goog.crypt.base64 :as base64]))

(declare piece->imgsrc)

(defn FileCoordinates
  [_cfg]
  (let [num-files 8
        files (range 0 num-files)]
    (->> files
      (map
        (fn [f]
          (str "<div class='file-44ae4'>" (idx->alpha f) "</div>")))
      (apply str))))

(defn RankCoordinates
  [_cfg]
  (let [num-ranks 8
        ranks (reverse (range 1 (inc num-ranks)))]
    (->> ranks
      (map
        (fn [r]
          (str "<div class='rank-3d54c'>" r "</div>")))
      (apply str))))

(defn DraggingPiece
  [{:keys [height id piece piece-square-pct width x y]}]
  (let [piece-pct (* 100 piece-square-pct)]
    (template
      (str
        "<div id='{id}' class='dragging-4a6c1' style='left:{left}px;top:{top}px;height:{height}px;width:{width}px;'>"
         ;; FIXME: this needs to be customizable for the user
         ;; https://github.com/oakmac/chessboard2/issues/26
         ;; FIXME: need alt text here for the image
         "<img src='data:image/svg+xml;base64," (piece->imgsrc piece) "' alt='' style='height:" piece-pct "%;width:" piece-pct "%;' />"
        "</div>")
      {:height height
       :id id
       :left x
       :top y
       :width width})))

(defn Circle
  [{:keys [board-width color id opacity orientation size square] :as _cfg}]
  (let [square-dims (square->dimensions square board-width orientation)
        square-width (/ board-width 8) ;; FIXME: need to support variable number of squares here
        circle-width (* size square-width)
        circle-width-pct (* (/ circle-width board-width) 100)
        left-px (- (:center-left square-dims) (half circle-width))
        left-pct (* (/ left-px board-width) 100)
        top-px (- (:center-top square-dims) (half circle-width))
        top-pct (* (/ top-px board-width) 100)]
    (template
      (str
        "<div class='item-18a5b circle-a0266' id='{id}'"
          "style='"
            "background-color:{color};"
            "height:{height}%;"
            "left:{left}%;"
            "opacity:{opacity};"
            "top:{top}%;"
            "width:{width}%;"
          "'></div>")
      {:color color
       :height circle-width-pct
       :id id
       :left left-pct
       :opacity opacity
       :top top-pct
       :width circle-width-pct})))

(defn CustomItem
  [board-state {:keys [className html-str id square] :as _config}]
  (let [{:keys [board-width orientation piece-square-pct]} @board-state
        square-dims (square->dimensions square board-width orientation)
        square-height (/ board-width 8) ;; FIXME: need to support variable number of squares here
        square-width square-height
        ;; TODO: they need to be able to pass in a custom value for this
        itm-height (* piece-square-pct square-height)
        itm-width (* piece-square-pct square-width)]
    (template
      (str
        "<div class='item-18a5b {className}' id='{id}'"
          "style='"
            "height:{height}px;"
            "left:{left}px;"
            "top:{top}px;"
            "width:{width}px;"
            "'>"
          html-str
        "</div>")
      {:className className
       :height itm-height
       :id id
       :left (- (:center-left square-dims) (half itm-width))
       :top (- (:center-top square-dims) (half itm-height))
       :width itm-width})))

(defn Arrow
  [{:keys [color id opacity] :as cfg}]
  (let [position-info (arrow-util/position cfg)]
    (template
      (str
        "<div class='item-18a5b arrow-bc3c7' id='{id}'"
          "style='"
            "top:{start-y}%;"
            "left:{start-x}%;"
            "opacity:{opacity};"
            "transform:"
              "translateY(-50%)"
              "rotate({angle}rad);"
            "width:{arrow-width}%;"
            "height:{arrow-height}%;"
            "'>"
          "<div class='arrow-line-a8dce' style='"
            "background-color:{color};"
            "width:{line-width}%;"
            "margin-left:{base-offset}%;"
            "'>"
          "</div>"
          "<div class='arrow-head-38dfa' style='"
            "background-color:{color};"
            "width:{head-width}%;"
            "'>"
          "</div>"
        "</div>")
      (merge
        position-info
        {:color color
         :id id
         :opacity opacity}))))

;; TODO: they need the ability to override this
;; should be able to put random things on the board, like a toaster SVG
(defn piece->imgsrc
  [piece]
  (base64/encodeString (get wikipedia-theme (name piece))))

(def piece-required-keys
  #{:board-width
    :board-orientation
    :id
    :hidden?
    :piece
    :piece-square-pct
    :square
    :width})

;; FIXME: need alt text here for the image
(defn Piece
  [{:keys [board-orientation board-width _color id hidden? piece piece-square-pct square] :as piece-config}]
  (when flags/runtime-checks?
    (when (or (not= piece-required-keys (set (keys piece-config)))
              (some nil? (vals piece-config)))
      (js/console.warn "Not enough args passed to html/Piece:")
      (js/console.warn (pr-str (keys piece-config)))))
  (let [{:keys [left-pct top-pct]} (square->dimensions square board-width board-orientation)
        square-width (/ board-width 8)
        piece-pct (* 100 piece-square-pct)
        square-width-pct (* (/ square-width board-width) 100)]
   (str
     "<div class='piece-349f8' id='" id "'"
       " style='left:" left-pct "%;"
               "top:" top-pct "%;"
               "height:" square-width-pct "%;"
               "width:" square-width-pct "%;"
       (when hidden? "opacity:0;")
       "'>"
     ;; FIXME: this needs to be customizable for the user
     ;; https://github.com/oakmac/chessboard2/issues/26
     "<img src='data:image/svg+xml;base64," (piece->imgsrc piece) "' alt='' style='height:" piece-pct "%;width:" piece-pct "%;' />"
     "</div>")))

(defn Square
  [{:keys [color coord id]}]
  (let [classes (str "square-4b72b "
                  (if (= color "white") "white-3b784" "black-b7cb6"))]
    (str "<div class='" classes "' id='" id "' data-square-coord='" coord "'></div>")))

;; TODO: this function is a hot mess; refactor to something more functional / elegant
(defn Squares
  [{:keys [num-rows num-cols square->square-ids] :as _opts}]
  (let [html (atom "")
        white? (atom true)]
    (doseq [rank-idx (reverse (range 0 num-rows))]
      (swap! html str (str "<div class='rank-98fa8' data-rank-idx='" (inc rank-idx) "'>"))
      (doseq [col-idx (range 0 num-cols)]
        (let [coord (str (idx->alpha col-idx) (inc rank-idx))]
          (swap! html str (Square {:coord coord
                                   :color (if @white? "white" "black")
                                   :id (get square->square-ids coord)}))
          (swap! white? not)))
      (swap! html str (str "</div>"))
      (swap! white? not))
    @html))

(defn BoardContainer
  [{:keys [container-id orientation items-container-id squares-container-id] :as opts}]
  (template
    (str
      "<div class='chessboard-21da3' id='{container-id}'>"
      "<div class='board-container-41a68'>"
      "<div id='{items-container-id}' class='items-container-c9182'></div>"
      "<div id='{squares-container-id}' class='" css/squares " "
        (if (= orientation "white")
          css/orientation-white
          css/orientation-black)
           ;; NOTE: Squares container starts off with zero height and then is adjusted
        "' style='height:0'>{Squares}"
      "</div>"   ;; end .squares-2dea6
      "<div class='coordinates-top-f30c9'>{FileCoordinates}</div>"
      "<div class='coordinates-right-7fc08'>{RankCoordinates}</div>"
      "<div class='coordinates-bottom-ac241'>{FileCoordinates}</div>"
      "<div class='coordinates-left-183e9'>{RankCoordinates}</div>"
      "</div>"   ;; end .board-container-41a68
      "</div>") ;; end .chessboard-21da3
    {:container-id container-id
     :FileCoordinates (FileCoordinates opts)
     :items-container-id items-container-id
     :RankCoordinates (RankCoordinates opts)
     :Squares (Squares opts)
     :squares-container-id squares-container-id}))
