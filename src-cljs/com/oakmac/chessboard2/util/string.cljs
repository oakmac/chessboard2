(ns com.oakmac.chessboard2.util.string
  (:require
    [clojure.string :as str]))

(defn safe-lower-case
  "lower-case s if it is a String
  returns null if s is not a String"
  [s]
  (when (string? s)
    (str/lower-case s)))

(defn lower-case-if-string
  "lower-case s if it is a String, otherwise returns s unmodified"
  [s]
  (if (string? s)
    (str/lower-case s)
    s))
