(ns test.chessboard2.js-api-test
  (:require
   [cljs.test :refer [deftest is]]
   [com.oakmac.chessboard2.js-api :refer [parse-move-args]]
   [com.oakmac.chessboard2.constants :refer [animate-speed-strings->times]]))

(deftest parse-move-args-test
  (is (= (parse-move-args ["e2-e4" "d2-d4"])
         [{:from "e2"
           :to "e4"}
          {:from "d2"
           :to "d4"}]))
  (is (= (parse-move-args ["e2-e4" false "d2-d4"])
         [{:animate? false
           :from "e2"
           :to "e4"}
          {:animate? false
           :from "d2"
           :to "d4"}]))
  (is (= (parse-move-args ["e2-e4" "d2-d4" 5000])
         [{:animate-speed-ms 5000
           :from "e2"
           :to "e4"}
          {:animate-speed-ms 5000
           :from "d2"
           :to "d4"}]))
  (let [callback-fn1 (fn [] nil)]
    (is (= (parse-move-args ["e2-e4" false callback-fn1])
           [{:animate? false
             :from "e2"
             :on-complete callback-fn1
             :to "e4"}])))
  (let [callback-fn1 (fn [] nil)]
    (is (= (parse-move-args ["e2-e4" "d2-d4" "superfast" callback-fn1])
           [{:animate-speed-ms (get animate-speed-strings->times "superfast")
             :from "e2"
             :on-complete callback-fn1
             :to "e4"}
            {:animate-speed-ms (get animate-speed-strings->times "superfast")
              :from "d2"
              :on-complete callback-fn1
              :to "d4"}]))))
