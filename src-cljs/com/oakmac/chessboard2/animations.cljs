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

        ;; find all of the "move" animations: pieces that exist in both position A and position B
        move-animations (reduce
                          (fn [anims [square piece]]
                            (if-let [closest-piece (find-closest-piece posA piece square)]
                              (conj anims {:type "ANIMATION_MOVE"
                                           :source closest-piece
                                           :destination square
                                           :piece piece
                                           :capture? (contains? posA square)})
                              anims))
                          []
                          posB)
        squares-being-moved-from (map :source move-animations)
        squares-being-moved-to (set (map :destination move-animations))

        ;; remove these squares from the two positions
        posA (apply dissoc posA squares-being-moved-from)
        posB (apply dissoc posB squares-being-moved-to)

        ;; find all of the "add" animations: pieces that only exist in position B and need
        ;; to be added to the board
        add-animations (reduce
                         (fn [anims [square piece]]
                           (conj anims {:type "ANIMATION_ADD"
                                        :square square
                                        :piece piece}))
                         []
                         posB)

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
                           posA)]

    (concat move-animations add-animations clear-animations)))

(def test1-posA {"a1" "wQ", "c3" "bP"})
(def test1-posB {"a2" "wQ", "c3" "bP"})
(def test1-anims
  #{{:type "ANIMATION_MOVE"
     :source "a1"
     :destination "a2"
     :piece "wQ"
     :capture? false}})

(assert (= (set (calculate-animations test1-posA test1-posB))
           test1-anims))

(def test2-posA {})
(def test2-posB {"b2" "bP", "c3" "bP"})
(def test2-anims
  #{{:type "ANIMATION_ADD" :square "b2" :piece "bP"}
    {:type "ANIMATION_ADD" :square "c3" :piece "bP"}})

(assert (= (set (calculate-animations test2-posA test2-posB))
           test2-anims))

(def test3-posA {"b2" "bP", "c3" "bP"})
(def test3-posB {})
(def test3-anims
  #{{:type "ANIMATION_CLEAR" :square "b2" :piece "bP"}
    {:type "ANIMATION_CLEAR" :square "c3" :piece "bP"}})

(assert (= (set (calculate-animations test3-posA test3-posB))
           test3-anims))

(def test4-posA {"b2" "wP", "c3" "wP"})
(def test4-posB {"b2" "bP", "c3" "bP"})
(def test4-anims
  #{{:type "ANIMATION_CLEAR" :square "b2" :piece "wP"}
    {:type "ANIMATION_CLEAR" :square "c3" :piece "wP"}
    {:type "ANIMATION_ADD" :square "b2" :piece "bP"}
    {:type "ANIMATION_ADD" :square "c3" :piece "bP"}})

(assert (= (set (calculate-animations test4-posA test4-posB))
           test4-anims))

(def test5-posA {"a1" "wP", "b2" "wP", "c3" "wP", "f6" "wQ"})
(def test5-posB {"a1" "wP", "b2" "bP", "c3" "bP", "h6" "wQ"})
(def test5-anims
  #{{:type "ANIMATION_CLEAR" :square "b2" :piece "wP"}
    {:type "ANIMATION_CLEAR" :square "c3" :piece "wP"}
    {:type "ANIMATION_ADD" :square "b2" :piece "bP"}
    {:type "ANIMATION_ADD" :square "c3" :piece "bP"}
    {:type "ANIMATION_MOVE" :source "f6" :destination "h6" :piece "wQ" :capture? false}})

(assert (= (set (calculate-animations test5-posA test5-posB))
           test5-anims))

(def test6-posA {"a1" "wQ", "a2" "bR", "c3" "bP"})
(def test6-posB {"a2" "wQ", "c3" "bP"})
(def test6-anims
  #{{:type "ANIMATION_MOVE"
     :source "a1"
     :destination "a2"
     :piece "wQ"
     :capture? true}})

(assert (= (set (calculate-animations test6-posA test6-posB))
           test6-anims))
