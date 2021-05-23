(ns com.oakmac.chessboard2.html
  (:require
    [com.oakmac.chessboard2.pieces :refer [wikipedia-theme]]
    [com.oakmac.chessboard2.util.squares :refer [idx->alpha square->dimensions]]
    [goog.crypt.base64 :as base64]))

;; TODO: they need the ability to override this
;; should be able to put random things on the board, like a toaster SVG
(defn piece->imgsrc
  [piece]
  (base64/encodeString (get wikipedia-theme (name piece))))

;; FIXME: need alt text here for the image
(defn Piece
  [{:keys [board-width color id hidden? piece piece-square-pct square width]}]
  (let [{:keys [left top]} (square->dimensions square board-width)
        square-width (/ board-width 8)
        piece-pct (* 100 piece-square-pct)]
   (str
     "<div class='piece-349f8" (when hidden? " hidden-20b44")  "' id='" id "' style='left:" left "px; top:" top "px; width: " square-width "px; height: " square-width "px;'>"
     ;; FIXME: this needs to be customizable for the user
     "<img src='data:image/svg+xml;base64," (piece->imgsrc piece) "' alt='' style='height: " piece-pct "%; width: " piece-pct "%;' />"
     "</div>")))

(defn Square
  [{:keys [color coord id]}]
  (let [classes (str "square-4b72b "
                  (if (= color "white") "white-3b784" "black-b7cb6"))]
    (str "<div class='" classes "' id='" id "' data-square-coord='" coord "'></div>")))

;; TODO: this function is a hot mess; refactor to something more functional / elegant
(defn BoardContainer
  [{:keys [board-height num-rows num-cols square-el-ids pieces-container-id]}]
  (str
    "<div class=chessboard-21da3>"
    "<div class=board-container-41a68 style='height: " board-height "px; width: " board-height "px;'>"
    "<div id='" pieces-container-id "' class=items-container-c9182 style='height:0'></div>"
    "<div class=squares-2dea6 style='height:" board-height "px;'>"
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
