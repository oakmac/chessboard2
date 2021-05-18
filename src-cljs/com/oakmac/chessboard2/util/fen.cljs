(ns com.oakmac.chessboard2.util.fen
  (:require
    [clojure.string :as str]))

(defn valid-fen?
  [f]
  (and (string? f)
       ;; FIXME: write this for real
       true))

(defn fen->piece-code
  "convert FEN piece code to bP, wK, etc"
  [piece]
  (if (= piece (str/lower-case piece))
    (str "b" (str/upper-case piece))
    (str "w" (str/upper-case piece))))

(def fen-pieces
  (set (.split "rnbqkpRNBQKP" "")))

(def alpha-rows (vec (.split "hgfedcba" "")))

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

;; TODO: candidate for optimization
(defn fen->position
  "converts a FEN string to a Position Map"
  [fen]
  (let [fen (-> fen
              ;; cut off any move, casting, etc info from the end
              ;; we are only interested in position information
              (str/replace #" .+$" "")
              explode-fen-spaces)
        rows (.split fen "/")
        board-vec (map-indexed
                    (fn [row-idx row-str]
                      (map-indexed
                        (fn [col-idx square-str]
                          {:row-idx row-idx
                           :col-idx col-idx
                           :fen-code square-str})
                        row-str))
                    rows)
        every-square (flatten board-vec)]
    (reduce
      (fn [pos {:keys [col-idx row-idx fen-code]}]
        (if (contains? fen-pieces fen-code)
          (let [alpha-square (str (nth alpha-rows row-idx) (inc col-idx))]
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
