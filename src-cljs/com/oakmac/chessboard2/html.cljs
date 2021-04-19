(ns com.oakmac.chessboard2.html
  (:require
    [clojure.string :as str]))

(defn Square
  [b-or-w code id]
  (str
    "<div class=square-55d63 id=\"" id "\">"

    "</div>"))

(defn BoardContainer
  [{:keys [num-rows num-cols]}]
  (str
    "<div class=chessboard-63f37>"
    "<div class=board-b72b1>"
    (let [html (atom "")]
      (doseq [row-idx (range 0 num-rows)]
        (swap! html str (str "<div class='row-5277c'>"))
        (doseq [col-idx (range 0 num-cols)]
          (swap! html str (str "<div>" row-idx "-" col-idx "</div>")))
        (swap! html str (str "</div>")))
      @html)
    "</div>"
    "</div>"))
