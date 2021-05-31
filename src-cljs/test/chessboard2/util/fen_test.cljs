(ns test.chessboard2.util.fen-test
  (:require
   [cljs.test :refer [deftest is]]
   [com.oakmac.chessboard2.util.fen :refer [fen->position valid-fen?]]))

(deftest fen->position-test
  (is (= {} (fen->position "8/8/8/8/8/8/8/8")))
  (is (= {"a2" "wP", "b2" "bP"} (fen->position "8/8/8/8/8/8/Pp6/8"))))
  ;; FIXME: need more tests here

(deftest valid-fen-test
  (is (true? (valid-fen? "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR")))
  (is (true? (valid-fen? "8/8/8/8/8/8/8/8")))
  (is (true? (valid-fen? "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R")))
  (is (true? (valid-fen? "3r3r/1p4pp/2nb1k2/pP3p2/8/PB2PN2/p4PPP/R4RK1 b - - 0 1")))
  (is (false? (valid-fen? "3r3z/1p4pp/2nb1k2/pP3p2/8/PB2PN2/p4PPP/R4RK1 b - - 0 1")))
  (is (false? (valid-fen? "anbqkbnr/8/8/8/8/8/PPPPPPPP/8")))
  (is (false? (valid-fen? "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/")))
  (is (false? (valid-fen? "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBN")))
  (is (false? (valid-fen? "888888/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR")))
  (is (false? (valid-fen? "888888/pppppppp/74/8/8/8/PPPPPPPP/RNBQKBNR")))
  (is (false? (valid-fen? {}))))
