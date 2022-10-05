(ns test.chessboard2.js-api-test
  (:require
   [cljs.test :refer [deftest is]]
   [com.oakmac.chessboard2.js-api :refer [parse-move-args parse-position-args]]
   [com.oakmac.chessboard2.constants :refer [animate-speed-strings->times]]))

(deftest parse-move-args-test
  (is (= (parse-move-args ["e2-e4"])
         [{:from "e2"
           :to "e4"}]))
  (is (= (parse-move-args ["e2-e4" "d2-d4"])
         [{:from "e2"
           :to "e4"}
          {:from "d2"
           :to "d4"}]))
  (is (= (parse-move-args ["e2-e4" false "d2-d4"])
         [{:animate false
           :from "e2"
           :to "e4"}
          {:animate false
           :from "d2"
           :to "d4"}]))
  (is (= (parse-move-args ["e2-e4" "d2-d4" 5000])
         [{:animateSpeed 5000
           :from "e2"
           :to "e4"}
          {:animateSpeed 5000
           :from "d2"
           :to "d4"}]))
  (let [callback-fn1 (fn [] nil)]
    (is (= (parse-move-args ["e2-e4" false callback-fn1])
           [{:animate false
             :from "e2"
             :onComplete callback-fn1
             :to "e4"}])))
  (let [callback-fn1 (fn [] nil)]
    (is (= (parse-move-args ["e2-e4" "d2-d4" "superfast" callback-fn1])
           [{:animateSpeed (get animate-speed-strings->times "superfast")
             :from "e2"
             :onComplete callback-fn1
             :to "e4"}
            {:animateSpeed (get animate-speed-strings->times "superfast")
             :from "d2"
             :onComplete callback-fn1
             :to "d4"}])))
  (let [callback-fn1 (fn [] 1)
        callback-fn2 (fn [] 2)]
    (is (= (parse-move-args [(js-obj "from" "e2"
                                     "foo" "bar"
                                     "to" "e4"
                                     "onComplete" callback-fn1)
                             "d2-d4"
                             callback-fn2])
           [{:from "e2"
             :onComplete callback-fn1
             :to "e4"}
            {:from "d2"
             :onComplete callback-fn2
             :to "d4"}]))))

(deftest parse-position-args-test
  (is (= (parse-position-args [false])
         {:animate false}))
  (is (= (parse-position-args ["fen-string-here" false])
         {:animate false}))
  (is (= (parse-position-args [false "fen-string-here"])
         {:animate false}))
  (is (= (parse-position-args [(js-obj) false 2000])
         {:animate false
          :animateSpeed 2000}))
  (is (= (parse-position-args [2000 "fen-string-here"])
         {:animateSpeed 2000}))
  (let [callback-fn1 (fn [] 1)]
    (is (= (parse-position-args [2000 "fen-string-here" callback-fn1])
           {:animateSpeed 2000
            :onComplete callback-fn1})))
  (let [callback-fn1 (fn [] 1)]
    (is (= (parse-position-args [(js-obj) callback-fn1])
           {:onComplete callback-fn1}))))
