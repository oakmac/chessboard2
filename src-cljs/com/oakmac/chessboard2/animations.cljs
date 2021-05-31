(ns com.oakmac.chessboard2.animations
  (:require
    [com.oakmac.chessboard2.util.board :refer [start-position]]
    [com.oakmac.chessboard2.util.squares :refer [squares-for-board square->distance]]))

(defn create-radius*
  "returns a sequence of the nearest squares from a square"
  [center-square]
  (let [all-squares (squares-for-board 8 8)
        distances (map
                    (fn [s]
                      {:distance (square->distance s center-square)
                       :square s})
                    all-squares)
        sorted-by-distance (sort-by :distance distances)
        surrounding-squares (map :square sorted-by-distance)]
    ;; the first square in the sequence will always be the center square (distance value = 0)
    ;; so we can just return the rest of the sequence
    (rest surrounding-squares)))

(def create-radius (memoize create-radius*))

(defn find-closest-piece
  "returns the square of the nearest instance of piece to square
  returns nil if no instance of piece is found in the position"
  [position piece square]
  (let [nearest-squares (create-radius square)]
    (first
      (filter
        (fn [sq]
          (= piece (get position sq)))
        nearest-squares))))

(defn calculate-animations
  "returns the animations that need to happen in order to get from posA to posB"
  [posA posB]
  (let [;; which squares are the same in both positions?
        same-squares (reduce
                       (fn [squares square]
                         (if (= (get posA square) (get posB square))
                           (conj squares square)
                           squares))
                       #{}
                       (keys posB))

        ;; remove squares that are the same in both positions
        ;; convert positions to atoms here: we will incrementally be removing squares from them
        ;; as we calculate animations
        posA (atom (apply dissoc posA same-squares))
        posB (atom (apply dissoc posB same-squares))

        ;; find all of the "move" animations: pieces that exist in both position A and position B
        move-animations (reduce
                          (fn [anims [square piece]]
                            (if-let [closest-piece (find-closest-piece @posA piece square)]
                              (let [move-animation {:type "ANIMATION_MOVE"
                                                    :source closest-piece
                                                    :destination square
                                                    :piece piece
                                                    :capture? (contains? @posA square)}]
                                ;; remove these squares from the positions
                                (swap! posA dissoc closest-piece)
                                (swap! posB dissoc square)
                                (conj anims move-animation))
                              anims))
                          []
                          (sort (vec @posB)))
        squares-being-moved-to (set (map :destination move-animations))

        ;; find all of the "add" animations: pieces that only exist in position B and need
        ;; to be added to the board
        add-animations (reduce
                         (fn [anims [square piece]]
                           (conj anims {:type "ANIMATION_ADD"
                                        :square square
                                        :piece piece}))
                         []
                         (sort (vec @posB)))

        ;; find all of the "clear" animations: pieces that only exist in position A
        ;; and need to be removed from the board
        clear-animations (reduce
                           (fn [anims [square piece]]
                             ;; do not clear a piece if it is on a square that is the target of a "move"
                             ;; ie: a piece capture
                             (if-not (contains? squares-being-moved-to square)
                               (conj anims {:type "ANIMATION_CLEAR"
                                            :square square
                                            :piece piece})
                               anims))
                           []
                           (sort (vec @posA)))]

    ;; return a vector of the animations
    (vec (concat move-animations add-animations clear-animations))))
