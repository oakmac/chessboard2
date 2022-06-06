(ns test.chessboard2.util.moves-test
  (:require
   [cljs.test :refer [deftest is]]
   [com.oakmac.chessboard2.util.moves :refer [apply-move-to-position]]))

(deftest apply-move-test
  (is (= (apply-move-to-position {"a1" "wR"} {:from "a1", :to "a2"})
         {"a2" "wR"}))
  (is (= (apply-move-to-position {"a1" "wR"} {:from "b1", :to "a1"})
         {"a1" "wR"}))
  (is (= (apply-move-to-position {"a1" "wR" "b1" "wR"} {:from "b1", :to "a1"})
         {"a1" "wR"}))
  (is (= (apply-move-to-position {"a1" "wR" "b1" "wR" "c1" "wB"} {:from "b1", :to "a1"})
         {"a1" "wR", "c1" "wB"})))
