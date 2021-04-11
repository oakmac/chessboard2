(ns com.oakmac.chessboard2.html
  (:require
    [clojure.string :as str]))

(defn Square
  [b-or-w code id]
  (str
    "<div class=square-55d63 id=\"" id "\">"

    "</div>"))

(defn Squares
  []
  (str "FIXME:"))

(defn Rows
  [num-rows]
  (let [r1 (range 0 num-rows)
        r2 (map (fn [row-idx]
                  (str "<div class=row>Row #" row-idx "</div>"))
                r1)]
    (str/join "" r2)))

(defn BoardContainer
  [{:keys [num-rows num-cols]}]
  (str
    "<div class=chessboard-63f37>"
    "<div class=board-b72b1>"
    (Rows num-rows)
    "</div>"
    "</div>"))
