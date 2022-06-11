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
    [com.oakmac.chessboard2.util.predicates :refer [fen-string? start-string? valid-color? valid-move-string? valid-square? valid-piece? valid-position?]]
    [com.oakmac.chessboard2.util.squares :refer [create-square-el-ids square->dimensions]]
    [com.oakmac.chessboard2.util.string :refer [safe-lower-case]]
    [goog.array :as garray]
    [goog.dom :as gdom]
    [goog.object :as gobj]))

(defn move-piece
  [board-state {:keys [animate?] :as move}]
  (let [new-position (apply-move-to-position (:position @board-state) move)]
    (position board-state new-position animate?)))

(defn array-of-moves? [arg]
  (and (array? arg)
       (every? arg valid-move-string?)))

(defn looks-like-a-move-object? [js-move]
  (and (object? js-move)
       (valid-square? (gobj/get js-move "from"))
       (valid-square? (gobj/get js-move "to"))))

;; FIXME: handle 0-0 and 0-0-0
(defn move-piece
  [board-state moves]
  (cond
    (valid-move-string? arg1) (move-piece board-state (move->map arg1 "MOVE_FORMAT"))
    ;; TODO (array-of-moves? arg1) ()
    (looks-like-a-move-object? arg1) (move-piece board-state (js->clj arg1 :keywordize-keys true))
    :else (js/console.warn "FIXME ERROR CODE: Invalid value passed to the .move() method:" arg1)))
