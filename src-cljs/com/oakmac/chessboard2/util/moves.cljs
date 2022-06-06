(ns com.oakmac.chessboard2.util.moves)

(defn apply-move-to-position
  "applies a move to a chess position, returns the new position with the move executed"
  [position {:keys [from to] :as _move}]
  (if (get position from)
    (let [source-piece (get position from)]
      (-> position
        (dissoc from)
        (assoc to source-piece)))
    position))
