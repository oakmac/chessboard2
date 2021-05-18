(ns com.oakmac.chessboard2.core
  (:require
    [clojure.string :as str]
    [com.oakmac.chessboard2.html :as html]
    [com.oakmac.chessboard2.util.dom :as dom-util]
    [com.oakmac.chessboard2.util.fen :refer [fen->position position->fen valid-fen?]]
    [com.oakmac.chessboard2.util.predicates :refer [valid-position?]]
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
  [{:keys [square-el-ids position]}]
  (doseq [[square piece] position]
    (let [square-id (get square-el-ids square)
          square-el (gdom/getElement square-id)]
      ;; FIXME: should append element here instead of innerHTML
      ;; maybe all pieces are children of the board element; positioned absolutely
      ;; absolute positioning should make animation easy
      (gobj/set square-el "innerHTML" (html/Piece {:piece piece})))))

(defn- draw-board!
  [{:keys [orientation]}])
  ;; (gobj/set root-el "innerHTML" (str "<h1>" orientation "</h1>")))

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
  [board new-pos animate?]
  (cond
    (not new-pos) (:position @board)
    (= (str/lower-case new-pos) "fen") (position->fen (:position @board))
    :else nil))

;; -----------------------------------------------------------------------------
;; Constructor

(defn init-dom!
  [{:keys [root-el orientation position] :as board}]
  (gobj/set root-el "innerHTML" (html/BoardContainer board)))

(defn start-position? [s]
  (and (string? s)
       (= "start" (str/lower-case s))))

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
                            :square-el-ids square-el-ids
                            :board-height root-width
                            :board-width root-width)
         ;; create an atom per instance to track the state of the board
         board-state (atom opts3)]
     (init-dom! @board-state)
     (add-events! root-el)
     (draw-position-instant! @board-state)
     ;; return JS object that implements the API
     (js-obj
       "clear" #(position board-state {} %1)
       "destroy" "FIXME"
       "fen" "FIXME"
       "flip" #(orientation board-state "flip")
       "move" #()
       "orientation" #(orientation board-state %1)
       "position" #()
       "resize" #()))))
       ; "start" #(position board-state start-position %1)))))

(when js/window
  (gobj/set js/window "Chessboard2" constructor))
