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
    [goog.object :as gobj]




    ;; FIXME: figure out the name for where this belongs
    [com.oakmac.chessboard2.homeless :as homeless]





    [com.oakmac.chessboard2.util.predicates :refer [fen-string? start-string? valid-color? valid-move-string? valid-square? valid-piece? valid-position?]]))

(defn convert-animate-speed
  [animateSpeed]
  (cond
    (number? animateSpeed) animateSpeed
    (string? animateSpeed) (get animate-speed-strings->times animateSpeed)
    :else nil))

(defn valid-move? [m]
  (and
    (map? m)
    (valid-square? (:from m))
    (valid-square? (:to m))
    (when-let [f (:onComplete m)]
      (fn? f))))

(defn move-pieces
  "Executes a collection of Moves on the board. Modifies the position.
  Returns a collection of Promises."
  [board-state moves]
  (when flags/runtime-checks?
    (assert (every? valid-move? moves) "Invalid moves passed to move-pieces"))
  (let [current-pos (:position @board-state)
        new-pos (reduce apply-move-to-position current-pos moves)
        js-before-position (clj->js current-pos)
        js-after-position (clj->js new-pos)
        animations (calculate-animations current-pos new-pos)
        moves-map (zipmap (map :from moves) moves)

        ; _ (js/console.log "moves:" (pr-str moves))

        ;; create an empty JS object to store Promise resolve-fns
        js-resolve-fns (reduce
                         (fn [js-resolves {:keys [source destination piece] :as anim}]
                           (gobj/set js-resolves source nil)
                           js-resolves)
                         (js-obj)
                         animations)

        ;; create a collection of Promises
        move-promises (reduce
                        (fn [promises {:keys [source destination piece] :as anim}]
                          (conj promises (js/Promise.
                                           (fn [resolve-fn reject-fn]
                                             ;; store the resolve-fn on our object
                                             (gobj/set js-resolve-fns source resolve-fn)))))
                                             ;; TODO: do we need to store the reject-fn here?
                                             ;; can we ensure that will never be called?
                        []
                        animations)

        ;; create a map of callback functions
        callback-fns (reduce
                       (fn [callbacks {:keys [source destination piece] :as anim}]
                         (assoc callbacks source
                                          (fn []
                                            (let [js-move-info (js-obj "afterPosition" js-after-position
                                                                       "beforePosition" js-before-position
                                                                       ;; FIXME: duration here
                                                                       "from" source
                                                                       "to" destination
                                                                       "piece" piece)]
                                              ;; was a callback function provided for this move?
                                              (when-let [callback-fn (get-in moves-map [source :onComplete])]
                                                (when (fn? callback-fn)
                                                  (callback-fn js-move-info)))
                                              ;; call the resolve-fn for the Promise
                                              (when-let [resolve-fn (gobj/get js-resolve-fns source)]
                                                (when (fn? resolve-fn)
                                                  (resolve-fn js-move-info)))))))
                       {}
                       animations)

        ;; add the callback functions and duration times to the animations
        animations2 (map
                      (fn [{:keys [source] :as animation}]
                        (let [{:keys [animate animateSpeed] :as _move} (get moves-map source)]
                          (cond-> animation
                            true (assoc :on-finish (get callback-fns source))
                            (false? animate) (assoc :instant? true)
                            animateSpeed (assoc :duration-ms (convert-animate-speed animateSpeed)))))
                      animations)

        ; _ (js/console.log "animations:" (pr-str animations2))

        ;; convert animations to DOM operations
        dom-ops (map
                  (fn [anim]
                    (homeless/animation->dom-op anim board-state))
                  animations2)]
    ;; apply the DOM operations
    (dom-ops/apply-ops! board-state dom-ops)

    ;; update board position atom
    (swap! board-state assoc :position new-pos)

    ;; return the move promises
    move-promises))

(defn get-position
  "Returns the board position as a Clojure Map"
  [board-state]
  (get @board-state :position))

;; FIXME:
;; - need to determine the return value here, probably a single Promise
;; - need to be able to pass config object here for controlling animation speed,
;;   callback-fn, etc
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
