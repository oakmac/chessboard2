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

(assert (= (find-closest-piece start-position "wR" "c3") "a1"))
(assert (= (find-closest-piece start-position "wR" "f3") "h1"))
(assert (= (find-closest-piece {} "wR" "f3") nil))

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
        posA (apply dissoc posA same-squares)
        posB (apply dissoc posB same-squares)
        ;; find all of the "move" animations
        move-animations (reduce
                          (fn [anims [square piece]]
                            (if-let [closest-piece (find-closest-piece posA piece square)]
                              (conj anims {:type "ANIMATION_MOVE"
                                           :source closest-piece
                                           :destination square
                                           :piece piece})
                              anims))
                          []
                          posB)]

  ;; find all of the "move" animations
  ;; "add piece" animations
  ;; "clear" animations
    move-animations))

    ; [{:type "move"
    ;   :source "b1"
    ;   :destination "c3"}
    ;  {:type "move"
    ;   :source "d2"
    ;   :destination "d4"}]))

(def position1
  {"a1" "wQ"
   "c3" "bP"})

(def position2
  {"a2" "wQ"
   "c3" "bP"})

(js/console.log (pr-str (calculate-animations position1 position2)))
