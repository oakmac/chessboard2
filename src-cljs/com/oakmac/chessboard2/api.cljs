(ns com.oakmac.chessboard2.api
  "Functions that represent the CLJS API for Chessboard2"
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
    [com.oakmac.chessboard2.util.predicates :refer [fen-string? start-string? valid-color? valid-move-string? valid-square? valid-piece? valid-position?]]))

(defn default-move-cfg
  "Returns the default move config map."
  [board-state]
  (let [{:keys [animate-speed-ms]} @board-state]
    {:animate true
     :animateSpeed animate-speed-ms
     :onComplete nil}))

; (defn move-piece
;   [board-state {:keys [animate?] :as move}]
;   (let [new-position (apply-move-to-position (:position @board-state) move)]
;     (position board-state new-position animate?)))

(defn move-pieces
  "Executes a collection of Moves on the board. Modifies the position.
  Returns a collection of Promises."
  [board-state moves]
  (let [current-pos (:position @board-state)
        new-pos (reduce
                  (fn [pos move]
                    (apply-move-to-position pos move))
                  current-pos
                  moves)
        default-cfg (default-move-cfg board-state)
        moves2 (map
                 (fn [m]
                   (merge default-cfg m))
                 moves)]
    ; (js/console.log (pr-str new-pos))
    (js/console.log (pr-str moves2))))
