(ns com.oakmac.chessboard2.util.lang)

;; TODO: candidate for removal
;; TODO: could use unit tests
(defn atom?
  [a]
  (satisfies? IAtom a))
