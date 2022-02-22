(ns schnaq.test-data)

(def ^:private cat-or-dog-authors-and-users
  [{:db/id "user/wegi"
    :user/nickname "Wegi"}
   {:db/id "user/schredder"
    :user/nickname "Der Schredder"}
   {:db/id "user/rambo"
    :user/nickname "Christian"}])

(def ^:private cat-or-dog-statements
  [{:db/id "statement/get-dog"
    :statement/author "user/wegi"                           ; Use the tempid above
    :statement/content "we should get a dog"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/discussions ["discussion/cat-or-dog" "discussion/tapir-or-ameisenbaer"]}
   {:db/id "statement/get-cat"
    :statement/author "user/schredder"                      ; Use the tempid above
    :statement/content "we should get a cat"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/discussions ["discussion/cat-or-dog"]}
   {:db/id "statement/get-both"
    :statement/author "user/rambo"                          ; Use the tempid above
    :statement/content "we could get both, a dog and a cat"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/discussions ["discussion/cat-or-dog"]}
   {:db/id "statement/watchdogs"
    :statement/author "user/wegi"
    :statement/content "dogs can act as watchdogs"
    :statement/created-at #inst "2020-01-01"
    :statement/parent "statement/get-dog"
    :statement/type :statement.type/support
    :statement/version 1
    :statement/discussions ["discussion/cat-or-dog" "discussion/tapir-or-ameisenbaer"]}
   {:db/id "statement/walks"
    :statement/author "user/schredder"                      ; Use the tempid above
    :statement/content
    "you have to take the dog for a walk every day, which is tedious"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/get-dog"
    :statement/type :statement.type/attack
    :statement/discussions ["discussion/cat-or-dog" "discussion/tapir-or-ameisenbaer"]}
   {:db/id "statement/no-problem"
    :statement/author "user/rambo"
    :statement/content "it would be no problem"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/get-both"
    :statement/type :statement.type/support
    :statement/discussions ["discussion/cat-or-dog"]}
   {:db/id "statement/moneeey"
    :statement/author "user/wegi"
    :statement/content "we do not have enough money for two pets"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/no-problem"
    :statement/type :statement.type/attack
    :statement/discussions ["discussion/cat-or-dog"]}
   {:db/id "statement/independent"
    :statement/author "user/schredder"
    :statement/content "cats are very independent"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/get-cat"
    :statement/type :statement.type/support
    :statement/discussions ["discussion/cat-or-dog"]}
   {:db/id "statement/overbred"
    :statement/author "user/wegi"
    :statement/content "this is not true for overbred races"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/independent"
    :statement/type :statement.type/attack
    :statement/discussions ["discussion/cat-or-dog"]}
   {:db/id "statement/darwin-likes"
    :statement/author "user/schredder"
    :statement/content "this lies in their natural conditions"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/independent"
    :statement/type :statement.type/support
    :statement/discussions ["discussion/cat-or-dog"]}
   {:db/id "statement/ancestry"
    :statement/author "user/rambo"
    :statement/content "cats ancestors are animals in wildlife, who are hunting alone and not in groups"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/independent"
    :statement/type :statement.type/support
    :statement/discussions ["discussion/cat-or-dog"]}
   {:db/id "statement/taxes"
    :statement/author "user/schredder"
    :statement/content "a cat does not cost taxes like a dog does"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/get-cat"
    :statement/type :statement.type/support
    :statement/discussions ["discussion/cat-or-dog"]}
   {:db/id "statement/germoney"
    :statement/author "user/rambo"
    :statement/content "in germany a second dog costs even more taxes"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/taxes"
    :statement/type :statement.type/support
    :statement/discussions ["discussion/cat-or-dog"]}
   {:db/id "statement/doggo-same"
    :statement/author "user/rambo"
    :statement/content "other costs of living for cats and dogs are nearly the same"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/taxes"
    :statement/type :statement.type/support
    :statement/discussions ["discussion/cat-or-dog"]}
   {:db/id "statement/moody"
    :statement/author "user/wegi"
    :statement/content "cats are capricious"
    :statement/created-at #inst "2020-01-07"
    :statement/version 1
    :statement/parent "statement/get-cat"
    :statement/type :statement.type/attack
    :statement/discussions ["discussion/cat-or-dog"]}
   {:db/id "statement/catcatcatcat"
    :statement/author "user/schredder"
    :statement/content "the fact, that cats are capricious, is based on the cats race"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/moody"
    :statement/type :statement.type/attack
    :statement/discussions ["discussion/cat-or-dog"]}
   {:db/id "statement/not-all-cats"
    :statement/author "user/schredder"
    :statement/content "not every cat is capricious"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/moody"
    :statement/type :statement.type/attack
    :statement/discussions ["discussion/cat-or-dog"]}
   {:db/id "statement/fire-cats"
    :statement/author "user/rambo"
    :statement/content "several cats of my friends are real assholes"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/moody"
    :statement/type :statement.type/support
    :statement/discussions ["discussion/cat-or-dog"]}])

(def ^:private cat-or-dog-discussion
  [{:db/id "discussion/cat-or-dog"
    :discussion/title "Cat or Dog?"
    :discussion/description "Should a person looking for a pet rather buy a dog or a cat?"
    :discussion/states []
    :discussion/share-hash "cat-dog-hash"
    :discussion/edit-hash "cat-dog-edit-hash"
    :discussion/author "user/wegi"
    :discussion/created-at #inst "2019-01-01"
    :discussion/starting-statements ["statement/get-dog" "statement/get-both" "statement/get-cat"]}
   {:db/id "discussion/tapir-or-ameisenbaer"
    :discussion/title "Tapir oder Ameisenbär?"
    :discussion/created-at #inst "2019-01-01"
    :discussion/share-hash "ameisenbär-hash"
    :discussion/description "What do what do"
    :discussion/author "user/wegi"
    :discussion/states []
    :discussion/starting-statements ["statement/get-dog"]}])

(def ^:private graph-discussion
  [{:db/id "discussion/graph"
    :discussion/title "Wetter Graph"
    :discussion/created-at #inst "2019-01-01"
    :discussion/share-hash "graph-hash"
    :discussion/edit-hash "graph-edit-hash"
    :discussion/author "user/wegi"
    :discussion/description "Der Graph muss korrekt sein"
    :discussion/states []
    :discussion/starting-statements ["statement/warm" "statement/foo"]}
   {:db/id "statement/GrossFoo"
    :statement/author "user/rambo"
    :statement/content "Foo"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/warm"
    :statement/type :statement.type/support
    :statement/discussions ["discussion/graph"]}
   {:db/id "statement/warm"
    :statement/author "user/rambo"
    :statement/content "Es ist warm"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/discussions ["discussion/graph"]}
   {:db/id "statement/B"
    :statement/author "user/rambo"
    :statement/content "B"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/GrossFoo"
    :statement/type :statement.type/support
    :statement/discussions ["discussion/graph"]}
   {:db/id "statement/SonneScheint"
    :statement/author "user/rambo"
    :statement/content "Die Sonne scheint!"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/warm"
    :statement/type :statement.type/support
    :statement/discussions ["discussion/graph"]}
   {:db/id "statement/C"
    :statement/author "user/rambo"
    :statement/content "Die Sonne gibt Vitamin C"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/SonneScheint"
    :statement/type :statement.type/support
    :statement/discussions ["discussion/graph"]}
   {:db/id "statement/Bar"
    :statement/author "user/rambo"
    :statement/content "Bar"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/foo"
    :statement/type :statement.type/support
    :statement/discussions ["discussion/graph"]}
   {:db/id "statement/foo"
    :statement/author "user/rambo"
    :statement/content "foo"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/discussions ["discussion/graph"]}])

(def ^:private simple-discussion
  [{:db/id "discussion/simple"
    :discussion/title "Simple Discussion"
    :discussion/created-at #inst "2019-01-01"
    :discussion/share-hash "simple-hash"
    :discussion/author "user/wegi"
    :discussion/edit-hash "simple-hash-secret"
    :discussion/description "A very simple discussion"
    :discussion/states []
    :discussion/starting-statements ["statement/brainstorm"]}
   {:db/id "statement/denken"
    :statement/author "user/rambo"
    :statement/content "Man denkt viel nach dabei"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/brainstorm"
    :statement/type :statement.type/support
    :statement/discussions ["discussion/simple"]}
   {:db/id "statement/brainstorm"
    :statement/author "user/rambo"
    :statement/content "Brainstorming ist total wichtig"
    :statement/created-at #inst "2020-01-01"
    :statement/creation-secret "secret-creation-secret"
    :statement/version 1
    :statement/discussions ["discussion/simple"]}
   {:db/id "statement/denken-tut-weh"
    :statement/author "user/rambo"
    :statement/content "Denken sorgt nur für Kopfschmerzen. Lieber den Donaldo machen!"
    :statement/created-at #inst "2020-01-01"
    :statement/version 1
    :statement/parent "statement/denken"
    :statement/type :statement.type/attack
    :statement/discussions ["discussion/simple"]}])

(def deleted-discussions
  [{:discussion/title "Deleted discussion"
    :discussion/share-hash "public-share-hash-deleted"
    :discussion/edit-hash "secret-public-hash-deleted"
    :discussion/author "user/schredder"
    :discussion/states [:discussion.state/deleted]}])

(def alex
  {:db/id "user.registered/alex"
   :user.registered/keycloak-id "59456d4a-6950-47e8-88d8-a1a6a8de9276"
   :user.registered/display-name "A. Schneider"
   :user.registered/email "alexander@schneider.gg"
   :user.registered/last-name "Schneider"
   :user.registered/first-name "Alexander"
   :user.registered/groups ["test-group"]
   :user.registered/notification-mail-interval :notification-mail-interval/weekly
   :user.registered/visited-schnaqs
   [#:discussion{:share-hash "cat-dog-hash"}]})

(def kangaroo
  {:db/id "user.registered/kangaroo"
   :user.registered/keycloak-id "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
   :user.registered/display-name "kangaroo"
   :user.registered/email "k@ngar.oo"
   :user.registered/last-name "the"
   :user.registered/first-name "kangaroo"
   :user.registered/groups ["test-group"]
   :user.registered/notification-mail-interval :notification-mail-interval/daily
   :user.registered/visited-schnaqs
   [#:discussion{:share-hash "cat-dog-hash"}
    #:discussion{:share-hash "simple-hash"}]})

(def registered-users
  [alex kangaroo])

(def polls
  [{:db/id "poll/single-choice"
    :poll/title "Ganz allein"
    :poll/type :poll.type/single-choice
    :poll/options [{:db/id "option/milch"
                    :option/value "Milch"}
                   {:db/id "option/eis"
                    :option/value "Eis"}
                   {:db/id "option/wasser"
                    :option/value "Wasser"
                    :option/votes 4}]
    :poll/discussion "discussion/cat-or-dog"}
   {:db/id "poll/multiple-choice"
    :poll/title "Ganz allein mit mehreren"
    :poll/type :poll.type/multiple-choice
    :poll/options [{:db/id "option/milche"
                    :option/value "Milche"}
                   {:db/id "option/eise"
                    :option/value "Eise"
                    :option/votes 2}
                   {:db/id "option/wassers"
                    :option/value "Wassers"
                    :option/votes 1}]
    :poll/discussion "discussion/cat-or-dog"}
   {:db/id "poll/increment-test"
    :poll/title "Inkrementiere die Votes!"
    :poll/type :poll.type/single-choice
    :poll/options [{:db/id "option/mit-vote"
                    :option/value "Mit Vote"
                    :option/votes 1}
                   {:db/id "option/ohne-vote"
                    :option/value "Ohne Vote"}]
    :poll/discussion "discussion/simple"}])

(def schnaq-test-data
  (concat cat-or-dog-authors-and-users cat-or-dog-statements cat-or-dog-discussion
          deleted-discussions graph-discussion simple-discussion registered-users polls))
