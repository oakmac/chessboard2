(ns com.oakmac.chessboard2.util.moves)

;; FIXME: does this function need to handle 0-0 and 0-0-0?
(defn move->map
  "Converts a move String to a map"
  ([m]
   (move->map m "MOVE_FORMAT"))
  ([m format]
   (let [arr (.split m "-")]
     (case format
       "ARROW_FORMAT"
       {:start (aget arr 0)
        :end (aget arr 1)}
       "MOVE_FORMAT"
       {:from (aget arr 0)
        :to (aget arr 1)}
       nil))))

(defn apply-move-to-position
  "applies a move to a chess position, returns the new position with the move executed"
  [position {:keys [from to] :as _move}]
  (if (get position from)
    (let [source-piece (get position from)]
      (-> position
        (dissoc from)
        (assoc to source-piece)))
    position))
