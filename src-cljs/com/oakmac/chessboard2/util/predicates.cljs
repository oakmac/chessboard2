(ns com.oakmac.chessboard2.util.predicates
  (:require
    [clojure.string :as str]
    [com.oakmac.chessboard2.util.data-transforms :refer [js-map->clj]]))

(defn js-map?
  [m]
  (instance? js/Map m))

(defn map-string?
  [s]
  (and (string? s)
       (= "map" (str/lower-case s))))

(defn fen-string?
  [s]
  (and (string? s)
       (= "fen" (str/lower-case s))))

(defn start-string?
  [s]
  (and (string? s)
       (= "start" (str/lower-case s))))

(defn valid-piece?
  [p]
  (and (string? p)
       (not= -1 (.search p #"^[bw][KQRNBP]$"))))

;; FIXME: this will not work for non 8x8 boards
;; need to make this function accept board size args
(defn valid-square?
  [s]
  (and (string? s)
       (not= -1 (.search s #"^[a-h][1-8]$"))))

(defn valid-position?
  [p]
  (and (map? p)
       (every? valid-square? (keys p))
       (every? valid-piece? (vals p))))

(defn valid-js-position-object?
  [js-pos]
  (and
    (object? js-pos)
    (let [clj-pos (js->clj js-pos)]
      (and (every? valid-square? (keys clj-pos))
           (every? valid-piece? (vals clj-pos))))))

(defn valid-js-position-map?
  [js-pos]
  (and
   (js-map? js-pos)
   (let [clj-pos (js-map->clj js-pos)]
     (and (every? valid-square? (keys clj-pos))
          (every? valid-piece? (vals clj-pos))))))

(defn valid-move-string?
  [m]
  (and (string? m)
    (let [mv-arr (.split m "-")]
      (and (= (count mv-arr) 2)
           (not= (aget mv-arr 0) (aget mv-arr 1))
           (valid-square? (aget mv-arr 0))
           (valid-square? (aget mv-arr 1))))))

;; FIXME: write me
(defn valid-color? [c]
  (and (string? c)))
