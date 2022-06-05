(ns com.oakmac.chessboard2.core
  (:require
    [clojure.string :as str]
    [com.oakmac.chessboard2.animations :refer [calculate-animations]]
    [com.oakmac.chessboard2.css :as css]
    [com.oakmac.chessboard2.html :as html]
    [com.oakmac.chessboard2.util.board :refer [start-position]]
    [com.oakmac.chessboard2.util.dom :as dom-util :refer [add-class! append-html! remove-class! remove-element! set-style-prop!]]
    [com.oakmac.chessboard2.util.fen :refer [fen->position position->fen valid-fen?]]
    [com.oakmac.chessboard2.util.functions :refer [defer]]
    [com.oakmac.chessboard2.util.ids :refer [random-id]]
    [com.oakmac.chessboard2.util.pieces :refer [random-item-id random-piece-id]]
    [com.oakmac.chessboard2.util.predicates :refer [fen-string? start-string? valid-color? valid-move? valid-square? valid-position?]]
    [com.oakmac.chessboard2.util.squares :refer [create-square-el-ids square->dimensions]]
    [com.oakmac.chessboard2.util.string :refer [safe-lower-case]]
    [goog.dom :as gdom]
    [goog.object :as gobj]))

;; TODO
;; - add showNotation config option
;; - .move() should accept an Object
;;   - optional callback function that completes when the move is finished
;;   - animate speed option (per move)
;; - .move('0-0') and .move('0-0-0') should work as expected

;; dev toggle
(def warn-on-extra-items? true)

(def initial-state
  {:items {}
   :num-rows 8
   :num-cols 8
   :orientation "white"
   :position {}})

;; TODO: move to predicates ns
(defn arrow-item? [item]
  (= "CHESSBOARD_ARROW" (:type item)))

;; TODO: move to util namespace
(defn clj->js-map
  "Converts a Clojure Map to a JavaScript Map"
  [clj-map]
  (let [js-map (js/Map.)]
    (doseq [[k v] clj-map]
      (.set js-map (clj->js k) (clj->js v)))
    js-map))

(defn click-root-el [js-evt]
  (.log js/console "clicked root element:" js-evt))

(defn- add-events!
  "Attach DOM events."
  [root-el]
  (.addEventListener root-el "click" click-root-el))

(defn toggle-orientation [o]
  (if (= o "white") "black" "white"))

(defn- draw-position-instant!
  "put pieces inside squares"
  [board-state]
  (let [{:keys [board-width items-container-id orientation piece-square-pct position square->piece-id]} @board-state
        html (atom "")]
    ;; remove existing pieces from the DOM
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
                                     :piece piece
                                     :piece-square-pct piece-square-pct
                                     :square square
                                     :width (/ board-width 8)}))
        (swap! board-state assoc-in [:square->piece-id square] piece-id)))
    (append-html! items-container-id @html)))

(defn- draw-board!
  [{:keys [orientation]}])
  ;; (gobj/set root-el "innerHTML" (str "<h1>" orientation "</h1>")))

(defn get-all-item-elements-from-dom
  [items-container-id]
  (gobj/get (gdom/getElement items-container-id) "children"))

;; PERF: drop this multimethod, use a vanilla cond
(defmulti animation->dom-op
  "Convert an Animation to the DOM operation necessary for it to take place"
  (fn [animation _board-state]
    (:type animation)))

(defmethod animation->dom-op "ANIMATION_ADD"
  [{:keys [piece square] :as animation} board-state]
  (let [{:keys [animate-speed-ms board-width items-container-id piece-square-pct position square-el-ids]} @board-state
        new-piece-id (random-piece-id)
        new-piece-html (html/Piece {:board-width board-width
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
(defmethod animation->dom-op "ANIMATION_MOVE"
  [{:keys [capture? destination piece source] :as animation} board-state]
  (let [{:keys [animate-speed-ms board-width piece-square-pct square->piece-id]} @board-state
        current-piece-id (get square->piece-id source)
        new-piece-id (random-piece-id)
        new-piece-html (html/Piece {:board-width board-width
                                    :id new-piece-id
                                    :hidden? false
                                    :piece piece
                                    :piece-square-pct piece-square-pct
                                    :square source
                                    :width (/ board-width 8)})
        target-square-dimensions (square->dimensions destination board-width)]
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

(defmethod animation->dom-op "ANIMATION_CLEAR"
  [{:keys [piece square] :as animation} board-state]
  (let [{:keys [square->piece-id]} @board-state
        piece-id (get square->piece-id square)]
    {:delete-square->piece square
     :fade-out-piece piece-id}))

(defmethod animation->dom-op :default
  [animation _board-state]
  (js/console.warn "Unknown animation type:" animation))

;; TODO: move this to DOM util?
(defn fade-out-el!
  "fades an element to zero opacity and removes it from the DOM"
  [el-id animate-speed-ms]
  (when-let [el (dom-util/get-element el-id)]
    ;; remove transition
    (set-style-prop! el "transition" "")

    ;; remove the piece from the DOM once the animation finishes
    (.addEventListener el "transitionend"
      (fn []
        (remove-element! el)))

    ;; begin fade out animation on next stack
    (defer
      (fn []
       (set-style-prop! el "transition" (str "all " animate-speed-ms "ms"))
       (set-style-prop! el "opacity" "0")))))

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
          (fade-out-el! piece-id animate-speed-ms))))

    ;; captures
    (let [captures (map :capture-piece-id ops)]
      (doseq [el-id captures]
        (fade-out-el! el-id animate-speed-ms)))

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

;; TODO: change to green arrows?
(def default-arrow-color "#888") ;; try #003088
(def default-arrow-opacity 0.4)
(def default-arrow-size 0.4) ;; TODO: do "small", "medium", "large" here?
(def default-arrow-config
  {:color default-arrow-color})

(defn- looks-like-a-js-arrow-config? [js-cfg]
  (and (object? js-cfg)
       (valid-square? (gobj/get js-cfg "start"))
       (valid-square? (gobj/get js-cfg "end"))))

(defn move->map
  "Converts a move String to a map"
  [m]
  (let [arr (.split m "-")]
    {:start (aget arr 0)
     :end (aget arr 1)}))

(defn add-arrow
  "Adds an analysis arrow to the board. Returns the id of the new arrow."
  [board-state {:keys [color end opacity size start] :as arrow-config}]
  (let [{:keys [board-width]} @board-state
        id (random-id "item")
        arrow-item {:id id
                    :type "CHESSBOARD_ARROW"
                    :start start
                    :end end
                    :color color
                    :opacity opacity}
        arrow-html (html/Arrow {:board-width board-width
                                :color color
                                :end end
                                :id id
                                :size (case size
                                        "small" 0.3
                                        "medium" 0.5
                                        "large" 0.7
                                        0.5)
                                :opacity opacity
                                :start start})]
    (apply-dom-ops! board-state [{:new-html arrow-html}])
    (swap! board-state assoc-in [:items id] arrow-item)
    id))

(defn remove-arrow
  "Remove an Analysis Arrow from the board"
  [board-state item-id]
  (apply-dom-ops! board-state [{:remove-el item-id}])
  (swap! board-state update-in [:items] dissoc item-id))

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

(defn valid-size? [s]
  (case s
    "small" true
    "medium" true
    "large" true
    false))

(defn js-add-arrow
  [board-state arg1 arg2 arg3]
  (let [cfg (cond-> default-arrow-config
              (valid-move? arg1) (merge (move->map arg1))
              (valid-color? arg2) (merge {:color arg2})
              (valid-size? arg2) (merge {:size arg2})
              (valid-size? arg3) (merge {:size arg3})
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
  "Returns the Arrow Items on the board as either an Array (default), JS Object, or JS Map"
  [board-state return-fmt]
  (let [arrows (get-arrows board-state)
        lc-return-fmt (safe-lower-case return-fmt)]
    (case lc-return-fmt
      "object" (clj->js arrows)
      "map" (clj->js-map arrows)
      (clj->js (vec (vals arrows))))))

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
    (draw-position-instant! board)))

(defn set-black-orientation!
  [board]
  (let [squares-selector (str "#" (:container-id @board) " ." css/squares)
        squares-el (dom-util/get-element squares-selector)]
    (remove-class! squares-el css/orientation-white)
    (add-class! squares-el css/orientation-black)
    (swap! board assoc :orientation "black")
    (draw-position-instant! board)))

(defn orientation
 ([board]
  (orientation board nil))
 ([board arg]
  (let [lc-arg (safe-lower-case arg)
        squares-selector (str "#" (:container-id @board) " ." css/squares)]
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
  (draw-position-instant! board-state))

(defn set-position!
  "Sets a new position on the board"
  [board-state new-pos animate?]
  (if animate?
    (set-position-with-animations! board-state new-pos)
    (set-position-instant! board-state new-pos))
  (when warn-on-extra-items?
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
         opts3 (assoc opts2 :root-el root-el
                            :animate-speed-ms default-animate-speed-ms
                            :board-height root-width
                            :board-width root-width
                            :container-id container-id
                            :items {}
                            :items-container-id (str (random-uuid))
                            :piece-square-pct 0.9
                            :square->piece-id {}
                            :square-el-ids square-el-ids)
         ;; create an atom per instance to track the state of the board
         board-state (atom opts3)]
     (init-dom! @board-state)
     (add-events! root-el)
     (draw-position-instant! board-state)
     ;; return a JS object that implements the API
     (js-obj
       ;; TODO: do we need to animate arrows from square to square?
       ;;       I think this might be worth prototyping at least to see the effect
       ;; TODO: do we need to allow a method for setting multiple arrows in one call?
       "addArrow" (partial js-add-arrow board-state)
       "arrows" #() ;; FIXME: returns an object of the arrows on the board
       "clearArrows" (partial clear-arrows board-state)
       "getArrows" (partial js-get-arrows board-state)
       "removeArrow" (partial js-remove-arrow board-state)
       "moveArrow" (partial js-move-arrow board-state)

       "addCircle" #() ;; FIXME: add a circle to the board
       "circles" #() ;; FIXME: returns an object of the circles on the board
       "clearCircles" #() ;; FIXME
       "getCircles" #() ;; FIXME: returns a collection of all the Circles on the board
       "removeCircle" #() ;; FIXME: remove a circle from the board

       "config" #() ;; FIXME
       "getConfig" #() ;; FIXME
       "setConfig" #() ;; FIXME

       "addItem" #() ;; FIXME: add an item to the board
       "clearItems" #() ;; FIXME
       "getItems" #() ;; FIXME: return all items on the board (pieces, arrows, circles, etc)
       "items" #() ;; FIXME: alias of getItems
       "moveItem" #() ;; FIXME

       "addPiece" #()
       "clearPieces" #() ;; FIXME
       "getPieces" #() ;; FIXME
       "pieces" #() ;; FIXME: returns an object of the pieces on the board
       "removePiece" #()

       "clear" #(position board-state {} %1)
       "move" #() ;; FIXME
       "movePiece" #() ;; FIXME
       "position" #(position board-state (js->clj %1) %2)

       "bouncePiece" #() ;; FIXME
       "clearSquareHighlights" #() ;; FIXME
       "destroy" #() ;; FIXME
       "fen" #(position board-state "fen" false)
       "flip" #(orientation board-state "flip")
       "flipPiece" #() ;; FIXME: rotate a piece upside down with animation

       "getSquares" #() ;; FIXME: squares can have colors, ids, properties like "black" and "white"
                        ;;        whether they are highlighted or not
       "setSquare" #() ;; FIXME ;; .setSquare('e2', 'blue')
       "squares" #() ;; FIXME

       "getNotation" #() ;; FIXME
       "hideNotation" #() ;; FIXME
       "notation" #() ;; FIXME: returns the current state with 0 arg, allows changing with other args
       "showNotation" #() ;; FIXME
       "toggleNotation" #() ;; FIXME

       "orientation" #(orientation board-state %1)

       "pulsePiece" #() ;; FIXME

       "resize" #()

       "start" #(position board-state start-position %1)))))

;; TODO: support other module exports / formats here
(when (and js/window (not (fn? (gobj/get js/window "Chessboard2"))))
  (gobj/set js/window "Chessboard2" constructor))
