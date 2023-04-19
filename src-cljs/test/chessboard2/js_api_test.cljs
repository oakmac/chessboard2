(ns test.chessboard2.js-api-test
  (:require
   [cljs.test :refer [deftest is]]
   [com.oakmac.chessboard2.constants :refer [animate-speed-strings->times start-position]]
   [com.oakmac.chessboard2.js-api :refer [parse-constructor-second-arg
                                          parse-move-args
                                          parse-set-coordinates-args
                                          parse-set-position-args]]
   [com.oakmac.chessboard2.util.fen :refer [fen->position]]))

(def ruy-lopez-fen "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R")

(deftest parse-constructor-second-arg-test
  (is (= (parse-constructor-second-arg nil)
         {}))
  (is (= (parse-constructor-second-arg "start")
         {:position start-position}))
  (is (= (parse-constructor-second-arg ruy-lopez-fen)
         {:position (fen->position ruy-lopez-fen)}))
  (is (= (parse-constructor-second-arg (js-obj))
         {:position {}}))
  (is (= (parse-constructor-second-arg (js-obj "e4" "wK"))
         {:position {"e4" "wK"}}))
  (is (= (parse-constructor-second-arg (js-obj "aa" "bb"))
         {})
      "Invalid positions are ignored")
  (is (= (parse-constructor-second-arg (js-obj "position" "start"))
         {:position start-position}))
  (is (= (parse-constructor-second-arg (js-obj "position" ruy-lopez-fen))
         {:position (fen->position ruy-lopez-fen)})))
  ;; TODO: more unit tests here
  ;; - test invalid config values

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

(deftest parse-set-position-args-test
  (is (= (parse-set-position-args (array))
         {}))
  (is (= (parse-set-position-args [false])
         {:animate false}))
  (is (= (parse-set-position-args ["fen-string-here" false])
         {:animate false}))
  (is (= (parse-set-position-args (array false "fen-string-here"))
         {:animate false}))
  (is (= (parse-set-position-args (array (js-obj "foo" "bar") false 2000))
         {:animate false
          :animateSpeed 2000}))
  (is (= (parse-set-position-args [2000 "fen-string-here"])
         {:animateSpeed 2000}))
  (is (= (parse-set-position-args [2000 "super slow" 678])
         {:animateSpeed 678})
      "last argument wins")
  (let [callback-fn1 (fn [] 1)]
    (is (= (parse-set-position-args ["fast" "fen-string-here" callback-fn1])
           {:animateSpeed "fast"
            :onComplete callback-fn1})))
  (let [callback-fn1 (fn [] 1)]
    (is (= (parse-set-position-args [(js-obj) callback-fn1])
           {:onComplete callback-fn1})))
  (is (= (parse-set-position-args [(js-obj "animateSpeed" 350)])
         {:animateSpeed 350}))
  (let [callback-fn1 (fn [] 1)]
    (is (= (parse-set-position-args [212 (js-obj "onComplete" callback-fn1)])
           {:animateSpeed 212, :onComplete callback-fn1}))))

;; TODO: make this work
; (deftest parse-set-coordinates-args-test
;   (is (= (parse-set-coordinates-args nil)
;          {}))
;   (is (= (parse-set-coordinates-args "")
;          {}))
;   (is (= (parse-set-coordinates-args "tl")
;          {:top  {:type "ranks"}
;           :left {:type "files"}}))
;   (is (= (parse-set-coordinates-args "trbl")
;          {:top    {:type "ranks"}
;           :right  {:type "files"}
;           :bottom {:type "ranks"}
;           :left   {:type "files"}}))
;   (is (= (parse-set-coordinates-args "tlaz")
;          {:top  {:type "ranks"}
;           :left {:type "files"}})
;       "invalid TRBL values are ignored")
;   (is (= (parse-set-coordinates-args (array))
;          {}))
;   (is (= (parse-set-coordinates-args (array "left" "bottom"))
;          {:left   {:type "files"}
;           :bottom {:type "ranks"}}))
;   (is (= (parse-set-coordinates-args (array "bottom" "left"))
;          {:left   {:type "files"}
;           :bottom {:type "ranks"}})
;       "order does not matter")
;   (is (= (parse-set-coordinates-args (array "left" "bottom" "foo"))
;          {:left   {:type "files"}
;           :bottom {:type "ranks"}})
;       "invalid TRBL values are ignored")
;   (is (= (parse-set-coordinates-args (js-obj "left"   (js-obj "style" "font-size: 13px; color: blue;")
;                                              "bottom" (js-obj "style" "text-transform: uppercase;")))
;          {:left   {:type "files"
;                    :style "font-size: 13px; color: blue;"}
;           :bottom {:type "ranks"
;                    :style "text-transform: uppercase;"}})))
