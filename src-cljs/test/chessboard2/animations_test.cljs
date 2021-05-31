(ns test.chessboard2.animations-test
  (:require
   [cljs.test :refer [deftest is]]
   [com.oakmac.chessboard2.animations :refer [calculate-animations find-closest-piece]]
   [com.oakmac.chessboard2.util.board :refer [start-position]]))

(deftest closest-piece-test
  (is (= (find-closest-piece start-position "wR" "c3") "a1"))
  (is (= (find-closest-piece start-position "wR" "f3") "h1"))
  (is (= (find-closest-piece {} "wR" "f3") nil)))

(def test1-posA {"a1" "wQ", "c3" "bP"})
(def test1-posB {"a2" "wQ", "c3" "bP"})
(def test1-anims
  [{:type "ANIMATION_MOVE"
    :source "a1"
    :destination "a2"
    :piece "wQ"
    :capture? false}])

(def test2-posA {})
(def test2-posB {"b2" "bP", "c3" "bP"})
(def test2-anims
  [{:type "ANIMATION_ADD" :square "b2" :piece "bP"}
   {:type "ANIMATION_ADD" :square "c3" :piece "bP"}])

(def test3-posA {"b2" "bP", "c3" "bP"})
(def test3-posB {})
(def test3-anims
  [{:type "ANIMATION_CLEAR" :square "b2" :piece "bP"}
   {:type "ANIMATION_CLEAR" :square "c3" :piece "bP"}])

(def test4-posA {"b2" "wP", "c3" "wP"})
(def test4-posB {"b2" "bP", "c3" "bP"})
(def test4-anims
  [{:type "ANIMATION_ADD" :square "b2" :piece "bP"}
   {:type "ANIMATION_ADD" :square "c3" :piece "bP"}
   {:type "ANIMATION_CLEAR" :square "b2" :piece "wP"}
   {:type "ANIMATION_CLEAR" :square "c3" :piece "wP"}])

(def test5-posA {"a1" "wP", "b2" "wP", "c3" "wP", "f6" "wQ"})
(def test5-posB {"a1" "wP", "b2" "bP", "c3" "bP", "h6" "wQ"})
(def test5-anims
  [{:type "ANIMATION_MOVE" :source "f6" :destination "h6" :piece "wQ" :capture? false}
   {:type "ANIMATION_ADD" :square "b2" :piece "bP"}
   {:type "ANIMATION_ADD" :square "c3" :piece "bP"}
   {:type "ANIMATION_CLEAR" :square "b2" :piece "wP"}
   {:type "ANIMATION_CLEAR" :square "c3" :piece "wP"}])

(def test6-posA {"a1" "wQ", "a2" "bR", "c3" "bP"})
(def test6-posB {"a2" "wQ", "c3" "bP"})
(def test6-anims
  [{:type "ANIMATION_MOVE"
    :source "a1"
    :destination "a2"
    :piece "wQ"
    :capture? true}])

(def test7-posA {"e8" "bR"})
(def test7-posB {"f6" "bR", "c3" "bR"})
(def test7-anims
  [{:type "ANIMATION_MOVE"
    :source "e8"
    :destination "c3"
    :piece "bR"
    :capture? false}
   {:type "ANIMATION_ADD"
    :square "f6"
    :piece "bR"}])

(deftest calculate-animations-test
  (is (= (calculate-animations test1-posA test1-posB) test1-anims))
  (is (= (calculate-animations test2-posA test2-posB) test2-anims))
  (is (= (calculate-animations test3-posA test3-posB) test3-anims))
  (is (= (calculate-animations test4-posA test4-posB) test4-anims))
  (is (= (calculate-animations test5-posA test5-posB) test5-anims))
  (is (= (calculate-animations test6-posA test6-posB) test6-anims))
  (is (= (calculate-animations test7-posA test7-posB) test7-anims)))
