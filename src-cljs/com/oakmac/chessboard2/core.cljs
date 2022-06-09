(ns com.oakmac.chessboard2.core
  (:require
    [com.oakmac.chessboard2.animations :refer [calculate-animations]]
    [com.oakmac.chessboard2.css :as css]
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
    [com.oakmac.chessboard2.util.predicates :refer [fen-string? start-string? valid-color? valid-move? valid-square? valid-piece? valid-position?]]
    [com.oakmac.chessboard2.util.squares :refer [create-square-el-ids square->dimensions]]
    [com.oakmac.chessboard2.util.string :refer [safe-lower-case]]
    [goog.array :as garray]
    [goog.dom :as gdom]
    [goog.object :as gobj]))

(declare percent? size-string->number tshirt-sizes)

;; TODO
;; - add showNotation config option
;; - .move() should accept an Object
;;   - optional callback function that completes when the move is finished
;;   - animate speed option (per move)
;; - .move('0-0') and .move('0-0-0') should work as expected

(def initial-state
  {:items {}
   :num-rows 8
   :num-cols 8
   :orientation "white"
   :position {}})

;; TODO: move to predicates ns
(defn arrow-item? [item]
  (= "CHESSBOARD_ARROW" (:type item)))

(defn circle-item? [item]
  (= "CHESSBOARD_CIRCLE" (:type item)))

(defn piece-item? [item]
  (= "CHESSBOARD_PIECE" (:type item)))

(defn click-root-el [js-evt]
  (.log js/console "clicked root element:" js-evt))

(defn- add-events!
  "Attach DOM events."
  [root-el]
  (.addEventListener root-el "click" click-root-el))

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
        ids (atom #{})]
    (garray/forEach item-els
      (fn [itm]
        (swap! ids conj (gobj/get itm "id"))))
    @ids))

;; TODO: I think we need to convert this function to use only dom-ops
;; need to decide if Pieces are considered Items or not
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
      ;; FIXME: what to do about other Item types here?
      (swap! html str (html/Arrow (merge
                                    item
                                    {:board-width board-width
                                     :orientation orientation}))))
    (append-html! items-container-id @html)))

(defn animation->dom-op-add
  [{:keys [piece square] :as _animation} board-state]
  (let [{:keys [animate-speed-ms board-width orientation piece-square-pct]} @board-state
        new-piece-id (random-piece-id)
        new-piece-html (html/Piece {:board-width board-width
                                    :board-orientation orientation
                                    :id new-piece-id
                                    :hidden? true
                                    :piece piece
                                    :piece-square-pct piece-square-pct
                                    :square square
                                    :width (/ board-width 8)})]
    {:new-html new-piece-html
     :defer-fn (fn []
                 ;; start opacity animation after piece has been added to the DOM
                 (set-style-prop! new-piece-id "transition" (str "all " animate-speed-ms "ms"))
                 (set-style-prop! new-piece-id "opacity" "100%"))
     :new-square->piece (hash-map square new-piece-id)}))

;; TODO:
;; - Should we re-use the same DOM element here instead of destroying + creating a new one?
;; - Is it important for item-ids to persist?
(defn animation->dom-op-move
  [{:keys [capture? destination piece source] :as _animation} board-state]
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
        target-square-dimensions (square->dimensions destination board-width orientation)]
    (merge
      {:new-html new-piece-html
       :defer-fn (fn []
                   ;; start move animation
                   (set-style-prop! new-piece-id "transition" (str "all " animate-speed-ms "ms"))
                   (set-style-prop! new-piece-id "left" (str (:left target-square-dimensions) "px"))
                   (set-style-prop! new-piece-id "top" (str (:top target-square-dimensions) "px")))
       :remove-el current-piece-id
       :new-square->piece (hash-map destination new-piece-id)
       :delete-square->piece source}
      (when capture?
        {:capture-piece-id (get square->piece-id destination)}))))

(defn animation->dom-op-clear
  [{:keys [square] :as _animation} board-state]
  (let [{:keys [square->piece-id]} @board-state
        piece-id (get square->piece-id square)]
    {:delete-square->piece square
     :fade-out-piece piece-id}))

;; NOTE: would normally use a defmethod here
;; the output file size is slightly reduced by not including defmethods in the project
;; -- C. Oakman, June 2022
(defn animation->dom-op
  [animation board-state]
  (case (:type animation)
    "ANIMATION_ADD" (animation->dom-op-add animation board-state)
    "ANIMATION_MOVE" (animation->dom-op-move animation board-state)
    "ANIMATION_CLEAR" (animation->dom-op-clear animation board-state)
    (js/console.warn "Unknown animation type:" (:type animation))))

(defn apply-dom-ops!
  "Apply DOM operations to the board"
  [board-state ops]
  (let [{:keys [animate-speed-ms items-container-id]} @board-state]
    ;; remove elements
    (let [removes (map :remove-el ops)]
      (doseq [el-id removes]
        (when el-id
          (remove-element! el-id))))

    ;; append new HTML
    (let [new-html (->> (map :new-html ops)
                        (apply str))]
      (append-html! items-container-id new-html))

    ;; functions to run on the next stack
    (defer (fn []
             (doseq [{:keys [defer-fn]} ops]
               (when (fn? defer-fn) (defer-fn)))))

    ;; piece fade-outs
    (let [fade-outs (map :fade-out-piece ops)]
      (doseq [piece-id fade-outs]
        (when piece-id
          (dom-util/fade-out-and-remove-el! piece-id animate-speed-ms))))

    ;; captures
    (let [captures (map :capture-piece-id ops)]
      (doseq [el-id captures]
        (dom-util/fade-out-and-remove-el! el-id animate-speed-ms)))

    ;; update the board-state with new piece-ids
    (let [dissocs (map :delete-square->piece ops)
          updates (map :new-square->piece ops)]
      (swap! board-state update :square->piece-id
        (fn [m]
          (as-> m $
           (apply dissoc $ dissocs)
           (apply merge $ updates)))))))

;; -----------------------------------------------------------------------------
;; API Methods

;; FIXME: should be able to remove a circle either via square code or id

(defn get-items-by-type
  "Returns a map of <type> Items on the board"
  [board-state type-str]
  (let [items (->> @board-state
                   :items
                   vals
                   (filter #(= type-str (:type %))))]
    (zipmap (map :id items) items)))

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
  (map->js-return-format (get-items-by-type board-state "CHESSBOARD_CIRCLE")
                         (safe-lower-case return-fmt)))

(defn remove-circle-by-id
  "Removes a Circle from the board using it's item-id"
  [board-state item-id]
  (apply-dom-ops! board-state [{:remove-el item-id}])
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
    (apply-dom-ops! board-state dom-ops)
    (swap! board-state update-in [:items]
           (fn [items]
             (apply dissoc items item-ids)))
    nil))

(defn add-circle
  "Adds a Circle to the board. Returns the id of the new Circle."
  [board-state {:keys [color id opacity size square] :as _circle-config}]
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
    (apply-dom-ops! board-state [{:new-html circle-html}])
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
    (add-or-replace-circle board-state cfg)))

(def default-arrow-config
  {:color "#777"
   :opacity 0.8
   :size "large"})

(defn- looks-like-a-js-arrow-config? [js-cfg]
  (and (object? js-cfg)
       (valid-square? (gobj/get js-cfg "start"))
       (valid-square? (gobj/get js-cfg "end"))))

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
    (apply-dom-ops! board-state [{:new-html arrow-html}])
    (swap! board-state assoc-in [:items id] arrow-item)
    id))

(defn remove-arrow
  "Remove an Analysis Arrow from the board"
  [board-state item-id]
  (apply-dom-ops! board-state [{:remove-el item-id}])
  (swap! board-state update-in [:items] dissoc item-id))

;; FIXME: you should be able to pass in 'e2-e4' and remove the arrow on those squares
(defn js-remove-arrow
  [board-state item-id]
  ;; TODO: validation here, warn if item-id is not valid
  (remove-arrow board-state item-id))

(defn move-arrow
  "Move an Analysis Arrow on the board"
  [board-state item-id new-move])
  ;; TODO: apply-dom-ops

(defn js-move-arrow
  [board-state item-id new-move]
  ;; TODO: validation here, warn if item-id is not valid
  (valid-move? new-move)
  ;; TODO: (move->map new-move) ??
  (move-arrow board-state item-id new-move))

(defn js-add-arrow
  [board-state arg1 arg2 arg3]
  (let [cfg (cond-> default-arrow-config
              (valid-move? arg1) (merge (move->map arg1 "ARROW_FORMAT"))
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
  (map->js-return-format (get-items-by-type board-state "CHESSBOARD_ARROW")
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
    (apply-dom-ops! board-state dom-ops)
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

(defn set-position-with-animations!
  [board-state new-pos]
  (let [animations (calculate-animations (:position @board-state) new-pos)
        dom-ops (map #(animation->dom-op % board-state) animations)]
    (apply-dom-ops! board-state dom-ops)
    (swap! board-state assoc :position new-pos)))

(defn set-position-instant!
  [board-state new-pos]
  (swap! board-state assoc :position new-pos)
  (draw-items-instant! board-state))

(defn set-position!
  "Sets a new position on the board"
  [board-state new-pos animate?]
  (if animate?
    (set-position-with-animations! board-state new-pos)
    (set-position-instant! board-state new-pos))

  (when flags/runtime-checks?
    (js/setTimeout
      (fn []
        (let [items-els (get-all-item-elements-from-dom (:items-container-id @board-state))]
          (js/console.log (gobj/get items-els "length"))))
          ;; TODO: compare the internal items collection length here
      (+ 50 (:animate-speed-ms @board-state)))))

(defn position
  "returns or sets the current board position"
  [board-state new-pos animate?]
  (let [animate? (not (false? animate?))] ;; the default value for animate? is true
    (cond
      ;; no first argument: return the position as a JS Object
      (not new-pos) (-> @board-state :position clj->js)
      ;; first argument is "fen": return position as a FEN string
      (fen-string? new-pos) (-> @board-state :position position->fen)
      ;; first argument is "start": set the starting position
      (start-string? new-pos) (set-position! board-state start-position animate?)
      ;; new-pos is a fen string
      (valid-fen? new-pos) (set-position! board-state (fen->position new-pos) animate?)
      ;; new-pos is a valid position
      (valid-position? new-pos) (set-position! board-state new-pos animate?)
      ;; ¯\_(ツ)_/¯
      :else
      ;; FIXME: error code here
      (do (js/console.warn "Invalid value passed to the position method:" (clj->js new-pos))
          nil))))

(defn move-piece
  [board-state {:keys [animate?] :as move}]
  (let [new-position (apply-move-to-position (:position @board-state) move)]
    (position board-state new-position animate?)))

(defn array-of-moves? [arg]
  (and (array? arg)
       (every? arg valid-move?)))

(defn looks-like-a-move-object? [js-move]
  (and (object? js-move)
       (valid-square? (gobj/get js-move "from"))
       (valid-square? (gobj/get js-move "to"))))

;; FIXME: handle 0-0 and 0-0-0
;; FIXME: this function should be variadic
(defn js-move-piece
  [board-state arg1]
  (cond
    (valid-move? arg1) (move-piece board-state (move->map arg1 "MOVE_FORMAT"))
    ;; TODO (array-of-moves? arg1) ()
    (looks-like-a-move-object? arg1) (move-piece board-state (js->clj arg1 :keywordize-keys true))
    :else (js/console.warn "FIXME ERROR CODE: Invalid value passed to the .move() method:" arg1)))

;; TODO: this function should accept an Array of squares
;; TODO: this function should accept an Object with "onFinishAnimation" callback
(defn js-remove-piece
  [board-state]
  (let [js-args (array)]
    (copy-arguments js-args)
    (.shift js-args)
    (let [current-pos (:position @board-state)
          squares-to-remove (set (js->clj js-args))
          ;; any argument of 'false' to this function means no animation
          animate? (not-any? false? squares-to-remove)
          new-position (apply dissoc current-pos squares-to-remove)]
      (position board-state new-position animate?))))

(defn js-get-pieces
  "Returns the Pieces on the board as either a JS Array (default), JS Object, or JS Map"
  [board-state return-fmt]
  ;; FIXME: this does not work correctly because pieces are not Items
  (map->js-return-format (get-items-by-type board-state "CHESSBOARD_PIECE") (safe-lower-case return-fmt)))

(defn valid-add-piece-config? [cfg]
  (and (valid-piece? (:piece cfg))
       (valid-square? (:square cfg))))

(defn add-piece
  [board-state {:keys [animate? piece square] :as add-piece-cfg}]
  (if-not (valid-add-piece-config? add-piece-cfg)
    (js/console.warn "FIXME ERROR CODE: Invalid arguments passed to the .addPiece() method")
    (let [current-pos (:position @board-state)
          new-pos (assoc current-pos square piece)]
      (position board-state new-pos animate?))))

(defn- looks-like-a-js-add-piece-config? [js-cfg]
  (and (object? js-cfg)
       (valid-piece? (gobj/get js-cfg "piece"))
       (valid-square? (gobj/get js-cfg "square"))))

(def default-add-piece-config
  {:animate? true})
   ;; TODO: onAnimationComplete callback here

(defn js-add-piece
  [board-state arg1 arg2 arg3]
  (let [cfg (cond-> default-add-piece-config
              (valid-square? arg1) (merge {:square arg1})
              (valid-piece? arg2) (merge {:piece arg2})
              (false? arg3) (merge {:animate? arg3})
              (looks-like-a-js-add-piece-config? arg1) (merge (js->clj arg1 :keywordize-keys true)))]
    (add-piece board-state cfg)))

;; -----------------------------------------------------------------------------
;; Constructor

(defn init-dom!
  [{:keys [root-el] :as board-cfg}]
  (gobj/set root-el "innerHTML" (html/BoardContainer board-cfg)))

(def valid-config-keys
  #{"position"})

(defn- expand-second-arg
  "expands shorthand versions of the second argument"
  [js-opts]
  (let [opts (js->clj js-opts)]
    (cond
      (start-string? opts)
      {:position start-position}

      (valid-fen? opts)
      {:position (fen->position opts)}

      (valid-position? opts)
      {:position opts}

      (map? opts)
      (let [opts2 (select-keys opts valid-config-keys)
            their-pos (get opts2 "position")]
        (cond-> {}
          (start-string? their-pos)   (assoc :position start-position)
          (valid-fen? their-pos)      (assoc :position (fen->position their-pos))
          (valid-position? their-pos) (assoc :position their-pos)))
          ;; FIXME: add other configs values here

      :else
      {})))

(def default-animate-speed-ms 120)
; (def default-animate-speed-ms 2500)

(defn constructor
  "Called to create a new Chessboard2 object."
  ([el-id]
   (constructor el-id {}))
  ([el js-opts]
   (let [root-el (dom-util/get-element el)
         ;; FIXME: fail if the DOM element does not exist
         container-id (random-id "container")
         root-width (dom-util/get-width root-el)
         opts1 (expand-second-arg js-opts)
         square-el-ids (create-square-el-ids (:num-rows initial-state)
                                             (:num-cols initial-state))
         opts2 (merge initial-state opts1)
         items-container-id (random-id "items-container")
         opts3 (assoc opts2 :root-el root-el
                            :animate-speed-ms default-animate-speed-ms
                            :board-height root-width
                            :board-width root-width
                            :container-id container-id
                            :items {}
                            :items-container-id items-container-id
                            :piece-square-pct 0.9
                            :square->piece-id {}
                            :square-el-ids square-el-ids)
         ;; create an atom per instance to track the state of the board
         board-state (atom opts3)]

     ;; Initial DOM Setup
     (init-dom! @board-state)
     (add-events! root-el)
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
       "moveArrow" (partial js-move-arrow board-state)

       "addCircle" (partial js-add-circle board-state)
       "circles" (partial js-get-circles board-state)
       "clearCircles" (partial clear-circles board-state)
       "getCircles" (partial js-get-circles board-state)
       "removeCircle" (partial js-remove-circle board-state)

       "config" #() ;; FIXME
       "getConfig" #() ;; FIXME
       "setConfig" #() ;; FIXME

       "addItem" #() ;; FIXME: add a custom Item to the board
       "clearItems" #() ;; FIXME
       "getItems" (partial js-get-items board-state)
       "items" (partial js-get-items board-state)
       "moveItem" #() ;; FIXME

       "addPiece" (partial js-add-piece board-state)
       "clearPieces" #(position board-state {} %1)
       "getPieces" (partial js-get-pieces board-state)
       "pieces" (partial js-get-pieces board-state)
       "removePiece" (partial js-remove-piece board-state)

       "clear" #(position board-state {} %1)
       "move" (partial js-move-piece board-state)
       "movePiece" (partial js-move-piece board-state)
       "position" #(position board-state (js->clj %1) %2)

       "destroy" #() ;; FIXME
       "fen" #(position board-state "fen" false)

       "clearSquareHighlights" #() ;; FIXME - should this just be "clearSquares" ?
       "getSquares" #() ;; FIXME: squares can have colors, ids, properties like "black" and "white"
                        ;;        whether they are highlighted or not
       "setSquare" #() ;; FIXME ;; .setSquare('e2', 'blue')
       "squares" #() ;; FIXME

       "getNotation" #() ;; FIXME
       "hideNotation" #() ;; FIXME
       "notation" #() ;; FIXME: returns the current state with 0 arg, allows changing with other args
       "showNotation" #() ;; FIXME
       "toggleNotation" #() ;; FIXME

       ;; TODO: do we need getOrientation and setOrientation?
       "flip" #(orientation board-state "flip")
       "orientation" #(orientation board-state %1)

       "animatePiece" #() ;; FIXME:
       "bouncePiece" #() ;; FIXME
       "flipPiece" #() ;; FIXME: rotate a piece upside down with animation
       "pulsePiece" #() ;; FIXME

       "resize" #() ;; FIXME

       "start" #(position board-state start-position %1)))))

;; TODO: support other module exports / formats here
(when (and js/window (not (fn? (gobj/get js/window "Chessboard2"))))
  (gobj/set js/window "Chessboard2" constructor))
