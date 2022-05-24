(ns com.oakmac.chessboard2.util.predicates
  (:require
    [clojure.string :as str]))

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

(defn valid-square?
  [s]
  (and (string? s)
       (not= -1 (.search s #"^[a-h][1-8]$"))))

(defn valid-position?
  [p]
  (and (map? p)
       (every? valid-square? (keys p))
       (every? valid-piece? (vals p))))

;; FIXME: valid-move?
