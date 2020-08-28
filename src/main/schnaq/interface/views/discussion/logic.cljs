(ns schnaq.interface.views.discussion.logic
  (:require [ghostwheel.core :refer [>defn]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]))


(>defn calculate-votes
  "Calculates the votes without needing to reload."
  [statement vote-type vote-store]
  [map? keyword? map? :ret number?]
  (let [[internal-key db-key] (if (= vote-type :upvotes)
                                [:meta/upvotes :up]
                                [:meta/downvotes :down])
        vote-change (get-in vote-store [db-key (:db/id statement)] 0)]
    (+ (internal-key statement) vote-change)))


(defn deduce-step
  "Deduces the current discussion-loop step by the available options."
  [options]
  (cond
    (some #{:starting-support/new} options) :starting-conclusions/select
    (some #{:undercut/new} options) :select-or-react
    :else :default))

(defn index-of
  "Returns the index of the first occurrence of `elem` in `coll` if its present and
  nil if not."
  [coll elem]
  (let [maybe-index (.indexOf coll elem)]
    (if (= maybe-index -1)
      nil
      maybe-index)))

(defn args-for-reaction
  "Returns the args for a certain reaction."
  [all-steps all-args reaction]
  (nth all-args (index-of all-steps reaction)))

(defn arg-type->attitude
  "Returns an attitude deduced from an argument-type."
  [arg-type]
  (cond
    (#{:argument.type/attack :argument.type/undercut} arg-type) "disagree"
    (#{:argument.type/support} arg-type) "agree"))

(defn submit-new-starting-premise
  "Takes arguments and a form input and calls the next step in the discussion."
  [current-args form]
  (let [new-text-element (oget form [:premise-text])
        new-text (oget new-text-element [:value])
        choice (oget form [:premise-choice :value])
        [reaction key-name] (if (= choice "against-radio")
                              [:starting-rebut/new :new/rebut-premise]
                              [:starting-support/new :new/support-premise])]
    (rf/dispatch [:continue-discussion reaction (assoc current-args key-name new-text)])
    (rf/dispatch [:form/should-clear [new-text-element]])))

(defn submit-new-premise
  "Submits a newly created premise as an undercut, rebut or support."
  [[support-args rebut-args undercut-args] form]
  (let [new-text-element (oget form [:premise-text])
        new-text (oget new-text-element [:value])
        choice (oget form [:premise-choice :value])]
    (case choice
      "against-radio" (rf/dispatch [:continue-discussion :rebut/new (assoc rebut-args :new/rebut new-text)])
      "for-radio" (rf/dispatch [:continue-discussion :support/new (assoc support-args :new/support new-text)])
      "undercut-radio" (rf/dispatch [:continue-discussion :undercut/new (assoc undercut-args :new/undercut new-text)]))
    (rf/dispatch [:form/should-clear [new-text-element]])))
