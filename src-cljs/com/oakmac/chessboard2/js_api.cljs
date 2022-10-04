(ns com.oakmac.chessboard2.js-api
  "Functions that handle the JS API for Chessboard2"
  (:require
    [com.oakmac.chessboard2.api :as api]
    [com.oakmac.chessboard2.constants :refer [animate-speed-strings animate-speed-strings->times]]
    [com.oakmac.chessboard2.util.data-transforms :refer [clj->js-map map->js-return-format]]
    [com.oakmac.chessboard2.util.fen :refer [fen->position position->fen valid-fen?]]
    [com.oakmac.chessboard2.util.moves :refer [apply-move-to-position move->map]]
    [com.oakmac.chessboard2.util.predicates :refer [fen-string? map-string? start-string? valid-color? valid-move-string? valid-square? valid-piece? valid-position?]]
    [com.oakmac.chessboard2.util.string :refer [lower-case-if-string safe-lower-case]]
    [goog.array :as garray]
    [goog.dom :as gdom]
    [goog.object :as gobj]))

(def valid-move-keys
  #{:animate :animateSpeed :from :onComplete :to})

(defn valid-animate-speed-number?
  [n]
  (and (int? n) (pos? n)))

(defn valid-animate-speed?
  [n]
  (or (valid-animate-speed-number? n)
      (contains? animate-speed-strings n)))

(defn looks-like-a-move-js-object? [js-move]
  (and (object? js-move)
       (valid-square? (gobj/get js-move "from"))
       (valid-square? (gobj/get js-move "to"))))

(defn convert-animate-speed
  "Converts an animate speed string to a time in milliseconds.
   If already a valid animate-speed number, returns unmodified."
   [s]
   (or
     (get animate-speed-strings->times (safe-lower-case s))
     (and (valid-animate-speed-number? s) s)))

(defn convert-move
  "Converts m into a Move object if possible. Returns nil otherwise."
  [m]
  (cond
    (valid-move-string? m) (move->map m)
    (looks-like-a-move-js-object? m) (js->clj m :keywordize-keys true)
    :else nil))

(defn remove-extra-move-keys
  "Remove any extra keys on a Move map"
  [m]
  (select-keys m valid-move-keys))

;; TODO: warn them if they pass an invalid argument to .move()
; (js/console.warn "FIXME ERROR CODE: Invalid value passed to the .move() method:" arg1)
(defn parse-move-args
  "Parse variadic arguments to the .move() function into Move config maps
  Returns a Vector of Move config maps."
  [args]
  (let [;; any argument of 'false' to this function means no animation
        disable-animation? (some false? args)
        ;; take the last function as the onComplete callback
        callback-fn (last (filter fn? args))
        lc-args (map lower-case-if-string args)
        ;; take the last timing value as the animate-speed
        animate-speed (last (filter valid-animate-speed? lc-args))]
    (->> (map convert-move args)
         (remove nil?)
         ;; remove any extra keys that a user may have passed in
         (map remove-extra-move-keys)
         (map (fn [m]
                (merge {}
                  (when disable-animation?
                    {:animate false})
                  (when animate-speed
                    {:animateSpeed (convert-animate-speed animate-speed)})
                  (when callback-fn
                    {:onComplete callback-fn})
                  m))))))

;; FIXME: handle 0-0 and 0-0-0, user will have to specify white or black
(defn move-piece
  "Returns a single JS Promise if only one move is made.
  Otherwise returns an Array of Promises of the moves being made."
  [board-state]
  (let [js-args (array)]
    (copy-arguments js-args)
    (.shift js-args)
    (let [move-configs (parse-move-args js-args)
          moves (api/move-pieces board-state move-configs)]
      (if (= 1 (count moves))
        (clj->js (first moves))
        (clj->js moves)))))

(defn get-position
  "Returns the board position in various formats."
  [board-state format]
  (let [position (api/get-position board-state)]
    (cond
      (map-string? format) (clj->js-map position)
      (fen-string? format) (position->fen position)
      :else (clj->js position))))

(defn set-position
  "Sets the board position."
  []
  ;; FIXME: write this
  nil)

(defn position
  "Sets or returns the board position."
  [board-state]
  (let [js-args (array)]
    (copy-arguments js-args)
    (.shift js-args)
    (let [first-arg (aget js-args 0)
          args-len (count js-args)]
      (cond
        ;; no first argument: return the position as a JS Object
        (zero? args-len) (get-position board-state nil)
        ;; first argument is "fen": return position as a FEN string
        (fen-string? first-arg) (get-position board-state first-arg)
        ;; first argument is "map": return position as a JS Map
        (map-string? first-arg) (get-position board-state first-arg)

        ;; FIXME: set position methods here

        :else
        ;; FIXME: error code here
        (do (js/console.warn "Invalid value passed to .position()")
            nil)))))


        ; ;; first argument is "start": set the starting position
        ; (start-string? first-arg) (set-position! board-state start-position animate?)
        ; ;; new-pos is a fen string
        ; (valid-fen? new-pos) (set-position! board-state (fen->position new-pos) animate?)
        ; ;; new-pos is a valid position
        ; (valid-position? new-pos) (set-position! board-state new-pos animate?)
        ; ;; ¯\_(ツ)_/¯
        ; :else
        ; ;; FIXME: error code here
        ; (do (js/console.warn "Invalid value passed to the position method:" (clj->js new-pos))
        ;     nil)
        ;
        ;
        ; (map-string? first-arg) "return Map of Position"
        ; :else (js/console.warn "Invalid argument passed to .position()")))))

    ; (js/console.log "ddddddd:")
    ; (js/console.log js-args)))
