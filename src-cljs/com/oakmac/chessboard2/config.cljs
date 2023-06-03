(ns com.oakmac.chessboard2.config
  (:require
    [com.oakmac.chessboard2.util.predicates :refer [valid-position?]]
    [com.oakmac.chessboard2.util.string :refer [safe-lower-case]]))

(defn valid-drop-off-board?
  [s]
  (contains? #{"snapback" "remove"} s))

(defn valid-orientation?
  [o]
  (or (= o "white")
      (= o "black")))

(defn set-default-orientation
  [o]
  (if (= (safe-lower-case o) "black")
    "black"
    "white"))

(defn valid-js-coords? [c]
  (cond
    (= c "default") true
    ;; FIXME: validate the config here
    :else false))

(def config-props
  {:coordinates
   {:default-val nil
    :valid-fn valid-js-coords?}

   :draggable
   {:default-val false
    :valid-fn boolean?}

   :dropOffBoard
   {:default-val "snapback"
    :valid-fn valid-drop-off-board?}

   :mouseDraggable
   {:default-val false
    :valid-fn boolean?}

   :onChange
   {:valid-fn fn?}

   :onDragMove
   {:valid-fn fn?}

   :onDragStart
   {:valid-fn fn?}

   :onDrop
   {:valid-fn fn?}

   :onMousedownSquare
   {:valid-fn fn?}

   :onMouseupSquare
   {:valid-fn fn?}

   :onMouseenterSquare
   {:valid-fn fn?}

   :onMouseleaveSquare
   {:valid-fn fn?}

   :onSnapEnd
   {:valid-fn fn?}

   :onTouchSquare
   {:valid-fn fn?}

   :orientation
   {:default-val "white"
    :valid-fn valid-orientation?}

   :position
   {:default-val {}
    :valid-fn valid-position?}

   :touchDraggable
   {:default-val false
    :valid-fn boolean?}

   :touchMove
   {:default-val false
    :valid-fn boolean?}})

(def default-config
  "Create a default config map based on config-props"
  (reduce
   (fn [acc [config-key {:keys [default-val]}]]
     (assoc acc config-key default-val))
   {}
   config-props))

(def valid-config-keys
  (set (keys config-props)))

(def valid-config-strings
  (->> valid-config-keys
    (map name)
    set))

;; TODO: Good candidate for unit tests
(defn merge-config
  [their-config]
  (reduce
   (fn [new-config [config-key {:keys [default-val valid-fn]}]]
     (let [their-val (get their-config config-key)
           valid? (valid-fn their-val)]
       (if valid?
         (assoc new-config config-key their-val)
         (assoc new-config config-key default-val))))
   {}
   config-props))

(defn state->config
  "Given the board-state, return the public config object"
  [board-state]
  (select-keys board-state valid-config-keys))
