(ns com.oakmac.chessboard2.api
  "Functions that represent the CLJS API for Chessboard2"
  (:require
    [com.oakmac.chessboard2.animations :refer [calculate-animations]]
    [com.oakmac.chessboard2.css :as css]
    [com.oakmac.chessboard2.dom-ops :as dom-ops]
    [com.oakmac.chessboard2.constants :refer [animate-speed-strings->times]]
    [com.oakmac.chessboard2.feature-flags :as flags]
    [com.oakmac.chessboard2.html :as html]
    [com.oakmac.chessboard2.util.board :refer [start-position]]
    [com.oakmac.chessboard2.util.data-transforms :refer [map->js-return-format]]
    [com.oakmac.chessboard2.util.dom :as dom-util :refer [add-class! append-html! remove-class! remove-element! set-style-prop!]]
    [com.oakmac.chessboard2.util.fen :refer [fen->position position->fen valid-fen?]]
    [com.oakmac.chessboard2.util.functions :refer [defer]]
    [com.oakmac.chessboard2.util.ids :refer [random-id]]
    [com.oakmac.chessboard2.util.moves :refer [apply-move-to-position]]
    [com.oakmac.chessboard2.util.pieces :refer [random-piece-id]]





    ;; FIXME: figure out the name for where this belongs
    [com.oakmac.chessboard2.homeless :as homeless]





    [com.oakmac.chessboard2.util.predicates :refer [fen-string? start-string? valid-color? valid-move-string? valid-square? valid-piece? valid-position?]]))

(defn default-move-cfg
  "Returns the default move config map."
  [board-state]
  (let [{:keys [animate-speed-ms]} @board-state]
    {:animate true
     :animateSpeed animate-speed-ms
     :onComplete nil}))

(defn convert-animate-speed
  [{:keys [animateSpeed] :as m}]
  (if-let [speed-ms (get animate-speed-strings->times animateSpeed)]
    (assoc m :animateSpeed speed-ms)
    m))

(defn move-pieces
  "Executes a collection of Moves on the board. Modifies the position.
  Returns a collection of Promises."
  [board-state moves]
  (let [current-pos (:position @board-state)
        new-pos (reduce apply-move-to-position current-pos moves)
        position-info {:after-pos new-pos, :before-pos current-pos}
        default-cfg (default-move-cfg board-state)
        moves2 (->> moves
                    (map #(merge default-cfg %))
                    (map convert-animate-speed))
        ;; perform DOM operations to put the board in the new state
        move-promises (map #(dom-ops/execute-move! board-state position-info %) moves2)]
    ;; update board position atom
    (swap! board-state assoc :position new-pos)
    ;; return the move promises
    move-promises))

(defn get-position
  "Returns the board position as a Clojure Map"
  [board-state]
  (get @board-state :position))

;; TODO: need to determine whether to set with animation or instantly
(defn set-position!
  "Sets the board position using animation.
  Returns a Promise."
  [board-state new-pos]
  (when flags/runtime-checks?
    (assert (valid-position? new-pos) "Invalid Position Map passed to set-position"))
  (let [current-pos (get-position board-state)
        animations (calculate-animations current-pos new-pos)

        ; _ (js/console.log (pr-str animations))
        ; _ (js/console.log "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")

        dom-ops (map
                  (fn [anim]
                    (homeless/animation->dom-op anim board-state))
                  animations)]

    ; (js/console.log (pr-str dom-ops))
    ; (js/console.log "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")

    (dom-ops/apply-ops! board-state dom-ops)
    (swap! board-state assoc :position new-pos)
    nil))

    ; (js/Promise.
    ;   (fn [resolve-fn _reject-fn]
    ;     (if animate
    ;       (execute-move-with-animation! board-state position-info move resolve-fn)
    ;       (do
    ;         (execute-move-instant! board-state position-info move)
    ;         (resolve-fn)))))))

    ; (js/console.log (pr-str animations)))

(defn set-position-instant!
  "Sets the board position instantly. Returns a Clojure map of the new position."
  [board-state new-pos])
  ;; FIXME: write me
