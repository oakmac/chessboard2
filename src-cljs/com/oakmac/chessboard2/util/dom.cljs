(ns com.oakmac.chessboard2.util.dom
  (:require
    [goog.dom :as gdom]
    [goog.object :as gobj]))

(defn get-element
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

(defn get-dimensions
  "returns the {:height, :width} of a DOM element"
  [el]
  (let [js-box (.getBoundingClientRect el)]
    {:height (gobj/get js-box "height")
     :width (gobj/get js-box "width")}))

(defn get-width
  [el]
  (:width (get-dimensions el)))
