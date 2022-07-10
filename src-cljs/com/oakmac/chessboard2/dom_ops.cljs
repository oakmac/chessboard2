(ns com.oakmac.chessboard2.dom-ops
  "DOM operations"
  (:require
    [com.oakmac.chessboard2.util.board :refer [start-position]]
    [com.oakmac.chessboard2.util.data-transforms :refer [map->js-return-format]]
    [com.oakmac.chessboard2.util.dom :as dom-util :refer [add-class! append-html! remove-class! remove-element! set-style-prop!]]
    [com.oakmac.chessboard2.util.squares :refer [idx->alpha square->dimensions squares->rect-dimensions]]))

(defn execute-move-with-animation!
  "Executes a move on the board using animation."
  [board-state position-info {:keys [animate animateSpeed from onComplete to]} resolve-fn]
  (let [{:keys [board-width orientation square->piece-id]} @board-state
        from-piece-id (get square->piece-id from)
        {:keys [left top]} (square->dimensions to board-width orientation)
        piece-el (dom-util/get-element from-piece-id)
        piece-code (get (:before-pos position-info) from)
        js-move-info (js-obj "afterPosition" (-> position-info :after-pos clj->js)
                             "beforePosition" (-> position-info :before-pos clj->js)
                             "duration" animateSpeed
                             "from" from
                             "piece" piece-code
                             "to" to)]
    ;; FIXME: move this to a runtime check?
    (if-not piece-el
      (js/console.warn "execute-move! error, could not find 'from' piece-id:" from-piece-id)
      (do
        (set-style-prop! piece-el "transition" (str "all " animateSpeed "ms"))
        (set-style-prop! piece-el "left" (str left "px"))
        (set-style-prop! piece-el "top" (str top "px"))
        ;; add a callback for the transitionend event
        (swap! board-state assoc-in [:animation-end-callbacks from-piece-id]
               (fn []
                 (when (fn? onComplete)
                   (onComplete js-move-info))
                 (resolve-fn js-move-info)))))))

(defn execute-move-instant!
  "Executes a move on the board instantly / synchronously."
  [board-state position-info {:keys [animate animateSpeed from onComplete to]}]
  (let [{:keys [board-width orientation square->piece-id]} @board-state
        from-piece-id (get square->piece-id from)
        {:keys [left top]} (square->dimensions to board-width orientation)
        piece-el (dom-util/get-element from-piece-id)]
    ;; FIXME: move this to a runtime check?
    (if-not piece-el
      (js/console.warn "execute-move-instant! error, could not find 'from' piece-id:" from-piece-id)
      (do
        (set-style-prop! piece-el "transition" "")
        (set-style-prop! piece-el "left" (str left "px"))
        (set-style-prop! piece-el "top" (str top "px"))))))

;; FIXME:
;; - instant animation return values
;; - does it make sense for instant animation to return a resolved Promise?
;;   - I think "yes", you should be able to combine instant and non-instant moves
;;   - need a test case for this scenario
;; - need to handle capturing DOM operations
(defn execute-move!
  "Executes a move on the board. Returns a Promise."
  [board-state position-info {:keys [animate animateSpeed from onComplete to] :as move}]
  (js/Promise.
    (fn [resolve-fn _reject-fn]
      (if animate
        (execute-move-with-animation! board-state position-info move resolve-fn)
        (do
          (execute-move-instant! board-state position-info move)
          (resolve-fn))))))
