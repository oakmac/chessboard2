(ns com.oakmac.chessboard2.core
  (:require
    [clojure.string :as str]
    [com.oakmac.chessboard2.html :as html]
    [com.oakmac.chessboard2.util.dom :as dom-util]
    [com.oakmac.chessboard2.util.fen :refer [fen->position position->fen valid-fen?]]
    [com.oakmac.chessboard2.util.pieces :refer [random-piece-id]]
    [com.oakmac.chessboard2.util.predicates :refer [fen-string? start-position? valid-position?]]
    [com.oakmac.chessboard2.util.squares :as squares-util]
    [goog.dom :as gdom]
    [goog.object :as gobj]))

(def default-num-cols 8)
(def default-num-rows 8)
(def start-position-fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR")
(def start-position (fen->position start-position-fen))

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
  (let [{:keys [board-width pieces-container-id piece-square-pct position square-el-ids]} @board-state
        pieces-el (gdom/getElement pieces-container-id)
        html (atom "")]
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
    ;; TODO: should append here instead of setting innerHTML (other items will be destroyed)
    (gobj/set pieces-el "innerHTML" @html)))

(defn- draw-board!
  [{:keys [orientation]}])
  ;; (gobj/set root-el "innerHTML" (str "<h1>" orientation "</h1>")))

(defn calculate-animations
  "return the animations that need to happen in order to get from posA to posB"
  [posA posB]
  ;; remove pieces that are the same in both positions
  ;; find all of the "move" animations
  ;; "add piece" animations
  ;; "clear" animations
  [{:type "move"
    :source "b1"
    :destination "c3"}
   {:type "move"
    :source "d2"
    :destination "d4"}])

(defn square->piece-el
  [])

(defn do-animations!
  [board-state animations]
  (doseq [animation animations]
    (let [piece-id (get-in @board-state [:square->piece-id (:source animation)])
          piece-el (gdom/getElement piece-id)]
      (when piece-el
        (gobj/set (gobj/get piece-el "style") "left" "100px")
        (gobj/set (gobj/get piece-el "style") "top" "200px")))))

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

(defn position
  "returns or sets the current board position"
  [board-state new-pos animate?]
  (cond
    ;; no first argument: return the position as a JS Object
    (not new-pos) (-> @board-state :position clj->js)
    ;; first argument is "fen", return position as a FEN string
    (fen-string? new-pos) (-> @board-state :position position->fen)
    ;; first argument is "start", set the starting position
    (start-position? new-pos) (position board-state start-position animate?)
    ;; new-pos is a fen string
    (valid-fen? new-pos) (position board-state (fen->position new-pos) animate?)
    ;; new-pos is a valid position
    (valid-position? new-pos)
    (if (false? animate?)
      ;; set position instantly
      (do
        (swap! board-state assoc :position new-pos)
        (draw-position-instant! board-state))
      ;; else do animations
      (let [animations (calculate-animations (:position @board-state) new-pos)]
        (do-animations! board-state animations)
        (swap! board-state assoc :position new-pos)))

    :else
    (do (js/console.warn "Invalid value passed to the position method:" (clj->js new-pos))
        nil)))

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
      (start-position? opts)
      {:position start-position}

      (valid-fen? opts)
      {:position (fen->position opts)}

      (valid-position? opts)
      {:position opts}

      (map? opts)
      (let [opts2 (select-keys opts valid-config-keys)
            their-pos (get opts2 "position")]
        (cond-> {}
          (start-position? their-pos) (assoc :position start-position)
          (valid-fen? their-pos)      (assoc :position (fen->position their-pos))
          (valid-position? their-pos) (assoc :position their-pos)))
          ;; FIXME: other configs values here

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
         square-el-ids (squares-util/create-square-el-ids (:num-rows initial-state)
                                                          (:num-cols initial-state))
         opts2 (merge initial-state opts1)
         opts3 (assoc opts2 :root-el root-el
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
     ;; return JS object that implements the API
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

;; TODO: support other module exports here
(when (and js/window (not (fn? (gobj/get js/window "Chessboard2"))))
  (gobj/set js/window "Chessboard2" constructor))
