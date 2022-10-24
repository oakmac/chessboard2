(ns com.oakmac.chessboard2.api
  "Functions that represent the CLJS API for Chessboard2"
  (:require
    [com.oakmac.chessboard2.animations :refer [animation->dom-op calculate-animations]]
    [com.oakmac.chessboard2.constants :refer [animate-speed-strings->times]]
    [com.oakmac.chessboard2.dom-ops :as dom-ops]
    [com.oakmac.chessboard2.feature-flags :as flags]
    [com.oakmac.chessboard2.html :as html]
    [com.oakmac.chessboard2.util.arrows :as arrow-util]
    [com.oakmac.chessboard2.util.dom :as dom-util :refer [get-element set-style-prop!]]
    [com.oakmac.chessboard2.util.ids :refer [random-id]]
    [com.oakmac.chessboard2.util.logging :refer [warn-log]]
    [com.oakmac.chessboard2.util.moves :refer [apply-move-to-position]]
    [com.oakmac.chessboard2.util.predicates :refer [valid-square? valid-position?]]
    [com.oakmac.chessboard2.util.squares :refer [square->dimensions]]
    [goog.object :as gobj]))

(defn get-items-by-type
  "Returns a map of <type> Items on the board"
  [board-state type-str]
  (let [items (->> @board-state
                   :items
                   vals
                   (filter #(= type-str (:type %))))]
    (zipmap (map :id items) items)))

(defn convert-animate-speed
  [{:keys [animate animateSpeed] :as _move} default-speed-ms]
  (cond
    (false? animate) 0
    (number? animateSpeed) animateSpeed
    (get animate-speed-strings->times animateSpeed) (get animate-speed-strings->times animateSpeed)
    :else default-speed-ms))

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
  (let [current-board-state @board-state
        current-pos (:position current-board-state)
        default-speed-ms (:animate-speed-ms current-board-state)
        new-pos (reduce apply-move-to-position current-pos moves)
        js-before-position (clj->js current-pos)
        js-after-position (clj->js new-pos)
        animations (calculate-animations current-pos new-pos)
        moves-map (zipmap (map :from moves) moves)

        ; _ (js/console.log "moves-map:" (pr-str moves-map))

        ;; add move duration times to the animations
        animations2 (map
                      (fn [{:keys [source] :as anim}]
                        (let [{:keys [animate] :as move} (get moves-map source)]
                          (cond-> anim
                            true (assoc :duration-ms (convert-animate-speed move default-speed-ms))
                            (false? animate) (assoc :instant? true))))
                      animations)

        ;; create an empty JS object to store Promise resolve-fns
        js-resolve-fns (reduce
                         (fn [js-resolves {:keys [source] :as _anim}]
                           (gobj/set js-resolves source nil)
                           js-resolves)
                         (js-obj)
                         animations2)

        ;; create a collection of Promises: one for each animation
        move-promises (reduce
                        (fn [promises {:keys [source] :as _anim}]
                          (conj promises
                                (js/Promise.
                                  (fn [resolve-fn reject-fn]
                                    ;; store the resolve-fn on our object
                                    (gobj/set js-resolve-fns source resolve-fn)))))
                                    ;; TODO: do we need to store the reject-fn here?
                                    ;; can we ensure that will never be called?
                        []
                        animations2)

        ;; attach callbacks to the animations
        animations3 (map
                      (fn [{:keys [duration-ms source destination piece] :as anim}]
                        (assoc anim :on-finish
                                    (fn []
                                      (let [js-move-info (js-obj "afterPosition" js-after-position
                                                                 "beforePosition" js-before-position
                                                                 "duration" duration-ms
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
                      animations2)

        ; _ (js/console.log "animations:" (pr-str animations3))

        ;; convert animations to DOM operations
        dom-ops (map
                  (fn [anim]
                    (animation->dom-op anim board-state))
                  animations3)]
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

(defn set-position!
  "Sets the board position using animation.
  Returns a Promise."
  [board-state new-pos opts]
  (when flags/runtime-checks?
    (assert (valid-position? new-pos) "Invalid Position Map passed to set-position"))
  (let [current-pos (get-position board-state)
        default-animate-speed-ms (:animate-speed-ms @board-state)
        animations (calculate-animations current-pos new-pos)
        options-animate-speed (:animateSpeed opts)

        ;; TODO: combine this with convert-animate-speed function
        animate-speed-ms (cond
                           (false? (:animate opts)) 0
                           (number? options-animate-speed) options-animate-speed
                           (get animate-speed-strings->times options-animate-speed) (get animate-speed-strings->times options-animate-speed)
                           :else default-animate-speed-ms)

        ;; add duration times to the animations
        animations2 (map
                      (fn [{:keys [source] :as animation}]
                        (cond-> animation
                          true (assoc :duration-ms animate-speed-ms)
                          (false? (:animate opts)) (assoc :instant? true)))
                      animations)

        ;; create an Object that will store our Promise callback function
        js-resolve-fns (js-obj)
        return-promise (js/Promise.
                         (fn [resolve-fn reject-fn]
                           (gobj/set js-resolve-fns "$" resolve-fn)))

        js-before-position (clj->js current-pos)
        js-after-position (clj->js new-pos)
        js-position-info (js-obj "afterPosition" js-after-position
                                 "beforePosition" js-before-position
                                 "duration" animate-speed-ms)

        ; _ (js/console.log (pr-str animations2))
        ; _ (js/console.log "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")

        animation-finished-callback (fn []
                                      ;; call their callback if provided
                                      (when-let [f (:onComplete opts)]
                                        (when (fn? f)
                                          (f js-position-info)))
                                      ;; call the resolve-fn for the return Promise
                                      (when-let [f (gobj/get js-resolve-fns "$")]
                                        (f js-position-info)))

        ;; create the DOM operations we need in order to get to the new position
        dom-ops (map-indexed
                  (fn [idx anim]
                    ;; attach a callback-fn to the first operation
                    (if (zero? idx)
                      (-> anim
                        (assoc :on-finish animation-finished-callback)
                        (animation->dom-op board-state))
                      (animation->dom-op anim board-state)))
                  animations2)]
    ;; apply the DOM operations to the board
    (dom-ops/apply-ops! board-state dom-ops)
    ;; update the board position
    (swap! board-state assoc :position new-pos)
    ;; return the Promise object
    return-promise))

;; TODO: do we need this?
; (defn set-position-instant!
;   "Sets the board position instantly. Returns a Clojure map of the new position."
;   [board-state new-pos])

;; TODO: we should allow fade-in animation of Custom Items
(defn add-item!
  "Adds a Custom Item to the board. Returns the id of the new Item."
  [board-state item-cfg]
  (let [{:keys [className data isMovable html square type]} item-cfg
        new-id (random-id type)
        new-item {:id new-id
                  :type type
                  :data data}
        inner-html (cond
                     (string? html) html
                     (fn? html) (html (clj->js new-item))
                     ;; FIXME: let them pass a template object that gets interpolated
                     :else (do (warn-log "Invalid html property of custom Item:" (pr-str item-cfg))
                               ""))
        item-html (html/CustomItem board-state
                                   {:className className
                                    :html-str inner-html
                                    :id new-id
                                    :square square})]
    (dom-ops/apply-ops! board-state [{:new-html item-html}])
    (swap! board-state assoc-in [:items new-id] new-item)
    new-id))

(defn remove-item!
  "Removes an Item from the board"
  [board-state item-id]
  (dom-ops/apply-ops! board-state [{:remove-el item-id}])
  (swap! board-state update-in [:items] dissoc item-id)
  nil)

(defn move-items
  "Move Items on the board.
  Returns a collection of Promises."
  [board-state moves]
  ; (when flags/runtime-checks?
  ;   (assert (every? valid-move? moves) "Invalid moves passed to move-pieces"))
  (let [current-board-state @board-state
        board-width (:board-width current-board-state)
        orientation (:orientation current-board-state)
        default-speed-ms (:animate-speed-ms current-board-state)
        animate-speed2 800

        ;; add move duration times to the animations
        ; animations2 (map
        ;               (fn [{:keys [source] :as anim}]
        ;                 (let [{:keys [animate] :as move} (get moves-map source)]
        ;                   (cond-> anim
        ;                     true (assoc :duration-ms (convert-animate-speed move default-speed-ms))
        ;                     (false? animate) (assoc :instant? true))))
        ;               animations)

        ;; create an empty JS object to store Promise resolve-fns
        js-resolve-fns (reduce
                         (fn [js-resolves {:keys [id] :as _move}]
                           (gobj/set js-resolves id nil)
                           js-resolves)
                         (js-obj)
                         moves)

        ;; create a collection of Promises: one for each animation
        move-promises (reduce
                        (fn [promises {:keys [id] :as _move}]
                          (conj promises
                                (js/Promise.
                                  (fn [resolve-fn _reject-fn]
                                    ;; store the resolve-fn on our object
                                    (gobj/set js-resolve-fns id resolve-fn)))))
                        []
                        moves)

        ;; attach callbacks to the animations
        moves2 (map
                 (fn [{:keys [id to] :as move}]
                   (assoc move :on-finish
                               (fn []
                                 (let [js-move-info (js-obj

                                                            ; "duration" duration-ms
                                                            ; "from" source
                                                            "to" to
                                                            "id" id)]
                                   ; ;; was a callback function provided for this move?
                                   ; (when-let [callback-fn (get-in moves-map [source :onComplete])]
                                   ;   (when (fn? callback-fn)
                                   ;     (callback-fn js-move-info)))
                                   ;; call the resolve-fn for the Promise
                                   (when-let [resolve-fn (gobj/get js-resolve-fns id)]
                                     (when (fn? resolve-fn)
                                       (resolve-fn js-move-info)))))))
                 moves)


        dom-ops (map
                  (fn [{:keys [id on-finish to] :as _move}]
                    (let [target-square-dimensions (square->dimensions to board-width orientation)]
                      {:defer-fn (fn []
                                   ;; start move animation
                                   (set-style-prop! id "transition" (str "all " animate-speed2 "ms"))


                                   ;; FIXME: these are the wrong coordinates, we need to animate to the center of the square
                                   (set-style-prop! id "left" (str (:left target-square-dimensions) "px"))
                                   (set-style-prop! id "top" (str (:top target-square-dimensions) "px"))
                                   ;; add the callback if provided
                                   (when (fn? on-finish)
                                     (swap! board-state assoc-in [:animation-end-callbacks id]
                                            (fn []
                                              (on-finish)))))
                       :duration-ms animate-speed2}))
                  moves2)]

    ;; apply the DOM operations
    (dom-ops/apply-ops! board-state dom-ops)
    ;; return the move promises
    move-promises))

(defn resize!
  "Takes measurements from the DOM and updates height / width values if necessary"
  [board-state]
  (let [{:keys [items-container-id orientation position squares-container-id items]} @board-state
        items-container-el (dom-util/get-element items-container-id)
        inner-width (dom-util/get-width items-container-el)
        arrows (get-items-by-type board-state "CHESSBOARD_ARROW")]
    ;; update height / width values in board-state
    ;; FIXME: this will need to adjust based on number of rows / columns
    (swap! board-state assoc :board-width inner-width
                             :board-height inner-width)
    ;; set Squares container height
    (dom-util/set-style-prop! squares-container-id "height" (str inner-width "px")))

    ;; FIXME: adjust Custom Items here
  nil)
