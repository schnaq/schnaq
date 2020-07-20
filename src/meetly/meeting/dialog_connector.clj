(ns meetly.meeting.dialog-connector
  (:import (java.util UUID)))

;; TODO this needs to be wired in, when the dialog.core is done.
(defn create-discussion-for-agenda
  "Creates a discussion for an agenda-point and returns some identifier for the agenda
  to save."
  [_title _description]
  (str (UUID/randomUUID)))

(defn start-discussion
  "Starts a new discussion (keyed by discussion-id)."
  [discussion-id username]
  ;; TODO fill this in when wiring dialog.core
  [[:starting-argument/select
    {:discussion/id discussion-id,
     :discussion/title "Cat or Dog?",
     :user/nickname username
     :present/arguments [{:db/id 17592186045479,
                          :argument/version 1,
                          :argument/author #:author{:nickname "Wegi"},
                          :argument/type :argument.type/support,
                          :argument/premises [{:db/id 17592186045480,
                                               :statement/content "dogs can act as watchdogs",
                                               :statement/version 1,
                                               :statement/author #:author{:nickname "Wegi"}}],
                          :argument/conclusion {:db/id 17592186045476,
                                                :statement/content "we should get a dog",
                                                :statement/version 1,
                                                :statement/author #:author{:nickname "Wegi"}}}
                         {:db/id 17592186045481,
                          :argument/version 1,
                          :argument/author #:author{:nickname "Der Schredder"},
                          :argument/type :argument.type/attack,
                          :argument/premises [{:db/id 17592186045482,
                                               :statement/content "you have to take the dog for a walk every day, which is tedious",
                                               :statement/version 1,
                                               :statement/author #:author{:nickname "Der Schredder"}}],
                          :argument/conclusion {:db/id 17592186045476,
                                                :statement/content "we should get a dog",
                                                :statement/version 1,
                                                :statement/author #:author{:nickname "Wegi"}}}
                         {:db/id 17592186045487,
                          :argument/version 1,
                          :argument/author #:author{:nickname "Christian"},
                          :argument/type :argument.type/support,
                          :argument/premises [{:db/id 17592186045488,
                                               :statement/content "it would be no problem",
                                               :statement/version 1,
                                               :statement/author #:author{:nickname "Christian"}}],
                          :argument/conclusion {:db/id 17592186045478,
                                                :statement/content "we could get both, a dog and a cat",
                                                :statement/version 1,
                                                :statement/author #:author{:nickname "Christian"}}}
                         {:db/id 17592186045491,
                          :argument/version 1,
                          :argument/author #:author{:nickname "Der miese Peter"},
                          :argument/type :argument.type/undercut,
                          :argument/premises [{:db/id 17592186045492,
                                               :statement/content "won't be best friends",
                                               :statement/version 1,
                                               :statement/author #:author{:nickname "Der miese Peter"}}
                                              {:db/id 17592186045493,
                                               :statement/content "a cat and a dog will generally not get along well",
                                               :statement/version 1,
                                               :statement/author #:author{:nickname "Der miese Peter"}}],
                          :argument/conclusion #:db{:id 17592186045487}}
                         {:db/id 17592186045494,
                          :argument/version 1,
                          :argument/author #:author{:nickname "Der Schredder"},
                          :argument/type :argument.type/support,
                          :argument/premises [{:db/id 17592186045495,
                                               :statement/content "cats are very independent",
                                               :statement/version 1,
                                               :statement/author #:author{:nickname "Der Schredder"}}],
                          :argument/conclusion {:db/id 17592186045477,
                                                :statement/content "we should get a cat",
                                                :statement/version 1,
                                                :statement/author #:author{:nickname "Der Schredder"}}}
                         {:db/id 17592186045506,
                          :argument/version 1,
                          :argument/author #:author{:nickname "Der Schredder"},
                          :argument/type :argument.type/support,
                          :argument/premises [{:db/id 17592186045507,
                                               :statement/content "a cat does not cost taxes like a dog does",
                                               :statement/version 1,
                                               :statement/author #:author{:nickname "Der Schredder"}}],
                          :argument/conclusion {:db/id 17592186045477,
                                                :statement/content "we should get a cat",
                                                :statement/version 1,
                                                :statement/author #:author{:nickname "Der Schredder"}}}
                         {:db/id 17592186045514,
                          :argument/version 1,
                          :argument/author #:author{:nickname "Wegi"},
                          :argument/type :argument.type/attack,
                          :argument/premises [{:db/id 17592186045515,
                                               :statement/content "cats are capricious",
                                               :statement/version 1,
                                               :statement/author #:author{:nickname "Wegi"}}],
                          :argument/conclusion {:db/id 17592186045477,
                                                :statement/content "we should get a cat",
                                                :statement/version 1,
                                                :statement/author #:author{:nickname "Der Schredder"}}}]}]
   [:starting-argument/new {:discussion/id discussion-id
                            :discussion/title "Cat or Dog?"
                            :user/nickname username}]])

(defn continue-discussion
  [[_reaction args]]
  [[:test args]])