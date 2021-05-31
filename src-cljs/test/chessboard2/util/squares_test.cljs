(ns test.chessboard2.util.squares-test
  (:require
   [cljs.test :refer [deftest is]]
   [com.oakmac.chessboard2.util.squares :refer [square->distance square->xy]]))

(deftest square->distance-test
  (is (= (square->distance "a1" "a1") 0))
  (is (= (square->distance "a1" "a2") 1))
  (is (= (square->distance "a1" "b2") 1))
  (is (= (square->distance "a1" "b3") 2))
  (is (= (square->distance "a1" "b4") 3)))

(deftest square->xy-test
  (is (= (square->xy "a8") {:x 0 :y 0}))
  (is (= (square->xy "a1") {:x 0 :y 7}))
  (is (= (square->xy "b7") {:x 1 :y 1})))
