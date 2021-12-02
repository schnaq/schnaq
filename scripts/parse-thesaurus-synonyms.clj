#!/usr/bin/env bb

(require '[clojure.string :as cstring])

(defn clean-synonym-line
  "Takes a synonym and removes paranthesis content.
  Also remove all synonyms with multiple tokens."
  [synonym-line]
  (->>
   (cstring/split synonym-line #";")
   (map #(cstring/split % #" "))
   (map (fn [synonym] (remove #(or (cstring/starts-with? % "(")
                                   (cstring/ends-with? % ")"))
                              synonym)))))

(let [raw-synonyms (slurp "synonyms_german.txt")
      lines (remove #(cstring/starts-with? %1 "#")
                    (cstring/split raw-synonyms #"\n"))
      cleaned-synonyms (map clean-synonym-line lines)
      mapped-synonyms (map (fn [synonym-list]
                             (when (= 1 (count (first synonym-list)))
                               {(cstring/lower-case (ffirst synonym-list))
                                (flatten (rest (remove #(> (count %) 1) synonym-list)))}))
                           cleaned-synonyms)
      without-empty-val (remove #(empty? (second (first %))) mapped-synonyms)]
  (spit "../resources/synonyms/synonyms_german.edn" (apply merge without-empty-val)))
