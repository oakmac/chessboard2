(ns com.oakmac.chessboard2.util.fen
  (:require
    [clojure.string :as str]))

(defn fen->piece-code
  "convert FEN piece code to bP, wK, etc"
  [piece]
  (if (= piece (str/lower-case piece))
    (str "b" (str/upper-case piece))
    (str "w" (str/upper-case piece))))

(defn piece-code->fen
  "convert piece code to its FEN value"
  [piece]
  (let [chunks (str/split piece "")]
    (if (= (first chunks) "w")
      (str/upper-case (second chunks))
      (str/lower-case (second chunks)))))

(def fen-pieces
  (set (.split "rnbqkpRNBQKP" "")))

(def file->alpha (vec (.split "abcdefgh" "")))

(defn- explode-empty-squares
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

(defn- squeeze-empty-squares
  "converts sequential FEN empty space numbers to their shorter version"
  [fen]
  (-> fen
    (str/replace "11111111" "8")
    (str/replace "1111111" "7")
    (str/replace "111111" "6")
    (str/replace "11111" "5")
    (str/replace "1111" "4")
    (str/replace "111" "3")
    (str/replace "11" "2")))

(defn simplify-fen-string
  "1) cut off any move, casting, etc info from the end of a FEN string (we are only interested in board position)
   2) replace all consecutive pawns with 1's"
  [f]
  (-> f
      (str/replace #" .+$" "")
      explode-empty-squares))

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

;; TODO: candidate for optimization
(defn position->fen
  "Converts a position Map to a FEN string"
  [position]
  (let [alphas (range 0 8)
        cols (range 1 9)
        all-squares (for [c (reverse cols)
                          a (map file->alpha alphas)]
                      (str a c))
        long-hand-fen (reduce
                        (fn [fen square]
                          (str
                            fen
                            (if-let [piece (get position square)]
                              (piece-code->fen piece)
                              "1")
                            (when (and (str/includes? square "h")
                                       (not= square "h1"))
                              "/")))
                        ""
                        all-squares)]
    (squeeze-empty-squares long-hand-fen)))

(defn- fen-chunk? [f]
  (and (= 8 (count f))
       (= -1 (.search f #"[^kqrnbpKQRNBP1]"))))

(defn valid-fen?
  [f]
  (if-not (string? f)
    false
    (let [f2 (simplify-fen-string f)
          fen-chunks (.split f2 "/")]
      (and (= 8 (count fen-chunks))
           (every? fen-chunk? fen-chunks)))))
