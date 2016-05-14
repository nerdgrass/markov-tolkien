(ns markov-tolkien.generator
  (:gen-class))


(def example "And the Golden Grouse And the Pobble who")

(def words (clojure.string/split example #" "))
words

(def word-transitions (partition-all 3 1 words))
word-transitions

(defn word-chain [word-transitions]
  (reduce (fn [r t] (merge-with clojure.set/union r
                               (let [[a b c] t]
                                 {[a b] (if c #{c} #{})})))
          {}
          word-transitions))

(defn walk-chain [prefix chain result]
  (let [suffixes (get chain prefix)]
    (if (empty? suffixes)
      result
      (let [suffix (first (shuffle suffixes))
            new-prefix [(last prefix) suffix]]
        (recur new-prefix chain (conj result suffix))))))
