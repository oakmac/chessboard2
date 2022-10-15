(ns com.oakmac.chessboard2.util.data-transforms
  (:require
    [goog.object :as gobj]))

(defn js-map->clj
  "Converts a JavaScript Map to a Clojure Map"
  [js-m]
  (let [tmp-obj (js-obj "z" (transient {}))]
    (.forEach js-m
      (fn [js-val js-key _js-map]
        (gobj/set tmp-obj "z" (assoc! (gobj/get tmp-obj "z") js-key js-val))))
    (persistent! (gobj/get tmp-obj "z"))))

(defn clj->js-map
  "Converts a Clojure Map to a JavaScript Map"
  [clj-map]
  (let [js-map (js/Map.)]
    (doseq [[k v] clj-map]
      (.set js-map (clj->js k) (clj->js v)))
    js-map))

(defn map->js-return-format
  "Returns a Clojure map of items as either a JS Array (default), JS Object, or JS Map"
  [items return-fmt]
  (case return-fmt
    "object" (clj->js items)
    "map" (clj->js-map items)
    (clj->js (vec (vals items)))))
