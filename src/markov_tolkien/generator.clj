(ns markov-tolkien.generator
  (:gen-class))


(defn word-chain [word-transitions]
  (reduce (fn [r t] (merge-with clojure.set/union r
                               (let [[a b c] t]
                                 {[a b] (if c #{c} #{})})))
          {}
          word-transitions))

(defn text->word-chain [s]
  (let [words (clojure.string/split s #"[\s|\n]")
        word-transitions (partition-all 3 1 words)]
    (word-chain word-transitions)))

(defn chain->text [chain]
  (apply str (interpose " " chain)))

(defn walk-chain [prefix chain result]
  (let [suffixes (get chain prefix)]
    (if (empty? suffixes)
      result
      (let [suffix (first (shuffle suffixes))
            new-prefix [(last prefix) suffix]
            result-with-spaces (chain->text result)
            result-char-count (count result-with-spaces)
            suffix-char-count (inc (count suffix))
            new-result-char-count (+ result-char-count suffix-char-count)]
        (if (>= new-result-char-count 140)
          result
          (recur new-prefix chain (conj result suffix)))))))


(defn generate-text
  [start-phrase word-chain]
  (let [prefix (clojure.string/split start-phrase #" ")
        result-chain (walk-chain prefix word-chain prefix)
        result-text (chain->text result-chain)]
    result-text))

(defn process-file [fname]
  (text->word-chain
   (slurp (clojure.java.io/resource fname))))

(def files ["concerning-hobbits.txt" "monad.txt" "clojure.txt" "fp.txt" "elm.txt"
            "haskell.txt" "lambda.txt" "valaquenta.txt"])
(def functional-tolkien (apply merge-with clojure.set/union (map process-file files)))
(def prefix-list ["On the" "It is" "And all" "We think"
                  "For every" "No other" "To a" "And every"
                  "They do" "For his" "And the" "But the"
                  "Are the" "In the" "For the" "It was"
                  "In the" "For it" "With only" "Are the"
                  "Though the"  "And when"
                  "The last" "And this" "No other" "With a"
                  "And at" "What a" "Of the"
                  "They crossed" "So that" "And all" "When they"
                  "To the" "He is" "And nobody" "And it's"
                  "For any" "For example," "With the" "Haskell is"])

(generate-text "in the" functional-tolkien)
