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

(assert (valid-square? "a1"))
(assert (valid-square? "e2"))
(assert (not (valid-square? "D2")))
(assert (not (valid-square? "g9")))
(assert (not (valid-square? "a")))
(assert (not (valid-square? true)))
(assert (not (valid-square? nil)))
(assert (not (valid-square? {})))

(assert (valid-piece? "bP"))
(assert (valid-piece? "bK"))
(assert (valid-piece? "wK"))
(assert (valid-piece? "wR"))
(assert (not (valid-piece? "WR")))
(assert (not (valid-piece? "Wr")))
(assert (not (valid-piece? "a")))
(assert (not (valid-piece? true)))
(assert (not (valid-piece? nil)))
(assert (not (valid-piece? {})))
