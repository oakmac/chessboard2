(ns com.oakmac.chessboard2.core
  (:require
    [clojure.string :as str]
    [com.oakmac.chessboard2.html :as html]
    [com.oakmac.chessboard2.util.dom :as dom-util]
    [com.oakmac.chessboard2.util.fen :refer [fen->position position->fen valid-fen?]]
    [com.oakmac.chessboard2.util.squares :as squares-util]
    [goog.object :as gobj]))

(def ruy-lopez-fen "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R")

(def default-num-cols 8)
(def default-num-rows 8)
(def start-position-fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR")
(def start-position (fen->position start-position-fen))

(def default-options
  {:position start-position})

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

(defn- draw-board!
  "FIXME: write this"
  [{:keys [arrows orientation position root-el]}]
  (gobj/set root-el "innerHTML" (str "<h1>" orientation "</h1>")))

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

(defn- expand-second-arg
  "expands shorthand versions of the second argument"
  [js-opts]
  (let [opts (js->clj js-opts)]
    (cond
      (and (string? opts) (= (str/lower-case opts) "start"))
      {:position start-position}

      (valid-fen? opts)
      {:position (fen->position opts)}

      ;; FIXME: allow passing a position object here
      ; (valid-position? opts)
      ; {:position opts}

      ;; FIXME: allow passing in a config object here

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
