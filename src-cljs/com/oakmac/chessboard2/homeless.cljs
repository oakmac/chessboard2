(ns com.oakmac.chessboard2.homeless
  "FIXME: remove this namespace; figure out good names for things"
  (:require
    [com.oakmac.chessboard2.html :as html]
    [com.oakmac.chessboard2.util.dom :as dom-util :refer [add-class! append-html! remove-class! remove-element! set-style-prop!]]
    [com.oakmac.chessboard2.util.pieces :refer [random-piece-id]]
    [com.oakmac.chessboard2.util.squares :refer [create-square-el-ids square->dimensions]]))

    ; [com.oakmac.chessboard2.animations :refer [calculate-animations]]
    ; [com.oakmac.chessboard2.css :as css]
    ; [com.oakmac.chessboard2.dom-ops :as dom-ops]
    ; [com.oakmac.chessboard2.feature-flags :as flags]
    ; [com.oakmac.chessboard2.js-api :as js-api]
    ; [com.oakmac.chessboard2.util.board :refer [start-position]]
    ; [com.oakmac.chessboard2.util.data-transforms :refer [map->js-return-format]]
    ; [com.oakmac.chessboard2.util.fen :refer [fen->position position->fen valid-fen?]]
    ; [com.oakmac.chessboard2.util.functions :refer [defer]]
    ; [com.oakmac.chessboard2.util.ids :refer [random-id]]
    ; [com.oakmac.chessboard2.util.moves :refer [apply-move-to-position move->map]]
    ; [com.oakmac.chessboard2.util.predicates :refer [fen-string? start-string? valid-color? valid-move-string? valid-square? valid-piece? valid-position?]]
    ; [com.oakmac.chessboard2.util.string :refer [safe-lower-case]]
    ; [goog.array :as garray]
    ; [goog.dom :as gdom]
    ; [goog.object :as gobj]))

(defn animation->dom-op-add
  [{:keys [piece square] :as _animation} board-state]
  (let [{:keys [animate-speed-ms board-width orientation piece-square-pct]} @board-state
        new-piece-id (random-piece-id)
        new-piece-html (html/Piece {:board-width board-width
                                    :board-orientation orientation
                                    :id new-piece-id
                                    :hidden? true
                                    :piece piece
                                    :piece-square-pct piece-square-pct
                                    :square square
                                    :width (/ board-width 8)})]
    {:new-html new-piece-html
     :defer-fn (fn []
                 ;; start opacity animation after piece has been added to the DOM
                 (set-style-prop! new-piece-id "transition" (str "all " animate-speed-ms "ms"))
                 (set-style-prop! new-piece-id "opacity" "100%"))
     :new-square->piece (hash-map square new-piece-id)}))

;; TODO:
;; - Should we re-use the same DOM element here instead of destroying + creating a new one?
;; - Is it important for item-ids to persist?
(defn animation->dom-op-move
  [{:keys [capture? destination piece source] :as _animation} board-state]
  (let [{:keys [animate-speed-ms board-width orientation piece-square-pct square->piece-id]} @board-state
        current-piece-id (get square->piece-id source)
        new-piece-id (random-piece-id)
        new-piece-html (html/Piece {:board-width board-width
                                    :board-orientation orientation
                                    :id new-piece-id
                                    :hidden? false
                                    :piece piece
                                    :piece-square-pct piece-square-pct
                                    :square source
                                    :width (/ board-width 8)})
        target-square-dimensions (square->dimensions destination board-width orientation)]
    (merge
      {:new-html new-piece-html
       :defer-fn (fn []
                   ;; start move animation
                   (set-style-prop! new-piece-id "transition" (str "all " animate-speed-ms "ms"))
                   (set-style-prop! new-piece-id "left" (str (:left target-square-dimensions) "px"))
                   (set-style-prop! new-piece-id "top" (str (:top target-square-dimensions) "px")))
       :remove-el current-piece-id
       :new-square->piece (hash-map destination new-piece-id)
       :delete-square->piece source}
      (when capture?
        {:capture-piece-id (get square->piece-id destination)}))))

(defn animation->dom-op-clear
  [{:keys [square] :as _animation} board-state]
  (let [{:keys [square->piece-id]} @board-state
        piece-id (get square->piece-id square)]
    {:delete-square->piece square
     :fade-out-piece piece-id}))

;; NOTE: would normally use a defmethod here
;; the output file size is slightly reduced by not including defmethods in the project
;; -- C. Oakman, June 2022
(defn animation->dom-op
  [animation board-state]
  (case (:type animation)
    "ANIMATION_ADD" (animation->dom-op-add animation board-state)
    "ANIMATION_MOVE" (animation->dom-op-move animation board-state)
    "ANIMATION_CLEAR" (animation->dom-op-clear animation board-state)
    (js/console.warn "Unknown animation type:" (:type animation))))

; (defn set-position-with-animations!
;   [board-state new-pos]
;   (let [animations (calculate-animations (:position @board-state) new-pos)
;         dom-ops (map #(animation->dom-op % board-state) animations)]
;     (dom-ops/apply-ops! board-state dom-ops)
;     (swap! board-state assoc :position new-pos)))
