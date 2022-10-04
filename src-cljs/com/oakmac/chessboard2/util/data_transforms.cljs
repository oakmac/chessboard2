(ns com.oakmac.chessboard2.util.data-transforms)

(defn js-map->clj
  "Converts a JavaScript Map to a Clojure Map"
  [js-m]
  (let [clj-m (transient {})]
    (.forEach js-m
      (fn [js-val js-key _js-map]
        (assoc! clj-m js-key js-val)))
    (persistent! clj-m)))

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
