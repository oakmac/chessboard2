(ns test.chessboard2.util.predicates-test
  (:require
   [cljs.test :refer [deftest is]]
   [com.oakmac.chessboard2.util.predicates :refer [valid-move? valid-piece? valid-square?]]))

(deftest valid-piece-test
  (is (true? (valid-piece? "bP")))
  (is (true? (valid-piece? "bK")))
  (is (true? (valid-piece? "wK")))
  (is (true? (valid-piece? "wR")))
  (is (false? (valid-piece? "WR")))
  (is (false? (valid-piece? "Wr")))
  (is (false? (valid-piece? "a")))
  (is (false? (valid-piece? true)))
  (is (false? (valid-piece? nil)))
  (is (false? (valid-piece? {}))))

(deftest valid-square-test
  (is (true? (valid-square? "a1")))
  (is (true? (valid-square? "e2")))
  (is (false? (valid-square? "D2")))
  (is (false? (valid-square? "g9")))
  (is (false? (valid-square? "a")))
  (is (false? (valid-square? true)))
  (is (false? (valid-square? nil)))
  (is (false? (valid-square? {}))))

;; FIXME: add tests for valid-position?

(deftest valid-move-test
  (is (true? (valid-move? "a1-a4")))
  (is (true? (valid-move? "e7-e8")))
  (is (false? (valid-move? "d2_d4")))
  (is (false? (valid-move? "d2-")))
  (is (false? (valid-move? "g9")))
  (is (false? (valid-move? "a")))
  (is (false? (valid-move? true)))
  (is (false? (valid-move? nil)))
  (is (false? (valid-move? {})))
  (is (false? (valid-move? "a1-a1"))))
