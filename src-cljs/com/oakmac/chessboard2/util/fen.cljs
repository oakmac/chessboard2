(ns com.oakmac.chessboard2.util.fen
  (:require
    [clojure.string :as str]))

(defn fen->piece-code
  "convert FEN piece code to bP, wK, etc"
  [piece]
  (if (= piece (str/lower-case piece))
    (str "b" (str/upper-case piece))
    (str "w" (str/upper-case piece))))

(def fen-pieces
  (set (.split "rnbqkpRNBQKP" "")))

(def file->alpha (vec (.split "abcdefgh" "")))

(defn- explode-fen-spaces
  "converts FEN empty space numbers to single characters
  makes parsing easier"
  [fen]
  (-> fen
    (str/replace "8" "11111111")
    (str/replace "7" "1111111")
    (str/replace "6" "111111")
    (str/replace "5" "11111")
    (str/replace "4" "1111")
    (str/replace "3" "111")
    (str/replace "2" "11")))

(defn simplify-fen-string
  "1) cut off any move, casting, etc info from the end of a FEN string (we are only interested in board position)
   2) replace all consecutive pawns with 1's"
  [f]
  (-> f
      (str/replace #" .+$" "")
      explode-fen-spaces))

;; TODO: candidate for optimization
(defn fen->position
  "converts a FEN string to a Position Map"
  [fen]
  (let [fen (simplify-fen-string fen)
        ranks (reverse (.split fen "/"))
        board-vec (map-indexed
                    (fn [rank-idx row-str]
                      (map-indexed
                        (fn [file-idx square-str]
                          {:rank-idx rank-idx
                           :file-idx file-idx
                           :fen-code square-str})
                        row-str))
                    ranks)
        every-square (flatten board-vec)]
    (reduce
      (fn [pos {:keys [file-idx rank-idx fen-code]}]
        (if (contains? fen-pieces fen-code)
          (let [alpha-square (str (nth file->alpha file-idx) (inc rank-idx))]
            (assoc pos alpha-square (fen->piece-code fen-code)))
          pos))
      {}
      every-square)))

(defn position->fen
  [p]
  "FIXME: write me :-)")

;; TODO: move to testing suite
(assert (= {} (fen->position "8/8/8/8/8/8/8/8")))
(assert (= {"a2" "wP", "b2" "bP"} (fen->position "8/8/8/8/8/8/Pp6/8")))
;; FIXME: need more tests here

(defn- fen-chunk? [f]
  (and (string? f)
       (= 8 (count f))
       (= -1 (.search f #"[^kqrnbpKQRNBP1]"))))

(defn valid-fen?
  [f]
  (if-not (string? f)
    false
    (let [f2 (simplify-fen-string f)
          fen-chunks (.split f2 "/")]
      (and (= 8 (count fen-chunks))
           (every? fen-chunk? fen-chunks)))))

(assert (valid-fen? "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR"))
(assert (valid-fen? "8/8/8/8/8/8/8/8"))
(assert (valid-fen? "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R"))
(assert (valid-fen? "3r3r/1p4pp/2nb1k2/pP3p2/8/PB2PN2/p4PPP/R4RK1 b - - 0 1"))
(assert (not (valid-fen? "3r3z/1p4pp/2nb1k2/pP3p2/8/PB2PN2/p4PPP/R4RK1 b - - 0 1")))
(assert (not (valid-fen? "anbqkbnr/8/8/8/8/8/PPPPPPPP/8")))
(assert (not (valid-fen? "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/")))
(assert (not (valid-fen? "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBN")))
(assert (not (valid-fen? "888888/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR")))
(assert (not (valid-fen? "888888/pppppppp/74/8/8/8/PPPPPPPP/RNBQKBNR")))
(assert (not (valid-fen? {})))
