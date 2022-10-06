(ns com.oakmac.chessboard2.dom-ops
  "DOM operations that change the board"
  (:require
    [com.oakmac.chessboard2.feature-flags :as flags]
    [com.oakmac.chessboard2.util.dom :as dom-util :refer [append-html! remove-element!]]
    [com.oakmac.chessboard2.util.functions :refer [defer]]))

(defn valid-op? [op]
  (and
    (map? op)
    (when-let [f (:defer-fn op)]
      (fn? f))))

(defn apply-ops!
  "Apply DOM operations to the board"
  [board-state ops]
  (when flags/runtime-checks?
    (assert (every? valid-op? ops) "Invalid DOM ops passed to apply-ops!"))
  (let [{:keys [items-container-id]} @board-state]
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

    ;; piece fade-outs and captures
    (doseq [{:keys [duration-ms capture-piece-id fade-out-piece]} ops]
      (when fade-out-piece
        (dom-util/fade-out-and-remove-el! fade-out-piece duration-ms))
      (when capture-piece-id
        (dom-util/fade-out-and-remove-el! capture-piece-id duration-ms)))

    ;; update the board-state with new piece-ids
    (let [dissocs (map :delete-square->piece ops)
          updates (map :new-square->piece ops)]
      (swap! board-state update :square->piece-id
             (fn [m]
               (as-> m $
                (apply dissoc $ dissocs)
                (apply merge $ updates)))))))
