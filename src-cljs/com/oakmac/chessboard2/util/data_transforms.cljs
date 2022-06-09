(ns com.oakmac.chessboard2.util.data-transforms)

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
