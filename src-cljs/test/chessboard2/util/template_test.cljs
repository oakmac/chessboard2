(ns test.chessboard2.util.template-test
  (:require
   [cljs.test :refer [deftest is]]
   [com.oakmac.chessboard2.util.template :refer [template]]))

(deftest template-test
  (is (= (template "abc" {:a "x"}) "abc"))
  (is (= (template "{a}bc" {:p "q"}) "{a}bc"))
  (is (= (template "{a}bc" {:a "x"}) "xbc"))
  (is (= (template "{a}bc" {"a" "x"}) "xbc"))
  (is (= (template "{a}bc" {:a 2}) "2bc"))
  (is (= (template "{a}bc{a}bc" {:a "x"}) "xbcxbc"))
  (is (= (template "{a}{a}{b}" {:a "x" :b "y"}) "xxy")))
