(ns com.oakmac.chessboard2.dom-ops
  "DOM operations"
  (:require
    ; [com.oakmac.chessboard2.animations :refer [calculate-animations]]
    ; [com.oakmac.chessboard2.css :as css]
    ; [com.oakmac.chessboard2.feature-flags :as flags]
    ; [com.oakmac.chessboard2.html :as html]
    [com.oakmac.chessboard2.util.board :refer [start-position]]
    [com.oakmac.chessboard2.util.data-transforms :refer [map->js-return-format]]
    [com.oakmac.chessboard2.util.dom :as dom-util :refer [add-class! append-html! remove-class! remove-element! set-style-prop!]]))
    ; [com.oakmac.chessboard2.util.fen :refer [fen->position position->fen valid-fen?]]
    ; [com.oakmac.chessboard2.util.functions :refer [defer]]
    ; [com.oakmac.chessboard2.util.ids :refer [random-id]]
    ; [com.oakmac.chessboard2.util.moves :refer [apply-move-to-position]]
    ; [com.oakmac.chessboard2.util.pieces :refer [random-piece-id]]
    ; [com.oakmac.chessboard2.util.predicates :refer [fen-string? start-string? valid-color? valid-move-string? valid-square? valid-piece? valid-position?]]))

(defn execute-move!
  "Executes a move on the board. Returns a Promise."
  [{:keys [animate animateSpeed from onComplete to]}]
  (js/Promise.
    (fn [resolve-fn reject-fn])))
      ;; FIXME: write this
