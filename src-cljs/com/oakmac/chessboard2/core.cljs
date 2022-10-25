(ns com.oakmac.chessboard2.core
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [com.oakmac.chessboard2.api :as api]
    [com.oakmac.chessboard2.config :as config]
    [com.oakmac.chessboard2.css :as css]
    [com.oakmac.chessboard2.dom-ops :as dom-ops]
    [com.oakmac.chessboard2.feature-flags :as flags]
    [com.oakmac.chessboard2.html :as html]
    [com.oakmac.chessboard2.js-api :as js-api]
    [com.oakmac.chessboard2.util.board :refer [start-position]]
    [com.oakmac.chessboard2.util.data-transforms :refer [map->js-return-format]]
    [com.oakmac.chessboard2.util.dom :as dom-util :refer [add-class! append-html! remove-class! remove-element!]]
    [com.oakmac.chessboard2.util.fen :refer [fen->position valid-fen?]]
    [com.oakmac.chessboard2.util.ids :refer [random-id]]
    [com.oakmac.chessboard2.util.logging :refer [error-log warn-log]]
    [com.oakmac.chessboard2.util.moves :refer [move->map]]
    [com.oakmac.chessboard2.util.pieces :refer [random-piece-id]]
    [com.oakmac.chessboard2.util.predicates :refer [arrow-item? circle-item? start-string? valid-color? valid-move-string? valid-square? valid-piece? valid-position?]]
    [com.oakmac.chessboard2.util.squares :as square-util]
    [com.oakmac.chessboard2.util.string :refer [safe-lower-case]]
    [goog.array :as garray]
    [goog.dom :as gdom]
    [goog.functions :as gfunctions]
    [goog.object :as gobj]))

(declare percent? size-string->number tshirt-sizes)

;; TODO
;; - need to write a Cypress test for calling .move() while an animation is happening
;; - [ ] .move('0-0') and .move('0-0-0') should work as expected (GitHub Issue #6)

;; NOTE: the transitionend event fires for every CSS property that is transitioned
;; This function fires twice for most (but not all) piece moves (css props 'left' and 'top')
(defn on-transition-end
  "This function fires on every 'transitionend' event inside the root DOM element."
  [board-state js-evt]
  (let [target-el (gobj/get js-evt "target")
        el-id (gobj/get target-el "id")]
    ;; is there an animation-end callback associated with this element?
    (when-let [callback-fn (get-in @board-state [:animation-end-callbacks el-id])]
      ;; execute the callback
      (callback-fn)
      ;; remove callback from the cache
      (swap! board-state update-in [:animation-end-callbacks] dissoc el-id))))

(defn xy->square
  "Returns the square from the provided X, Y Viewport coordinates.
  Returns nil if no square was found."
  [x y square->square-ids]
  (let [square-els-selector (->> square->square-ids
                              vals
                              (map #(str "#" %))
                              (str/join ", "))
        square-els (dom-util/query-select-all square-els-selector)]
    (reduce
      (fn [_acc square-el]
        (when (dom-util/xy-inside-element? square-el x y)
          (-> square-el
              (gobj/get "dataset")
              (gobj/get "squareCoord")
              reduced)))
      nil
      square-els)))

(defn on-touch-start
  "This function fires on every 'touchstart' event inside the root DOM element"
  [board-state js-evt]
  ;; prevent "double-tap to zoom"
  (dom-util/safe-prevent-default js-evt)
  (let [{:keys [onTouchSquare orientation position root-el square->piece-id square->square-ids touchMove]} @board-state
        target-el (gobj/get js-evt "target")
        js-first-touch (aget (gobj/get js-evt "touches") 0)
        clientX (gobj/get js-first-touch "clientX")
        clientY (gobj/get js-first-touch "clientY")

        square (xy->square clientX clientY square->square-ids)

        _ (when flags/runtime-checks?
            (when-not (valid-square? square)
              (error-log "Invalid square in on-touch-start:" square)))

        ;; NOTE: piece may be nil if there is no piece on the square they touched
        piece (get position square)

        ;; call their onTouchSquare function if provided
        on-touchsquare-result (when (fn? onTouchSquare)
                                (let [js-board-info (js-obj "orientation" orientation
                                                            "position" (clj->js position))]
                                  (onTouchSquare square piece js-board-info)))]
    ;; highlight the square and queue a move if touchmove is enabled
    (when (and (true? touchMove)
               (not (false? on-touchsquare-result))
               piece))
      ;; FIXME:
      ;; - highlight the square here?
      ;; - or should this be their responsibility?
      ;; queue their touchmove
      ; (swap! board-state assoc :touch-move-queue1 {:piece piece, :square square}))
    ;; return null
    nil))

;; FIXME: this function should support onMouseDown instead of onTouchSquare
(defn on-mouse-down
  "This function fires on every 'mousedown' event inside the root DOM element"
  [board-state js-evt]
  (dom-util/safe-prevent-default js-evt)
  (let [{:keys [onTouchSquare orientation position root-el square->piece-id square->square-ids touchMove]} @board-state
        target-el (gobj/get js-evt "target")
        clientX (gobj/get js-evt "clientX")
        clientY (gobj/get js-evt "clientY")

        square (xy->square clientX clientY square->square-ids)

        _ (when flags/runtime-checks?
            (when-not (valid-square? square)
              (error-log "Invalid square in on-mouse-down:" square)))

        ;; NOTE: piece may be nil if there is no piece on the square they touched
        piece (get position square)

        ;; call their onTouchSquare function if provided
        on-touchsquare-result (when (fn? onTouchSquare)
                                (let [js-board-info (js-obj "orientation" orientation
                                                            "position" (clj->js position))]
                                  (onTouchSquare square piece js-board-info)))]
    ;; highlight the square and queue a move if touchmove is enabled
    (when (and (true? touchMove)
               (not (false? on-touchsquare-result))
               piece))
      ;; FIXME:
      ;; - highlight the square here?
      ;; - or should this be their responsibility?
      ;; queue their touchmove
      ; (swap! board-state assoc :touch-move-queue1 {:piece piece, :square square}))
    ;; return null
    nil))

(defn- add-events!
  "Attach DOM events."
  [root-el board-state]
  (.addEventListener js/window "resize" (gfunctions/debounce
                                          (fn [] (api/resize! board-state))
                                          10)) ;; TODO: make this debounce value configurable
  (.addEventListener root-el "mousedown" (partial on-mouse-down board-state))
  (.addEventListener root-el "touchstart" (partial on-touch-start board-state))
  (.addEventListener root-el "transitionend" (partial on-transition-end board-state)))

;; TODO: move this to util ns
(defn toggle-orientation [o]
  (if (= o "white") "black" "white"))

(defn get-all-item-elements-from-dom
  "returns an Array of all Item elements currently in the DOM"
  [items-container-id]
  (gobj/get (gdom/getElement items-container-id) "children"))

(defn get-item-el-ids
  "returns a Set of all the Item ids currently in the DOM"
  [items-container-id]
  (let [item-els (get-all-item-elements-from-dom items-container-id)
        ;; TODO: make this transient for perf reasons?
        ids (atom #{})]
    (garray/forEach item-els
      (fn [itm]
        (swap! ids conj (gobj/get itm "id"))))
    @ids))

;; TODO:
;; - [ ] move this to dom-ops
;; - [ ] need to decide if Pieces are considered Items or not
;; - [ ] refactor this function to not use an atom
(defn- draw-items-instant!
  "Update all Items in the DOM instantly (ie: no animation)"
  [board-state]
  (let [{:keys [board-width items items-container-id orientation piece-square-pct position square->piece-id]} @board-state
        html (atom "")]
    ;; remove existing Items from the DOM
    (doseq [item-id (keys items)]
      (remove-element! item-id))
    ;; remove existing Pieces from the DOM
    (doseq [el-id (vals square->piece-id)]
      (remove-element! el-id))
    ;; clear the :square->piece-id map
    (swap! board-state assoc :square->piece-id {})
    (doseq [[square piece] position]
      ;; TODO: can do this without an atom
      (let [piece-id (random-piece-id)]
        (swap! html str (html/Piece {:board-orientation orientation
                                     :board-width board-width
                                     :id piece-id
                                     :hidden? false
                                     :piece piece
                                     :piece-square-pct piece-square-pct
                                     :square square
                                     :width (/ board-width 8)}))
        (swap! board-state assoc-in [:square->piece-id square] piece-id)))
    ;; add Items back
    (doseq [item (vals items)]
      (cond
        (arrow-item? item)
        (swap! html str (html/Arrow (merge
                                      item
                                      {:board-width board-width
                                       :orientation orientation})))

        (circle-item? item)
        (swap! html str (html/Circle (merge
                                       item
                                       {:board-width board-width
                                        :orientation orientation})))

        ;; FIXME: we will need to support Custom Items here

        :else (js/console.warn "draw-items-instant! Unrecognized Item type:" (pr-str item))))
    (append-html! items-container-id @html)))

;; -----------------------------------------------------------------------------
;; API Methods



(defn get-circles-by-square
  "Returns a Map of Circles with their square as the key."
  [board-state]
  (let [circles (->> @board-state
                     :items
                     vals
                     (filter circle-item?))]
    (zipmap (map :square circles) circles)))

(defn js-get-circles
  "Returns the Circle Items on the board as either a JS Array (default), JS Object, or JS Map"
  [board-state return-fmt]
  (map->js-return-format (api/get-items-by-type board-state "CHESSBOARD_CIRCLE")
                         (safe-lower-case return-fmt)))

(defn remove-circle-by-id
  "Removes a Circle from the board using it's item-id"
  [board-state item-id]
  (dom-ops/apply-ops! board-state [{:remove-el item-id}])
  (swap! board-state update-in [:items] dissoc item-id))

(defn remove-circle
  "Remove a Circle from the board"
  [board-state item-id-or-square]
  (if (valid-square? item-id-or-square)
    (let [square->circles-map (get-circles-by-square board-state)]
      (when-let [circle (get square->circles-map item-id-or-square)]
        (remove-circle-by-id board-state (:id circle))))
    (remove-circle-by-id board-state item-id-or-square)))

;; FIXME: this should be variadic as well as accept an array of circle ids
(defn js-remove-circle
  [board-state item-id-or-square]
  ;; TODO: validation here, warn if item-id is not valid
  (remove-circle board-state item-id-or-square))

;; TODO: this function could be combined with clear-arrows
(defn clear-circles
  "Removes all Circles from the board"
  [board-state]
  (let [item-ids (->> @board-state
                      :items
                      vals
                      (filter circle-item?)
                      (map :id))
        dom-ops (map
                  (fn [id]
                    {:remove-el id})
                  item-ids)]
    (dom-ops/apply-ops! board-state dom-ops)
    (swap! board-state update-in [:items]
           (fn [items]
             (apply dissoc items item-ids)))
    nil))

(defn add-circle
  "Adds a Circle to the board. Returns the id of the new Circle."
  [board-state {:keys [color id opacity size square] :as _circle-config}]
  (when flags/runtime-checks?
    (assert (valid-square? square) (str "Invalid square passed to add-circle:" (pr-str square))))
  (let [{:keys [board-width orientation]} @board-state
        id (or id (random-id "item"))
        size (size-string->number size)
        circle-item {:color color
                     :id id
                     :opacity opacity
                     :size size
                     :square square
                     :type "CHESSBOARD_CIRCLE"}
        circle-html (html/Circle {:board-width board-width
                                  :color color
                                  :id id
                                  :opacity opacity
                                  :orientation orientation
                                  :size size
                                  :square square})]
    (dom-ops/apply-ops! board-state [{:new-html circle-html}])
    (swap! board-state assoc-in [:items id] circle-item)
    id))

(defn add-or-replace-circle
  "Adds a Circle to the board. Returns the id of the new Circle.
  If there is already a Circle on the board on that square, it will be replaced."
  [board-state {:keys [square] :as circle-config}]
  (let [square->circles-map (get-circles-by-square board-state)]
    (if-let [current-circle (get square->circles-map square)]
      (do
        (remove-circle-by-id board-state (:id current-circle))
        (add-circle board-state (assoc circle-config :id (:id current-circle))))
      (add-circle board-state circle-config))))

(def default-circle-config
  {:color "#777"
   :opacity 0.8
   :size "small"})

(defn- looks-like-a-js-circle-config? [js-cfg]
  (and (object? js-cfg)
       (valid-square? (gobj/get js-cfg "square"))))

;; TODO: can be combined with valid-arrow-size?
(defn valid-circle-size? [s]
  (or (contains? tshirt-sizes s)
      (percent? s)))

;; TODO: move this to js-api ns and add unit tests for the argument parsing
(defn js-add-circle
  [board-state arg1 arg2 arg3]
  (let [cfg (cond-> default-circle-config
              (valid-square? arg1) (merge {:square arg1})
              (and (valid-color? arg2)
                   (not (valid-circle-size? arg2)))
              (merge {:color arg2})
              (valid-circle-size? arg2) (merge {:size arg2})
              (valid-circle-size? arg3) (merge {:size arg3})
              (looks-like-a-js-circle-config? arg1) (merge (js->clj arg1 :keywordize-keys true)))]
    (if (valid-square? (:square cfg))
      (add-or-replace-circle board-state cfg)
      (warn-log "Invalid square passed to .addCircle() method:" (:square cfg)))))

(def default-arrow-config
  {:color "#777"
   :opacity 0.8
   :size "large"})

(defn- looks-like-a-js-arrow-config? [js-cfg]
  (and (object? js-cfg)
       (valid-square? (gobj/get js-cfg "start"))
       (valid-square? (gobj/get js-cfg "end"))))

(def tshirt-sizes
  #{"small" "medium" "large"})

(defn percent? [n]
  (and (number? n)
       (>= n 0)))

(defn valid-arrow-size? [s]
  (or (contains? tshirt-sizes s)
      (percent? s)))

(defn size-string->number
  "Converts a size string to a number. Does nothing if s is already a number."
  [s]
  (let [lc-s (safe-lower-case s)]
    (cond
      (= lc-s "small") 0.4
      (= lc-s "medium") 0.7
      (= lc-s "large") 0.9
      (number? s) s
      ;; NOTE: this should never happen
      :else 0.9)))

(defn add-arrow
  "Adds an analysis arrow to the board. Returns the id of the new arrow."
  [board-state {:keys [color end opacity size start] :as _arrow-config}]
  (let [{:keys [board-width orientation]} @board-state
        id (random-id "item")
        size (size-string->number size)
        arrow-item {:id id
                    :color color
                    :end end
                    :opacity opacity
                    :size size
                    :start start
                    :type "CHESSBOARD_ARROW"}
        arrow-html (html/Arrow {:board-width board-width
                                :color color
                                :end end
                                :id id
                                :opacity opacity
                                :orientation orientation
                                :size size
                                :start start})]
    (dom-ops/apply-ops! board-state [{:new-html arrow-html}])
    (swap! board-state assoc-in [:items id] arrow-item)
    id))

(defn remove-arrow
  "Remove an Analysis Arrow from the board"
  [board-state item-id]
  (dom-ops/apply-ops! board-state [{:remove-el item-id}])
  (swap! board-state update-in [:items] dissoc item-id))

(defn get-arrows-on-squares
  "Returns a map of the Arrows on the given squares"
  [board-state {:keys [start end]}]
  (reduce
    (fn [arrows [item-id itm]]
      (if (and (= (:type itm) "CHESSBOARD_ARROW")
               (= (:start itm) start)
               (= (:end itm) end))
        (assoc arrows item-id itm)
        arrows))
    {}
    (:items @board-state)))

(defn js-remove-arrow
  [board-state item-id-or-squares]
  (cond
    (valid-move-string? item-id-or-squares)
    (let [arrows (get-arrows-on-squares board-state (move->map item-id-or-squares "ARROW_FORMAT"))]
      (doseq [item-id (keys arrows)]
        (remove-arrow board-state item-id)))

    :else
    (if-let [_arrow (get-in @board-state [:items item-id-or-squares])]
      (remove-arrow board-state item-id-or-squares)
      ;; TODO: error code here?
      (warn-log "Invalid argument passed to removeArrow():" item-id-or-squares))))

(defn move-arrow
  "Move an Analysis Arrow on the board"
  [board-state item-id new-move])
  ;; TODO: apply-dom-ops

(defn js-move-arrow
  [board-state item-id new-move]
  ;; TODO: validation here, warn if item-id is not valid
  (valid-move-string? new-move)
  ;; TODO: (move->map new-move) ??
  (move-arrow board-state item-id new-move))

(defn js-add-arrow
  [board-state arg1 arg2 arg3]
  (let [cfg (cond-> default-arrow-config
              (valid-move-string? arg1) (merge (move->map arg1 "ARROW_FORMAT"))
              (and (valid-color? arg2)
                   (not (valid-arrow-size? arg2)))
              (merge {:color arg2})
              (valid-arrow-size? arg2) (merge {:size arg2})
              (valid-arrow-size? arg3) (merge {:size arg3})
              (looks-like-a-js-arrow-config? arg1) (merge (js->clj arg1 :keywordize-keys true)))]
    (add-arrow board-state cfg)))

(defn get-arrows
  "Returns a map of the Arrow Items on the board"
  [board-state]
  (let [arrows (->> @board-state
                    :items
                    vals
                    (filter arrow-item?))]
    (zipmap (map :id arrows) arrows)))

(defn js-get-arrows
  "Returns the Arrow Items on the board as either a JS Array (default), JS Object, or JS Map"
  [board-state return-fmt]
  (map->js-return-format (api/get-items-by-type board-state "CHESSBOARD_ARROW")
                         (safe-lower-case return-fmt)))

(defn get-items
  "Returns a map of the Items on the board."
  [board-state]
  (:items @board-state))

(defn js-get-items
  "Returns the Items on the board as either a JS Array (default), JS Object, or JS Map"
  [board-state return-fmt]
  (map->js-return-format (get-items board-state) (safe-lower-case return-fmt)))

(defn clear-arrows
  "Removes all Analysis Arrows from the board"
  [board-state]
  (let [arrow-ids (->> @board-state
                       :items
                       vals
                       (filter arrow-item?)
                       (map :id))
        dom-ops (map
                  (fn [id]
                    {:remove-el id})
                  arrow-ids)]
    (dom-ops/apply-ops! board-state dom-ops)
    (swap! board-state update-in [:items]
           (fn [items]
             (apply dissoc items arrow-ids)))
    nil))

(defn set-white-orientation!
  [board]
  (let [squares-selector (str "#" (:container-id @board) " ." css/squares)
        squares-el (dom-util/get-element squares-selector)]
    (remove-class! squares-el css/orientation-black)
    (add-class! squares-el css/orientation-white)
    (swap! board assoc :orientation "white")
    (draw-items-instant! board)))

(defn set-black-orientation!
  [board]
  (let [squares-selector (str "#" (:container-id @board) " ." css/squares)
        squares-el (dom-util/get-element squares-selector)]
    (remove-class! squares-el css/orientation-white)
    (add-class! squares-el css/orientation-black)
    (swap! board assoc :orientation "black")
    (draw-items-instant! board)))

(defn orientation
 ([board]
  (orientation board nil))
 ([board arg]
  (let [lc-arg (safe-lower-case arg)]
    (cond
      (= lc-arg "white") (do (set-white-orientation! board)
                             "white")
      (= lc-arg "black") (do (set-black-orientation! board)
                             "black")
      (= lc-arg "flip") (do (swap! board update :orientation toggle-orientation)
                            (let [new-orientation (:orientation @board)]
                              (if (= new-orientation "white")
                                (set-white-orientation! board)
                                (set-black-orientation! board))
                              new-orientation))
      :else (:orientation @board)))))

(defn array-of-moves? [arg]
  (and (array? arg)
       (every? arg valid-move-string?)))

(defn looks-like-a-move-object? [js-move]
  (and (object? js-move)
       (valid-square? (gobj/get js-move "from"))
       (valid-square? (gobj/get js-move "to"))))

;; TODO: this function should accept an Array of squares
;; TODO: this function should accept an Object with "onFinishAnimation" callback
; (defn js-remove-piece
;   [board-state]
;   (let [js-args (array)]
;     (copy-arguments js-args)
;     (.shift js-args)
;     (let [current-pos (:position @board-state)
;           squares-to-remove (set (js->clj js-args))
;           ;; any argument of 'false' to this function means no animation
;           animate? (not-any? false? squares-to-remove)
;           new-position (apply dissoc current-pos squares-to-remove)]
;       (position board-state new-position animate?))))

(defn js-get-pieces
  "Returns the Pieces on the board as either a JS Array (default), JS Object, or JS Map"
  [board-state return-fmt]
  ;; FIXME: this does not work correctly because pieces are not Items
  (map->js-return-format (api/get-items-by-type board-state "CHESSBOARD_PIECE") (safe-lower-case return-fmt)))

(defn valid-add-piece-config? [cfg]
  (and (valid-piece? (:piece cfg))
       (valid-square? (:square cfg))))

; (defn add-piece
;   [board-state {:keys [animate? piece square] :as add-piece-cfg}]
;   (if-not (valid-add-piece-config? add-piece-cfg)
;     (js/console.warn "FIXME ERROR CODE: Invalid arguments passed to the .addPiece() method")
;     (let [current-pos (:position @board-state)
;           new-pos (assoc current-pos square piece)]
;       (position board-state new-pos animate?))))

(defn- looks-like-a-js-add-piece-config? [js-cfg]
  (and (object? js-cfg)
       (valid-piece? (gobj/get js-cfg "piece"))
       (valid-square? (gobj/get js-cfg "square"))))

(def default-add-piece-config
  {:animate? true})
   ;; TODO: onAnimationComplete callback here

; (defn js-add-piece
;   [board-state arg1 arg2 arg3]
;   (let [cfg (cond-> default-add-piece-config
;               (valid-square? arg1) (merge {:square arg1})
;               (valid-piece? arg2) (merge {:piece arg2})
;               (false? arg3) (merge {:animate? arg3})
;               (looks-like-a-js-add-piece-config? arg1) (merge (js->clj arg1 :keywordize-keys true)))]
;     (add-piece board-state cfg)))

(defn hide-coordinates!
  [board-state]
  (swap! board-state assoc :show-coords? false))
  ; (let [container-id (:container-id @board-state)]
  ;   (add-class! (dom-util/get-element container-id) "hide-notation-cbe71")
  ;   (swap! board-state assoc :show-notation? false)))

(defn show-coordinates!
  [board-state]
  (swap! board-state assoc :show-coords? true))
  ; (let [container-id (:container-id @board-state)]
  ;   (remove-class! (dom-util/get-element container-id) "hide-notation-cbe71")
  ;   (swap! board-state assoc :show-notation? true)))

(defn toggle-coordinates!
  [board-state]
  (swap! board-state update :show-coords? not))
  ; (if (:show-notation? @board-state)
  ;   (hide-coordinates! board-state)
  ;   (show-coordinates! board-state)))

;; -----------------------------------------------------------------------------
;; Constructor

(defn init-dom!
  [board-state]
  (let [{:keys [items-container-id root-el squares-container-id] :as board-cfg} @board-state]
    ;; write the container <div>s to the DOM
    (dom-util/set-inner-html! root-el (html/BoardContainer board-cfg))
    ;; NOTE: I think we could just call api/resize! here
    ;; take some measurements
    (let [items-container-el (dom-util/get-element items-container-id)
          inner-width (dom-util/get-width items-container-el)]
      ;; update the inner height / width
      ;; FIXME: this will need to adjust based on number of rows / columns
      (swap! board-state assoc :board-width inner-width
                               :board-height inner-width)
      ;; update the Squares container height to fill the space
      (dom-util/set-style-prop! squares-container-id "height" (str inner-width "px")))))

;; :coords true => default

;; recommend they style the coordinate text using CSS
;; the chessboard API can support the "on/off" and position stuff
(def default-coords
  {:bottom {:position "outside", :show? false, :type "letters"}
   :left   {:position "outside", :show? false, :type "numbers"}
   :right  {:position "outside", :show? false, :type "numbers"}
   :top    {:position "outside", :show? false, :type "letters"}})

(def default-animate-speed-ms 80)
; (def default-animate-speed-ms 2500)

(defn board-state-change
  [_key _atom old-state new-state]
  (when new-state
    ;; FIXME: board orientation
    ;; FIXME: coordinate config change
    ;; show / hide coordinates
    (when-not (= (:show-coords? old-state) (:show-coords? new-state))
      (if (:show-coords? new-state)
         (remove-class! (dom-util/get-element (:container-id new-state)) "hide-notation-cbe71")
         (add-class! (dom-util/get-element (:container-id new-state)) "hide-notation-cbe71")))))

(defn constructor2
  [root-el js-opts]
  (let [;; create some random IDs for internal elements
        container-id (random-id "container")
        items-container-id (random-id "items-container")
        squares-container-id (random-id "squares-container")

        root-width (dom-util/get-width root-el)

        their-config (js-api/parse-constructor-second-arg js-opts)
        starting-config (config/merge-config their-config)
        default-num-cols 8
        square->square-ids (square-util/create-random-square-ids default-num-cols default-num-cols)
        opts3 (assoc starting-config
                :animate-speed-ms default-animate-speed-ms
                :animation-end-callbacks {}
                :container-id container-id
                :coords default-coords
                :items {}
                :items-container-id items-container-id
                :num-cols default-num-cols
                :num-rows default-num-cols
                :piece-square-pct 0.9
                :root-el root-el
                :show-coords? true ;; are the Coordinates showing?
                :square->piece-id {}
                :square->square-ids square->square-ids
                :squares-container-id squares-container-id)
        ;; create an atom to track the state of the board
        board-state (atom opts3)]

    ;; add a watch function to the board state
    (add-watch board-state :on-change board-state-change)

    ;; Initial DOM Setup
    (init-dom! board-state)
    (add-events! root-el board-state)
    (draw-items-instant! board-state)

    ;; return a JS object that implements the API
    (js-obj
      ;; TODO: do we need to animate arrows from square to square?
      ;;       I think this might be worth prototyping at least to see the effect
      ;; TODO: do we need to allow a method for setting multiple arrows in one call?
      "addArrow" (partial js-add-arrow board-state)
      "arrows" (partial js-get-arrows board-state)
      "clearArrows" (partial clear-arrows board-state)
      "getArrows" (partial js-get-arrows board-state)
      "removeArrow" (partial js-remove-arrow board-state)
      ;; TODO: should this method exist? would be neat to prototype it and see the effect
      ; "moveArrow" (partial js-move-arrow board-state)

      "addCircle" (partial js-add-circle board-state)
      "circles" (partial js-get-circles board-state)
      "clearCircles" (partial clear-circles board-state)
      "getCircles" (partial js-get-circles board-state)
      "removeCircle" (partial js-remove-circle board-state)

      ;; FIXME: implement these
      ; https://github.com/oakmac/chessboard2/issues/7
      ; "config" #()
      ; "getConfig" #()
      ; "setConfig" #()

      ;; FIXME: allow adding custom items
      ;; https://github.com/oakmac/chessboard2/issues/9
      "addItem" (partial js-api/add-item board-state)
      "clearItems" #() ;; FIXME
      "getItems" (partial js-get-items board-state) ;; FIXME: should be able to pass the string type here as an argument
      "items" (partial js-get-items board-state)
      "moveItem" (partial js-api/move-item board-state)
      "removeItem" (partial js-api/remove-item board-state)

      ; "addPiece" (partial js-add-piece board-state) ;; FIXME: write this
      "clearPieces" (partial js-api/clear board-state)
      "getPieces" (partial js-get-pieces board-state)
      "pieces" (partial js-get-pieces board-state)
      ; "removePiece" (partial js-remove-piece board-state) ;; FIXME: write this

      "clear" (partial js-api/clear board-state)
      "move" (partial js-api/move-piece board-state)
      ;; FIXME: moveInstant ???
      "position" (partial js-api/position board-state)
      "getPosition" (partial js-api/get-position board-state)
      "setPosition" (partial js-api/set-position board-state)

      "destroy" (partial api/destroy! board-state)
      "fen" (partial js-api/fen board-state)

      "clearSquareHighlights" #() ;; FIXME - should this just be "clearSquares" ?
      ;; would also be nice if this returned x,y coordinates of the squares, for custom
      ;; integrations
      "getSquares" #() ;; FIXME: squares can have colors, ids, properties like "black" and "white"
                       ;;        whether they are highlighted or not
      "setSquare" #() ;; FIXME ;; .setSquare('e2', 'blue')
      "squares" #() ;; FIXME

      "coordinates" #() ;; FIXME: returns the current state with 0 arg, allows changing with other args
      "getCoordinates" #() ;; FIXME: returns the config object
      "hideCoordinates" (partial hide-coordinates! board-state)
      "setCoordinates" #() ;; FIXME: sets the config object (do we need this? just use .setConfig() ?)
      "showCoordinates" (partial show-coordinates! board-state)
      "toggleCoordinates" (partial toggle-coordinates! board-state)

      "flip" (partial orientation board-state "flip")
      "orientation" (partial orientation board-state)
      "getOrientation" (partial orientation board-state nil)
      "setOrientation" (partial orientation board-state)

      "animatePiece" #() ;; FIXME:
      "bouncePiece" #() ;; FIXME
      "flipPiece" #() ;; FIXME: rotate a piece upside down with animation
      "pulsePiece" #() ;; FIXME

      "resize" (partial api/resize! board-state)

      "start" (partial js-api/start board-state))))

(defn constructor
  "Called to create a new Chessboard2 object."
  ([]
   (error-log "Please pass a DOM element, element ID, or query selector as the first argument to the Chessboard2() function."))
  ([el]
   (constructor el {}))
  ([el js-opts]
   (let [root-el (dom-util/get-element el)]
     (if-not root-el
       (error-log "Unable to find DOM element:" el)
       (constructor2 root-el js-opts)))))

;; put Chessboard2 on the window object
(when (and (exists? js/window)
           (not (fn? (gobj/get js/window "Chessboard2"))))
  (gobj/set js/window "Chessboard2" constructor))

;; common JS export
(when (and (exists? js/exports)
           (not= (type (gobj/get js/exports "nodeName")) "string"))
  (gobj/set js/exports "Chessboard2" constructor))

;; TODO: do we need to support AMD?
