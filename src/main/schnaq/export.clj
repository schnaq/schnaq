(ns schnaq.export
  (:require [clojure.string :as string]
            [ghostwheel.core :refer [>defn >defn-]]
            [schnaq.database.discussion :as db]
            [schnaq.database.specs :as specs]))

(def ^:private punctuations
  "Check for these punctuations in a string."
  #{\. \? \! \: \;})

(>defn- add-punctuation-to-statement
  "Check if statement is punctuated. If not, add a full stop."
  [{:keys [statement/content] :as statement}]
  [::specs/statement :ret ::specs/statement]
  (if (punctuations (last content))
    statement
    (assoc statement :statement/content (str content "."))))

(>defn- indent-multi-paragraph-statement
  "Removes empty lines at the end of a statement and indents them correctly in
  the case of a 'valid' multi-line statement with paragraphs. Starting
  conclusions can't have an empty line, otherwise it would break the complete
  style of the export."
  [statement level]
  [string? number? :ret string?]
  (let [left-space (str "\n" (string/join (repeat (inc level) "  ")))
        splitted-and-trimmed (->> (string/split-lines statement)
                                  (map string/trim))]
    (if (zero? level)
      (->> splitted-and-trimmed (remove empty?) (string/join "\n"))
      (string/join left-space splitted-and-trimmed))))

(defn- next-line
  "Builds the next line of a node in text representation. Adds an empty line
  before a new starting-conclusion is detected."
  [old-text level statement]
  (let [relation-symbol (case (:statement/type statement)
                          :statement.type/attack "- "
                          :statement.type/support "+ "
                          :statement.type/neutral "o "
                          "")
        spacing (if (zero? level) "\n\n" "\n")
        ;; Indent multiline-text correctly. Additional level is to compensate for relation-symbol
        indented-label (indent-multi-paragraph-statement (:statement/content statement) level)
        next-line (str (string/join (repeat level "  ")) relation-symbol indented-label)]
    (str old-text spacing next-line)))

(>defn- nodes-after
  "Delivers all nodes which in the graph of the discussion come after `source-node`.
  Returns nodes as a list of tuples with the type of the link leading to the node
  being the first element.

  E.g. [:statement.type/attack {:db/id …}]"
  [source-node all-statements links]
  [:db/id sequential? sequential? :ret sequential?]
  (let [indexed-nodes (into {} (map #(vector (:id %) %) all-statements))]
    (map #(vector (:type %) (get indexed-nodes (:from %)))
         (filter #(= source-node (:to %)) links))))

(>defn generate-argdown
  "Generates a textual representation of the discussion in an argdown-format."
  [share-hash]
  [:discussion/share-hash :ret string?]
  (let [statements (db/all-statements share-hash)]
    (loop [queue (remove :statement/parent statements)
           text ""
           level 0]
      (if (empty? queue)
        ;; We're done here, give the finished text back
        text
        ;; Otherwise either toss the :level-down-marker, or do the recursive algo
        (if (= :level-down-marker (first queue))
          (recur (rest queue) text (dec level))
          (let [current-statement (first queue)
                updated-text (next-line text level current-statement)
                statements-to-add (filter #(= (get-in % [:statement/parent :db/id])
                                              (:db/id current-statement))
                                          statements)]
            (if (empty? statements-to-add)
              (recur (rest queue) updated-text level)
              (recur (concat statements-to-add [:level-down-marker] (rest queue)) updated-text (inc level)))))))))

(>defn generate-fulltext
  "Export a discussion as a fulltext."
  [share-hash]
  [:discussion/share-hash :ret string?]
  (let [statements (db/all-statements share-hash)
        punctuated-statements (map add-punctuation-to-statement statements)]
    (loop [queue (interleave (remove :statement/parent punctuated-statements)
                             (repeat :break-marker))
           text ""
           new-line? true]
      (if (empty? queue)
        ;; We're done here, give the finished text back
        (string/trim text)
        ;; Otherwise either toss the :break-marker, or do the recursive algo
        (if (= :break-marker (first queue))
          (recur (rest queue) (str text "\n") true)
          (let [current-statement (first queue)
                content (:statement/content current-statement)
                updated-text (format (if new-line? "%s%s" "%s %s")
                                     text content)
                statements-to-add (filter #(= (get-in % [:statement/parent :db/id])
                                              (:db/id current-statement))
                                          punctuated-statements)]
            (if (empty? statements-to-add)
              (recur (rest queue) updated-text false)
              (recur (concat statements-to-add (rest queue)) updated-text false))))))))
