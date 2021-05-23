(ns com.oakmac.chessboard2.core
  (:require
    [com.oakmac.chessboard2.animations :refer [calculate-animations]]
    [com.oakmac.chessboard2.html :as html]
    [com.oakmac.chessboard2.util.board :refer [start-position]]
    [com.oakmac.chessboard2.util.dom :as dom-util :refer [append-html! remove-class! remove-element! set-style-prop!]]
    [com.oakmac.chessboard2.util.fen :refer [fen->position position->fen valid-fen?]]
    [com.oakmac.chessboard2.util.pieces :refer [random-piece-id]]
    [com.oakmac.chessboard2.util.predicates :refer [fen-string? start-string? valid-position?]]
    [com.oakmac.chessboard2.util.squares :refer [create-square-el-ids square->dimensions]]
    [goog.dom :as gdom]
    [goog.object :as gobj]))

(def initial-state
  {:arrows {}
   :num-rows 8
   :num-cols 8
   :orientation "white"
   :position {}})

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
  (let [{:keys [board-width pieces-container-id piece-square-pct position]} @board-state
        html (atom "")]
    ;; FIXME: instantly remove any piece elements from the DOM
    ;; clear the :square->piece-id map
    (swap! board-state assoc :square->piece-id {})
    (doseq [[square piece] position]
      ;; TODO: can do this without an atom
      (let [piece-id (random-piece-id)]
        (swap! html str (html/Piece {:board-width board-width
                                     :id piece-id
                                     :piece piece
                                     :square square
                                     :width (/ board-width 8)
                                     :piece-square-pct piece-square-pct}))
        (swap! board-state assoc-in [:square->piece-id square] piece-id)))
    (append-html! pieces-container-id @html)))

(defn- draw-board!
  [{:keys [orientation]}])
  ;; (gobj/set root-el "innerHTML" (str "<h1>" orientation "</h1>")))

(defmulti do-animation!
  (fn [_board-state animation]
    (:type animation)))

(defmethod do-animation! "ANIMATION_ADD"
  [board-state {:keys [piece square]}]
  (let [{:keys [animate-speed-ms board-width pieces-container-id piece-square-pct position square-el-ids]} @board-state
        new-piece-id (random-piece-id)
        new-piece-html (html/Piece {:board-width board-width
                                    :id new-piece-id
                                    :hidden? true
                                    :piece piece
                                    :piece-square-pct piece-square-pct
                                    :square square
                                    :width (/ board-width 8)})]
    ;; add this piece to the DOM
    (append-html! pieces-container-id new-piece-html)
    ;; start opacity animation
    (js/setTimeout
      (fn []
        (set-style-prop! new-piece-id "opacity" "100%")
        (set-style-prop! new-piece-id "transition" (str "all " animate-speed-ms "ms")))
      1)
    ;; add the piece id to the board state
    (swap! board-state assoc-in [:square->piece-id square] new-piece-id)))

(defmethod do-animation! "ANIMATION_MOVE"
  [board-state {:keys [capture? destination piece source]}]
  (let [{:keys [animate-speed-ms board-width square->piece-id]} @board-state
        piece-id (get square->piece-id source)
        piece-el (gdom/getElement piece-id)]
    (if-not piece-el
      (do
        (js/console.warn "Unable to get piece element:" piece-id source)
        (js/console.log (pr-str square->piece-id)))
      (let [target-square-dimensions (square->dimensions destination board-width)]

        ; (set-style-prop! piece-el "transition" (str "all " animate-speed-ms "ms"))
        (set-style-prop! piece-el "left" (str (:left target-square-dimensions) "px"))
        (set-style-prop! piece-el "top" (str (:top target-square-dimensions) "px"))

        (when capture?
          (let [capture-piece-id (get square->piece-id destination)]
            (js/console.log "goodbye:" capture-piece-id)
            ;; TODO: do we want some very fast animation here?
            (js/setTimeout
              (fn []
                (remove-element! capture-piece-id))
              animate-speed-ms)))

        ;; update the square->piece mapping
        (swap! board-state update-in [:square->piece-id]
               (fn [sq->id]
                 (-> sq->id
                     (dissoc source)
                     (assoc destination piece-id))))))))

(defmethod do-animation! "ANIMATION_CLEAR"
  [board-state {:keys [piece square]}]
  (let [{:keys [animate-speed-ms board-width square->piece-id]} @board-state
        piece-id (get-in @board-state [:square->piece-id square])
        piece-el (gdom/getElement piece-id)]
    ;; remove the piece from the DOM once the animation finishes
    (.addEventListener piece-el "transitionend"
      (fn []
        (remove-element! piece-el)))

    ;; begin fade out animation
    (set-style-prop! piece-el "opacity" "0")
    (set-style-prop! piece-el "transition" (str "all " animate-speed-ms))

    ;; update square->piece mapping
    (swap! board-state update :square->piece-id dissoc square)))

(defmethod do-animation! :default
  [board-state animation]
  (js/console.warn "Unknown animation type:" animation))

; (defn do-animations!
;   [board-state animations]
;   (doseq [animation animations]
;     (do-animation! board-state animation)))

;; -----------------------------------------------------------------------------
;; API Methods

(defn orientation
  [board arg]
  (cond
    (= arg "white") (do (swap! board assoc :orientation "white")
                        (draw-board! @board)
                        "white")
    (= arg "black") (do (swap! board assoc :orientation "black")
                        (draw-board! @board)
                        "black")
    (= arg "flip") (do (swap! board update :orientation toggle-orientation)
                       (draw-board! @board)
                       (:orientation @board))
    :else (:orientation @board)))

(defn set-position-with-animations!
  [board-state new-pos]
  (let [animations (calculate-animations (:position @board-state) new-pos)]

    (js/console.log (pr-str animations))

    (doseq [anim animations]
      (do-animation! board-state anim))


    (js/console.log (pr-str (:square->piece-id @board-state)))

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
    (set-position-instant! board-state new-pos)))

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
      (do (js/console.warn "Invalid value passed to the position method:" (clj->js new-pos))
          nil))))

;; -----------------------------------------------------------------------------
;; Constructor

(defn init-dom!
  [{:keys [root-el orientation position] :as board}]
  (gobj/set root-el "innerHTML" (html/BoardContainer board)))

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

(defn constructor
  "Called to create a new Chessboard2 object."
  ([el-id]
   (constructor el-id {}))
  ([el js-opts]
   (let [root-el (dom-util/get-element el)
         ;; FIXME: fail if the DOM element does not exist
         root-width (dom-util/get-width root-el)
         opts1 (expand-second-arg js-opts)
         square-el-ids (create-square-el-ids (:num-rows initial-state)
                                             (:num-cols initial-state))
         opts2 (merge initial-state opts1)
         opts3 (assoc opts2 :root-el root-el
                            :animate-speed-ms 600
                            :board-height root-width
                            :board-width root-width
                            :piece-square-pct 0.9
                            :pieces-container-id (str (random-uuid))
                            :square->piece-id {}
                            :square-el-ids square-el-ids)
         ;; create an atom per instance to track the state of the board
         board-state (atom opts3)]
     (init-dom! @board-state)
     (add-events! root-el)
     (draw-position-instant! board-state)
     ;; return a JS object that implements the API
     (js-obj
       "clear" #(position board-state {} %1)
       "destroy" "FIXME"
       "fen" #(position board-state "fen" false)
       "flip" #(orientation board-state "flip")
       "move" #()
       "orientation" #(orientation board-state %1)
       "position" #(position board-state (js->clj %1) %2)
       "resize" #()
       "start" #(position board-state start-position %1)))))

;; TODO: support other module exports / formats here
(when (and js/window (not (fn? (gobj/get js/window "Chessboard2"))))
  (gobj/set js/window "Chessboard2" constructor))
