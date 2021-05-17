(ns com.oakmac.chessboard2.html
  (:require
    [clojure.string :as str]
    [com.oakmac.chessboard2.util.squares :refer [idx->alpha]]))

(defn Square
  [{:keys [color coord id]}]
  (let [classes (str "square-4b72b "
                  (if (= color "white") "white-3b784" "black-b7cb6"))]
    (str
      "<div class='" classes "' id='" id "' data-square-coord='" coord "'>"
      coord
      "</div>")))

;; TODO: this function is a hot mess; refactor to something more functional / elegant
(defn BoardContainer
  [{:keys [board-height num-rows num-cols square-el-ids]}]
  (str
    "<div class=chessboard-21da3>"
    "<div class=board-2dea6 style='height:" board-height "px;'>"
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
    "</div>"   ;; end .board-2dea6
    "</div>")) ;; end .chessboard-21da3
