(ns test.chessboard2.util.data-transforms-test
  (:require
   [cljs.test :refer [deftest is]]
   [com.oakmac.chessboard2.util.data-transforms :refer [js-map->clj]]))

(deftest js-map->clj-test
  (let [empty-map (js/Map.)]
    (is (= (js-map->clj empty-map) {})))
  (let [map1 (js/Map.)]
    (.set map1 "aaa" "AAA")
    (.set map1 "bbb" "BBB")
    (is (= (js-map->clj map1) {"aaa" "AAA" "bbb" "BBB"})))
  (let [map2 (js/Map.)]
    (.set map2 "aaa" "AAA")
    (.set map2 "bbb" "BBB")
    (.set map2 "ccc" "CCC")
    (.delete map2 "bbb")
    (is (= (js-map->clj map2) {"aaa" "AAA" "ccc" "CCC"}))))
