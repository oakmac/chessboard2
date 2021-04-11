(ns com.oakmac.chessboard2.core
  (:require
    [goog.dom :as gdom]
    [goog.object :as gobj]
    [com.oakmac.chessboard2.html :as html]
    [com.oakmac.chessboard2.util.fen :refer [fen->position position->fen]]))


(def ruy-lopez-fen "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R")


(def default-num-cols 8)
(def default-num-rows 8)
(def start-position-fen "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR")

(def ellipsis "…")

;; TODO: move to dom-util namespace?
(defn grab-element
  "does it's best to grab a native DOM element from it's argument
  arg can be either:
  1) already a DOM element
  2) id of an element (getElementById)
  3) querySelector

  return nil if not able to grab the element"
  [arg]
  (let [el1 (gdom/getElement arg)]
    (if el1
      el1
      (let [el2 (.querySelector js/document arg)]
        (if el2 el2 nil)))))

(def default-options
  (js-obj "position" "start"))

(def initial-state
  {:position {}
   :foo :bar})

(defn constructor
  "Called to create a new Chessboard2 object."
  ([el-id]
   (constructor el-id default-options))
  ([el js-opts]
   (let [root-el (grab-element el)
         board-state (atom initial-state)]
     (js/console.log (pr-str (fen->position "r7/8/8/8/8/8/8/R7")))
     (gobj/set root-el "innerHTML" (html/BoardContainer))
  ; (swap! some-state assoc "foo" "bar")
     (js-obj
       "clear" "FIXME: add"
       "destroy" "FIXME"
       "fen" "FIXME"
       "flip" "FIXME"
       "move" #()
       "orientation" #()
       "position" #()
       "resize" #()
       "start" #()))))

(when js/window
  (gobj/set js/window "Chessboard2" constructor))
