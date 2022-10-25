(ns com.oakmac.chessboard2.js-api
  "Functions that handle the JS API for Chessboard2"
  (:require
    [com.oakmac.chessboard2.api :as api]
    [com.oakmac.chessboard2.config :as config]
    [com.oakmac.chessboard2.constants :refer [animate-speed-strings animate-speed-strings->times start-position]]
    [com.oakmac.chessboard2.util.data-transforms :refer [clj->js-map js-map->clj]]
    [com.oakmac.chessboard2.util.fen :refer [fen->position position->fen valid-fen?]]
    [com.oakmac.chessboard2.util.logging :refer [warn-log]]
    [com.oakmac.chessboard2.util.moves :refer [move->map]]
    [com.oakmac.chessboard2.util.predicates :refer [fen-string?
                                                    map-string?
                                                    start-string?
                                                    valid-js-position-map?
                                                    valid-js-position-object?
                                                    valid-move-string?
                                                    valid-position?
                                                    valid-square?]]
    [com.oakmac.chessboard2.util.string :refer [lower-case-if-string safe-lower-case]]
    [goog.object :as gobj]))

;; TODO: good candidate for unit tests
(defn parse-constructor-second-arg
  "expands shorthand versions of the second argument to the Chessboard2 constructor"
  [js-opts]
  (let [opts-with-strings (js->clj js-opts)
        opts-with-keywords (js->clj js-opts :keywordize-keys true)]
    (cond
      (start-string? js-opts)
      {:position start-position}

      (valid-fen? js-opts)
      {:position (fen->position js-opts)}

      (valid-position? opts-with-strings)
      {:position opts-with-strings}

      (map? opts-with-strings)
      (let [;; remove any invalid config keys
            opts2 (select-keys opts-with-keywords config/valid-config-keys)
            their-pos (get opts-with-strings "position")]
        (cond-> opts2
          ;; set initial position
          (start-string? their-pos)   (assoc :position start-position)
          (valid-fen? their-pos)      (assoc :position (fen->position their-pos))
          (valid-position? their-pos) (assoc :position their-pos)))

      :else
      {})))

(def valid-move-keys
  #{:animate :animateSpeed :from :onComplete :to})

(def valid-set-position-keys
  #{:animate :animateSpeed :onComplete})

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

(defn remove-extra-position-keys
  "Remove any extra keys on a Set Position config"
  [m]
  (select-keys m valid-set-position-keys))

(defn convert-to-position-config
  "Converts arg to a Set Position config map if possible"
  [arg]
  (cond
    ;; any argument of 'false' means no animation
    (false? arg) {:animate false}
    ;; any function is an onComplete callback
    (fn? arg) {:onComplete arg}
    ;; any speed is animationSpeed
    (valid-animate-speed? arg) {:animateSpeed arg}
    ;; they can pass a JS object for these values directly
    (object? arg) (js->clj arg :keywordize-keys true)
    :else {}))

(defn parse-set-position-args
  "Parse variadic arguments to the setPosition methods into a valid config map.
  Returns the config map"
  [js-args]
  (or
    (->> js-args
      (map convert-to-position-config)
      (map remove-extra-position-keys)
      (apply merge))
    {}))

;; FIXME: handle 0-0 and 0-0-0, user will have to specify white or black
;; FIXME: the .move() method should support pieces or item-ids
;; .movePiece() --> only Pieces
;; .moveItem() --> only Items
;; .move() --> either
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
  [board-state]
  (let [js-args (array)]
    (copy-arguments js-args)
    (.shift js-args)
    (let [new-pos (aget js-args 0)
          opts (parse-set-position-args js-args)]
      (cond
        ;; first argument is "start": set the starting position
        (start-string? new-pos) (api/set-position! board-state start-position opts)
        ;; first argument is a FEN string: set the position
        (valid-fen? new-pos) (api/set-position! board-state (fen->position new-pos) opts)
        ;; first argument is a Position Object: set the position
        (valid-js-position-object? new-pos) (api/set-position! board-state (js->clj new-pos) opts)
        ;; first argument is a Position Map: set the position
        (valid-js-position-map? new-pos) (api/set-position! board-state (js-map->clj new-pos) opts)
        ;; ¯\_(ツ)_/¯
        :else (warn-log "Invalid position passed to setPosition():" new-pos)))))

(defn position
  "Sets or returns the board position."
  [board-state]
  (let [js-args (array)]
    (copy-arguments js-args)
    (.shift js-args)
    (let [arg1 (aget js-args 0)
          args-len (count js-args)
          opts (parse-set-position-args js-args)]
      (cond
        ;; no first argument: return the position as a JS Object
        (zero? args-len) (get-position board-state nil)
        ;; first argument is "fen": return position as a FEN string
        (fen-string? arg1) (get-position board-state arg1)
        ;; first argument is "map": return position as a JS Map
        (map-string? arg1) (get-position board-state arg1)
        ;; first argument is "start": set the starting position
        (start-string? arg1) (api/set-position! board-state start-position opts)
        ;; first argument is a FEN string: set the position
        (valid-fen? arg1) (api/set-position! board-state (fen->position arg1) opts)
        ;; first argument is a Position Object: set the position
        (valid-js-position-object? arg1) (api/set-position! board-state (js->clj arg1) opts)
        ;; first argument is a Position Map: set the position
        (valid-js-position-map? arg1) (api/set-position! board-state (js-map->clj arg1) opts)
        ;; ¯\_(ツ)_/¯
        :else (warn-log "Invalid value passed to position():" arg1)))))

(defn clear
  "Remove all of the pieces from the board. Accepts the same arguments / options as setPosition()
  Returns a Promise"
  [board-state]
  (let [js-args (array)]
    (copy-arguments js-args)
    (.shift js-args)
    (let [opts (parse-set-position-args js-args)]
      (api/set-position! board-state {} opts))))

(defn start
  "Sets the start position. Acceps the same arguments and options as setPosition()
  Returns a Promise"
  [board-state]
  (let [js-args (array)]
    (copy-arguments js-args)
    (.shift js-args)
    (let [opts (parse-set-position-args js-args)]
      (api/set-position! board-state start-position opts))))

(defn fen
  "Return or set the board position using a FEN String
  Returns a FEN String if returning the position
  Returns a Promise if setting the position"
  [board-state]
  (let [js-args (array)]
    (copy-arguments js-args)
    (.shift js-args)
    (let [arg1 (aget js-args 0)
          opts (parse-set-position-args js-args)]
      (cond
        (valid-fen? arg1) (api/set-position! board-state (fen->position arg1) opts)
        :else (get-position board-state "fen")))))

(defn add-item
  "Adds a custom item to the board."
  [board-state js-itm-cfg]
  ;; FIXME: do validation on js-itm-cfg here
  (api/add-item! board-state (js->clj js-itm-cfg :keywordize-keys true)))

;; FIXME: make this function variadic, support removing multiple item-ids at a time
(defn remove-item
  "Removes an Item from the board"
  [board-state item-id]
  (api/remove-item! board-state item-id))

(defn move-item
  "Moves an Item(s) on the board"
  [board-state js-cfg]
  ;; FIXME: parse variadic args smartly here
  (let [moves (api/move-items board-state [(js->clj js-cfg :keywordize-keys true)])]
    (if (= 1 (count moves))
      (clj->js (first moves))
      (clj->js moves))))
