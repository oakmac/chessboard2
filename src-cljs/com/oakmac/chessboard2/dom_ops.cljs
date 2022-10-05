(ns com.oakmac.chessboard2.dom-ops
  "DOM operations that change the board"
  (:require
    [com.oakmac.chessboard2.util.board :refer [start-position]]
    [com.oakmac.chessboard2.util.data-transforms :refer [map->js-return-format]]
    [com.oakmac.chessboard2.util.dom :as dom-util :refer [add-class! append-html! remove-class! remove-element! set-style-prop!]]
    [com.oakmac.chessboard2.util.functions :refer [defer]]
    [com.oakmac.chessboard2.util.squares :refer [idx->alpha square->dimensions squares->rect-dimensions]]))

(defn execute-move-with-animation!
  "Executes a move on the board using animation."
  [board-state position-info {:keys [animate animateSpeed capture? from onComplete to]} resolve-fn reject-fn]
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
    (if-not piece-el
      (reject-fn (js-obj "msg" (str "No piece found on 'from' square " from)))
      (do

        ; (set-style-prop! piece-el "transition" (str "all " animateSpeed "ms"))
        ; (set-style-prop! piece-el "left" (str left "px"))
        ; (set-style-prop! piece-el "top" (str top "px"))


        ;; update square->piece-id information
        ; (swap! board-state update :square->piece-id
        ;        (fn [m]
        ;          (-> m
        ;            (dissoc from)
        ;            (assoc to from-piece-id))))

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
      (do))))



        ; (set-style-prop! piece-el "transition" "")
        ; (set-style-prop! piece-el "left" (str left "px"))
        ; (set-style-prop! piece-el "top" (str top "px"))



        ;; update square->piece-id information
        ; (swap! board-state update :square->piece-id
        ;        (fn [m]
        ;          (-> m
        ;            (dissoc from)
        ;            (assoc to from-piece-id))))))))

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
    (fn [resolve-fn reject-fn]
      (if animate
        (execute-move-with-animation! board-state position-info move resolve-fn reject-fn)
        (do
          (execute-move-instant! board-state position-info move)
          (resolve-fn))))))

(defn apply-ops!
  "Apply DOM operations to the board"
  [board-state ops]
  (let [{:keys [animate-speed-ms items-container-id]} @board-state]
    ;; remove elements
    (let [removes (map :remove-el ops)]
      (doseq [el-id removes]
        (when el-id
          (remove-element! el-id))))

    ;; append new HTML
    (let [new-html (->> (map :new-html ops)
                        (apply str))]
      (append-html! items-container-id new-html))

    ;; functions to run on the next stack
    (defer (fn []
             (doseq [{:keys [defer-fn]} ops]
               (when (fn? defer-fn) (defer-fn)))))

    ;; piece fade-outs
    (let [fade-outs (map :fade-out-piece ops)]
      (doseq [piece-id fade-outs]
        (when piece-id
          (dom-util/fade-out-and-remove-el! piece-id animate-speed-ms))))

    ;; captures
    (let [captures (map :capture-piece-id ops)]
      (doseq [el-id captures]
        (dom-util/fade-out-and-remove-el! el-id animate-speed-ms)))

    ;; update the board-state with new piece-ids
    (let [dissocs (map :delete-square->piece ops)
          updates (map :new-square->piece ops)]
      (swap! board-state update :square->piece-id
             (fn [m]
               (as-> m $
                (apply dissoc $ dissocs)
                (apply merge $ updates)))))))
