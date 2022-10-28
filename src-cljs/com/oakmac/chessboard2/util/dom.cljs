(ns com.oakmac.chessboard2.util.dom
  (:require
    [com.oakmac.chessboard2.util.functions :refer [defer]]
    [goog.dom :as gdom]
    [goog.object :as gobj]))

(defn query-select [q]
  (.querySelector js/document q))

(defn query-select-all [q]
  (.querySelectorAll js/document q))

(defn safe-prevent-default
  "calls preventDefault() on a JS event if possible"
  [js-evt]
  (when (fn? (gobj/get js-evt "preventDefault"))
    (.preventDefault js-evt)))

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

(defn xy-inside-element?
  [el x y]
  (let [js-box (.getBoundingClientRect el)
        left (gobj/get js-box "left")
        width (gobj/get js-box "width")
        height (gobj/get js-box "height")
        top (gobj/get js-box "top")]
    (and (>= x left)
         (< x (+ left width))
         (>= y top)
         (< y (+ top height)))))

(defn get-height
  [el]
  (-> (.getBoundingClientRect el)
      (gobj/get "height")))

(defn get-width
  [el]
  (-> (.getBoundingClientRect el)
      (gobj/get "width")))

(defn set-style-prop!
  [el prop value]
  (-> (get-element el)
      (gobj/get "style")
      (gobj/set prop value)))

(defn set-inner-html!
  [el html]
  (-> (get-element el)
      (gobj/set "innerHTML" html)))

(defn append-html!
  [el additional-html]
  (-> (get-element el)
      (.insertAdjacentHTML "beforeend" additional-html)))

(defn add-class!
  [el classname]
  (-> (get-element el)
      (gobj/get "classList")
      (.add classname)))

(defn remove-class!
  [el classname]
  (-> (get-element el)
      (gobj/get "classList")
      (.remove classname)))

(defn remove-element!
  [el]
  (gdom/removeNode (get-element el)))

(defn fade-out-and-remove-el!
  "fades an element to zero opacity and removes it from the DOM
  optionally calls callback-fn when the animation is finished (if provided)"
  ([el-id animate-speed-ms]
   (fade-out-and-remove-el! el-id animate-speed-ms nil))
  ([el-id animate-speed-ms callback-fn]
   (when-let [el (get-element el-id)]
     ;; remove any existing transitions
     (set-style-prop! el "transition" "")

     ;; remove the piece from the DOM once the animation finishes
     (.addEventListener el "transitionend"
       (fn []
         (remove-element! el)
         (when (fn? callback-fn) (callback-fn))))

     ;; begin fade out animation on next stack
     (defer
       (fn []
        (set-style-prop! el "transition" (str "all " animate-speed-ms "ms"))
        (set-style-prop! el "opacity" "0"))))))

;; TODO: could write a Cypress test for this function
(defn el->path
  "returns a JS Array of the parent DOM nodes from el to stop-node (included)
  stop-node defaults to document.body if not provided"
  ([el]
   (el->path el js/document.body))
  ([el stop-node]
   (let [js-path (array)
         stop-node (gdom/getElement stop-node)
         append-parent-node-fn (fn [current-node]
                                 (when current-node
                                   ;; add this node to the path
                                   (.push js-path current-node)
                                   ;; recurse to parent node unless we are already there
                                   (when-not (= current-node stop-node)
                                     (recur (gdom/getParentElement current-node)))))]
     (append-parent-node-fn el)
     js-path)))
