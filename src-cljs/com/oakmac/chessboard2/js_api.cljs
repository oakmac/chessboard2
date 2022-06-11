(ns com.oakmac.chessboard2.js-api
  "Functions that handle the JS API for Chessboard2"
  (:require
    [com.oakmac.chessboard2.util.data-transforms :refer [map->js-return-format]]
    [com.oakmac.chessboard2.util.fen :refer [fen->position position->fen valid-fen?]]
    [com.oakmac.chessboard2.util.moves :refer [apply-move-to-position move->map]]
    [com.oakmac.chessboard2.util.predicates :refer [fen-string? start-string? valid-color? valid-move-string? valid-square? valid-piece? valid-position?]]
    [com.oakmac.chessboard2.util.string :refer [lower-case-if-string safe-lower-case]]
    [com.oakmac.chessboard2.constants :refer [animate-speed-strings animate-speed-strings->times]]
    [goog.array :as garray]
    [goog.dom :as gdom]
    [goog.object :as gobj]))

(defn valid-animate-speed?
  [a]
  (or (and (int? a) (pos? a))
      (contains? animate-speed-strings a)))

(defn looks-like-a-move-object? [js-move]
  (and (object? js-move)
       (valid-square? (gobj/get js-move "from"))
       (valid-square? (gobj/get js-move "to"))))

(defn convert-animate-speed
  "Converts an animate-speed string to a ms time.
   If already a valid animate-speed-ms, returns unmodified."
   [s]
   (or
     (get animate-speed-strings->times (safe-lower-case s))
     (and (int? s) s)))

(defn convert-move
  "Converts m into a Move object if possible. Returns nil otherwise."
  [m]
  (cond
    (valid-move-string? m) (move->map m)
    (looks-like-a-move-object? m) (js->clj m)
    :else nil))

(defn parse-move-args
  "Parse variadic arguments to the .move() function into a vector of Move config maps"
  [args]
  (let [disable-animation? (some false? args)
        callback-fn (last (filter fn? args))
        lc-args (map lower-case-if-string args)
        animate-speed-ms (last (filter valid-animate-speed? lc-args))]
    (->> (map convert-move args)
         (remove nil?)
         (map (fn [m]
                (merge {}
                  (when disable-animation?
                    {:animate? false})
                  (when animate-speed-ms
                    {:animate-speed-ms (convert-animate-speed animate-speed-ms)})
                  (when callback-fn
                    {:on-complete callback-fn})
                  m))))))

;; FIXME: handle 0-0 and 0-0-0
(defn move-piece
  [board-state]
  (let [js-args (array)]
    (copy-arguments js-args)
    (.shift js-args)
    (js/console.log js-args)))

  ;   (let [current-pos (:position @board-state)
  ;         squares-to-remove (set (js->clj js-args))
  ;         ;; any argument of 'false' to this function means no animation
  ;         animate? (not-any? false? squares-to-remove)
  ;         new-position (apply dissoc current-pos squares-to-remove)]
  ;     (position board-state new-position animate?)))
  ;
  ;
  ; (cond
  ;   (valid-move? arg1) (move-piece board-state (move->map arg1 "MOVE_FORMAT"))
  ;   ;; TODO (array-of-moves? arg1) ()
  ;   (looks-like-a-move-object? arg1) (move-piece board-state (js->clj arg1 :keywordize-keys true))
  ;   :else (js/console.warn "FIXME ERROR CODE: Invalid value passed to the .move() method:" arg1)))
