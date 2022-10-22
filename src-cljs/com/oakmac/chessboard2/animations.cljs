(ns com.oakmac.chessboard2.animations
  (:require
    [com.oakmac.chessboard2.html :as html]
    [com.oakmac.chessboard2.util.dom :refer [set-style-prop!]]
    [com.oakmac.chessboard2.util.pieces :refer [random-piece-id]]
    [com.oakmac.chessboard2.util.squares :refer [square->dimensions
                                                 square->distance
                                                 squares-for-board]]))

(defn animation->dom-op-add
  [{:keys [duration-ms instant? on-finish piece square] :as _animation} board-state]
  (let [{:keys [animate-speed-ms board-width orientation piece-square-pct]} @board-state
        new-piece-id (random-piece-id)
        new-piece-html (html/Piece {:board-width board-width
                                    :board-orientation orientation
                                    :id new-piece-id
                                    :hidden? true
                                    :piece piece
                                    :piece-square-pct piece-square-pct
                                    :square square
                                    :width (/ board-width 8)})
        animate-speed2 (if (true? instant?) 0 (or duration-ms animate-speed-ms))]
    {:new-html new-piece-html
     :defer-fn (fn []
                 ;; start opacity animation after piece has been added to the DOM
                 (set-style-prop! new-piece-id "transition" (str "all " animate-speed2 "ms"))
                 (set-style-prop! new-piece-id "opacity" "100%")
                 ;; add the callback if provided
                 (when (fn? on-finish)
                   (swap! board-state assoc-in [:animation-end-callbacks new-piece-id]
                          (fn []
                            (on-finish)))))
     :new-square->piece (hash-map square new-piece-id)}))

;; TODO:
;; - Should we re-use the same DOM element here instead of destroying + creating a new one?
;; - Is it important for item-ids to persist?
;; - The answer is "yes"
(defn animation->dom-op-move
  [{:keys [capture? destination duration-ms instant? on-finish piece source] :as _animation} board-state]
  (let [{:keys [animate-speed-ms board-width orientation piece-square-pct square->piece-id]} @board-state
        current-piece-id (get square->piece-id source)
        new-piece-id (random-piece-id)
        new-piece-html (html/Piece {:board-width board-width
                                    :board-orientation orientation
                                    :id new-piece-id
                                    :hidden? false
                                    :piece piece
                                    :piece-square-pct piece-square-pct
                                    :square source
                                    :width (/ board-width 8)})
        target-square-dimensions (square->dimensions destination board-width orientation)
        animate-speed2 (if (true? instant?) 0 (or duration-ms animate-speed-ms))]
    (merge
      {:new-html new-piece-html
       :defer-fn (fn []
                   ;; start move animation
                   (set-style-prop! new-piece-id "transition" (str "all " animate-speed2 "ms"))
                   (set-style-prop! new-piece-id "left" (str (:left-pct target-square-dimensions) "%"))
                   (set-style-prop! new-piece-id "top" (str (:top-pct target-square-dimensions) "%"))
                   ;; add the callback if provided
                   (when (fn? on-finish)
                     (swap! board-state assoc-in [:animation-end-callbacks new-piece-id]
                            (fn []
                              (on-finish)))))
       :duration-ms animate-speed2
       :remove-el current-piece-id
       :new-square->piece (hash-map destination new-piece-id)
       :delete-square->piece source}
      (when capture?
        {:capture-piece-id (get square->piece-id destination)}))))

(defn animation->dom-op-clear
  [{:keys [duration-ms instant? on-finish square] :as _animation} board-state]
  (let [{:keys [animate-speed-ms square->piece-id]} @board-state
        piece-id (get square->piece-id square)
        animate-speed2 (if (true? instant?) 0 (or duration-ms animate-speed-ms))]
    (merge
     {:delete-square->piece square
      :duration-ms animate-speed2
      :fade-out-piece piece-id}
     (when (fn? on-finish)
       {:defer-fn (fn []
                    (swap! board-state assoc-in [:animation-end-callbacks piece-id]
                           (fn []
                             (on-finish))))}))))

;; NOTE: would normally use a defmethod here
;; the output file size is slightly reduced by not including defmethods in the project
;; -- C. Oakman, June 2022
(defn animation->dom-op
  "Converts an Animation into the DOM operation necessary to make it happen"
  [animation board-state]
  (case (:type animation)
    "ANIMATION_ADD" (animation->dom-op-add animation board-state)
    "ANIMATION_MOVE" (animation->dom-op-move animation board-state)
    "ANIMATION_CLEAR" (animation->dom-op-clear animation board-state)
    (js/console.warn "Unknown animation type:" (:type animation))))

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

;; FIXME: pre-calculate this when the script loads (deferred)
;; dramatically speeds up the animation calculations
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
  "returns a vector of animations that need to happen in order to get from posA to posB"
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
                                                    :piece piece}]
                                ;; remove these squares from the positions
                                (swap! posA dissoc closest-piece)
                                (swap! posB dissoc square)
                                (conj anims move-animation))
                              anims))
                          []
                          (sort (vec @posB)))
        squares-being-moved-to (set (map :destination move-animations))

        ;; calculate captures
        move-animations2 (map
                           (fn [{:keys [destination] :as anim}]
                             (assoc anim :capture? (contains? @posA destination)))
                           move-animations)

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
    (vec (concat move-animations2 add-animations clear-animations))))
