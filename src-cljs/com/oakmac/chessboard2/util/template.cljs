(ns com.oakmac.chessboard2.util.template
  (:require
    [clojure.string :as str]))

(defn template [template-str variables-map]
  (reduce
    (fn [the-string [k v]]
      (let [template-variable (str "{" (name k) "}")]
        (str/replace the-string template-variable (str v))))
    template-str
    variables-map))
