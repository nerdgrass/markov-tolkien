(ns markov-tolkien.generator
  (:require [twitter.api.restful :as twitter]
            [twitter.oauth :as twitter-oauth]
            [environ.core :refer [env]]))


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
            "concerning-pipe-weed.txt" "haskell.txt" "lambda.txt" "music-of-ainur.txt" "valaquenta.txt"])
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

(defn end-at-last-punctuation [text]
  (let [trimmed-to-last-punct (apply str (re-seq #"[\s\w]+[^.!?,]*[.!?,]" text))
        trimmed-to-last-word (apply str (re-seq #".*[^a-zA-Z]+" text))
        result-text (if (empty? trimmed-to-last-punct)
                      trimmed-to-last-word
                      trimmed-to-last-punct)
        cleaned-text (clojure.string/replace result-text #"[,| ]$" ".")]
    (clojure.string/replace cleaned-text #"\"" "'")))

(defn tweet-text []
  (let [text (generate-text (-> prefix-list shuffle first) functional-tolkien)]
    (end-at-last-punctuation text)))

(def my-creds (twitter-oauth/make-oauth-creds (env :app-consumer-key)
                                              (env :app-consumer-secret)
                                              (env :user-access-token)
                                              (env :user-access-secret)))

(defn status-update []
  (let [tweet (tweet-text)]
    (println "generated tweet is :" tweet)
    (println "char count is:" (count tweet))
    (when (not-empty tweet)
      (try (twitter/statuses-update :oauth-creds my-creds
                                    :params {:status tweet})
           (catch Exception e (println "Oh no! " (.getMessage e)))))))
(status-update)
