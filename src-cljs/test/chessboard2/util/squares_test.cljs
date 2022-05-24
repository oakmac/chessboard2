(ns test.chessboard2.util.squares-test
  (:require
   [cljs.test :refer [deftest is]]
   [com.oakmac.chessboard2.util.squares :refer [square->distance square->xy squares->rect-dimensions]]))

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

(deftest squares->rect-dimensions-test
  (is (= (squares->rect-dimensions "a1" "a2" 800)
         {:height 200
          :width 100
          :left 0
          :top 600}))
  (is (= (squares->rect-dimensions "a1" "a2" 800)
         {:height 200
          :width 100
          :left 0
          :top 600}))
  (is (= (squares->rect-dimensions "a1" "b4" 800)
         {:height 400
          :width 200
          :left 0
          :top 400}))
  (is (= (squares->rect-dimensions "b4" "a1" 800)
         {:height 400
          :width 200
          :left 0
          :top 400}))
  (is (= (squares->rect-dimensions "d4" "h6" 800)
         {:height 300
          :width 500
          :left 300
          :top 200}))
  (is (= (squares->rect-dimensions "h6" "d4" 800)
         {:height 300
          :width 500
          :left 300
          :top 200})))
