(ns com.oakmac.chessboard2.html
  (:require
    [clojure.string :as str]
    [com.oakmac.chessboard2.util.squares :refer [idx->alpha]]))

(defn Square
  [{:keys [color code id]}]
  (let [classes (str "square-55d63 "
                  (if (= color "white") "white-1e1d7" "black-3c85d"))]
    (str
      "<div class='" classes "' id='" id "'>"
      code
      "</div>")))

(defn BoardContainer
  [{:keys [num-rows num-cols square-el-ids]}]
  (str
    "<div class=chessboard-63f37>"
    "<div class=board-b72b1>"
    (let [html (atom "")
          white? (atom true)]
      (doseq [row-idx (reverse (range 0 num-rows))]
        (swap! html str (str "<div class='row-5277c'>"))
        (doseq [col-idx (range 0 num-cols)]
          (let [code (str (idx->alpha col-idx) "-" (inc row-idx))]
            (swap! html str (Square {:code code
                                     :color (if @white? "white" "black")
                                     :id (get square-el-ids code)}))
            (swap! white? not)))
        (swap! html str (str "</div>")))
      @html)
    "</div>"
    "</div>"))

;; step 1) build the HTML skeleton (ie: rows and squares)
;; step 2) operate against that skeleton
