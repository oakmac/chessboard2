(ns com.oakmac.chessboard2.util.dom
  (:require
    [com.oakmac.chessboard2.util.functions :refer [defer]]
    [goog.dom :as gdom]
    [goog.object :as gobj]))

(defn query-select [q]
  (.querySelector js/document q))

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

(defn set-style-prop!
  [el prop value]
  (-> (get-element el)
      (gobj/get "style")
      (gobj/set prop value)))

(defn append-html!
  [el additional-html]
  (let [el (get-element el)
        current-html (gobj/get el "innerHTML")]
    (gobj/set el "innerHTML" (str current-html additional-html))))

(defn add-class!
  [el classname]
  (let [el (get-element el)]
    (.add (gobj/get el "classList") classname)))

(defn remove-class!
  [el classname]
  (let [el (get-element el)]
    (.remove (gobj/get el "classList") classname)))

(defn remove-element!
  [el]
  (gdom/removeNode (get-element el)))

(defn fade-out-and-remove-el!
  "fades an element to zero opacity and removes it from the DOM"
  [el-id animate-speed-ms]
  (when-let [el (get-element el-id)]
    ;; remove any existing transitions
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
