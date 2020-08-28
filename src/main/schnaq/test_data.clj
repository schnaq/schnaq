(ns schnaq.test-data)

(def ^:private meetings
  [{:db/id "meeting/expansion"
    :meeting/title "Wir wollen expandieren"
    :meeting/description "Woot Woot in da Hood"
    :meeting/start-date #inst "2019-10-01T01:01:01.000-00:00"
    :meeting/end-date #inst "2019-12-01T01:01:01.000-00:00"
    :meeting/share-hash "89eh32hoas-2983ud"
    :meeting/author "user/wegi"}
   {:db/id "meeting/graph"
    :meeting/title "graph-title"
    :meeting/description "graph-description"
    :meeting/start-date #inst "2019-10-01T01:01:01.000-00:00"
    :meeting/end-date #inst "2019-12-01T01:01:01.000-00:00"
    :meeting/share-hash "graph-hash"
    :meeting/author "user/mike"}])

(def ^:private agendas
  [{:db/id "agenda/first-agenda"
    :agenda/title "Top 1"
    :agenda/description "Top 2"
    :agenda/discussion "discussion/cat-or-dog"
    :agenda/meeting "meeting/expansion"}
   {:db/id "agenda/second-agenda"
    :agenda/title "Top 2"
    :agenda/description "Top 2.2"
    :agenda/discussion "discussion/tapir-or-ameisenbaer"
    :agenda/meeting "meeting/expansion"}
   {:db/id "agenda/graph-agenda"
    :agenda/title "Top 3"
    :agenda/description "Top 2.3"
    :agenda/discussion "discussion/graph"
    :agenda/meeting "meeting/graph"}])

(def ^:private cat-or-dog-authors-and-users
  [{:db/id "user/wegi"
    :user/core-author {:db/id "author/wegi" :author/nickname "Wegi"}}
   {:db/id "user/mike"
    :user/core-author {:db/id "author/mike" :author/nickname "Mike"}}
   {:db/id "user/schredder"
    :user/core-author {:db/id "author/schredder" :author/nickname "Der Schredder"}}
   {:db/id "user/rambo"
    :user/core-author {:db/id "author/rambo" :author/nickname "Christian"}}
   {:db/id "user/stinky"
    :user/core-author {:db/id "author/stinky" :author/nickname "Der miese Peter"}}])

(def ^:private cat-or-dog-statements
  [{:db/id "statement/get-dog"
    :statement/author "author/wegi"                         ; Use the tempid above
    :statement/content "we should get a dog"
    :statement/version 1}
   {:db/id "statement/get-cat"
    :statement/author "author/schredder"                    ; Use the tempid above
    :statement/content "we should get a cat"
    :statement/version 1}
   {:db/id "statement/get-both"
    :statement/author "author/rambo"                        ; Use the tempid above
    :statement/content "we could get both, a dog and a cat"
    :statement/version 1}])

(def ^:private cat-or-dog-arguments
  [{:db/id "argument/watchdogs"
    :argument/author "author/wegi"
    :argument/premises [{:db/id "statement/watchdogs"
                         :statement/author "author/wegi"
                         :statement/content "dogs can act as watchdogs"
                         :statement/version 1}]
    :argument/conclusion "statement/get-dog"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog" "discussion/tapir-or-ameisenbaer"]}
   {:db/id "argument/tedious-dogs"
    :argument/author "author/schredder"
    :argument/premises [{:db/id "statement/walks"
                         :statement/author "author/schredder" ; Use the tempid above
                         :statement/content
                         "you have to take the dog for a walk every day, which is tedious"
                         :statement/version 1}]
    :argument/conclusion "statement/get-dog"
    :argument/version 1
    :argument/type :argument.type/attack
    :argument/discussions ["discussion/cat-or-dog" "discussion/tapir-or-ameisenbaer"]}
   {:argument/author "author/stinky"
    :argument/premises [{:db/id "statement/no-use"
                         :statement/author "author/stinky"
                         :statement/content "we have no use for a watchdog"
                         :statement/version 1}]
    :argument/conclusion "argument/watchdogs"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "author/stinky"
    :argument/premises [{:db/id "statement/exercise"
                         :statement/author "author/stinky"
                         :statement/content
                         "going for a walk with the dog every day is good for social interaction and physical exercise"
                         :statement/version 1}]
    :argument/conclusion "argument/tedious-dogs"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog" "discussion/tapir-or-ameisenbaer"]}
   {:db/id "argument/both-is-fine"
    :argument/author "author/rambo"
    :argument/premises [{:db/id "statement/no-problem"
                         :statement/author "author/rambo"
                         :statement/content "it would be no problem"
                         :statement/version 1}]
    :argument/conclusion "statement/get-both"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "author/wegi"
    :argument/premises [{:db/id "statement/moneeey"
                         :statement/author "author/wegi"
                         :statement/content "we do not have enough money for two pets"
                         :statement/version 1}]
    :argument/conclusion "statement/no-problem"
    :argument/version 1
    :argument/type :argument.type/attack
    :argument/discussions ["discussion/cat-or-dog"]}
   ;; Here be premise groups
   {:db/id "argument/hate"
    :argument/author "author/stinky"
    :argument/premises [{:db/id "statement/best-friends"
                         :statement/author "author/stinky"
                         :statement/content "won't be best friends"
                         :statement/version 1}
                        {:db/id "statement/strong-hate"
                         :statement/author "author/stinky"
                         :statement/content
                         "a cat and a dog will generally not get along well"
                         :statement/version 1}]
    :argument/conclusion "argument/both-is-fine"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog"]}
   {:db/id "argument/independent-cats"
    :argument/author "author/schredder"
    :argument/premises [{:db/id "statement/independent"
                         :statement/author "author/schredder"
                         :statement/content "cats are very independent"
                         :statement/version 1}]
    :argument/conclusion "statement/get-cat"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "author/wegi"
    :argument/premises [{:db/id "statement/take-care-baby"
                         :statement/author "author/wegi"
                         :statement/content
                         "the purpose of a pet is to have something to take care of"
                         :statement/version 1}]
    :argument/conclusion "argument/independent-cats"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "author/wegi"
    :argument/premises [{:db/id "statement/overbred"
                         :statement/author "author/wegi"
                         :statement/content "this is not true for overbred races"
                         :statement/version 1}]
    :argument/conclusion "statement/independent"
    :argument/version 1
    :argument/type :argument.type/attack
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "author/schredder"
    :argument/premises [{:db/id "statement/darwin-likes"
                         :statement/author "author/schredder"
                         :statement/content "this lies in their natural conditions"
                         :statement/version 1}]
    :argument/conclusion "statement/independent"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   {:db/id "argument/hunters"
    :argument/author "author/rambo"
    :argument/premises [{:db/id "statement/ancestry"
                         :statement/author "author/rambo"
                         :statement/content
                         (str "cats ancestors are animals in wildlife, who are"
                              " hunting alone and not in groups")
                         :statement/version 1}]
    :argument/conclusion "statement/independent"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "author/wegi"
    :argument/premises [{:db/id "statement/wild-thang"
                         :statement/author "author/wegi"
                         :statement/content "house cats are not wild cats anymore"
                         :statement/version 1}]
    :argument/conclusion "argument/hunters"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog"]}
   {:db/id "argument/no-taxes"
    :argument/author "author/schredder"
    :argument/premises [{:db/id "statement/taxes"
                         :statement/author "author/schredder"
                         :statement/content "a cat does not cost taxes like a dog does"
                         :statement/version 1}]
    :argument/conclusion "statement/get-cat"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "author/stinky"
    :argument/premises [{:db/id "statement/credibility"
                         :statement/author "author/stinky"
                         :statement/content "thats what you just say without a credible source"
                         :statement/version 1}]
    :argument/conclusion "argument/no-taxes"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "author/rambo"
    :argument/premises [{:db/id "statement/germoney"
                         :statement/author "author/rambo"
                         :statement/content "in germany a second dog costs even
                         more taxes"
                         :statement/version 1}]
    :argument/conclusion "statement/taxes"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "author/rambo"
    :argument/premises [{:db/id "statement/doggo-same"
                         :statement/author "author/rambo"
                         :statement/content
                         "other costs of living for cats and dogs are nearly the same"
                         :statement/version 1}]
    :argument/conclusion "statement/taxes"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   {:db/id "argument/moody-cats"
    :argument/author "author/wegi"
    :argument/premises [{:db/id "statement/moody"
                         :statement/author "author/wegi"
                         :statement/content "cats are capricious"
                         :statement/version 1}]
    :argument/conclusion "statement/get-cat"
    :argument/version 1
    :argument/type :argument.type/attack
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "author/schredder"
    :argument/premises [{:db/id "statement/race-dogs"
                         :statement/author "author/schredder"
                         :statement/content
                         (str "this is based on the cats race and on the breeding"
                              ", and is not inherent for cats.")
                         :statement/version 1}]
    :argument/conclusion "argument/moody-cats"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "author/schredder"
    :argument/premises [{:db/id "statement/stinky-cats"
                         :statement/author "author/schredder"
                         :statement/content
                         "cats are only moody if youre stinky."
                         :statement/version 1}]
    :argument/conclusion "argument/moody-cats"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "author/schredder"
    :argument/premises [{:db/id "statement/catcatcatcat"
                         :statement/author "author/schredder"
                         :statement/content
                         (str "the fact, that cats are capricious, is based on the"
                              " cats race")
                         :statement/version 1}]
    :argument/conclusion "statement/moody"
    :argument/version 1
    :argument/type :argument.type/attack
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "author/schredder"
    :argument/premises [{:db/id "statement/not-all-cats"
                         :statement/author "author/schredder"
                         :statement/content "not every cat is capricious"
                         :statement/version 1}]
    :argument/conclusion "statement/moody"
    :argument/version 1
    :argument/type :argument.type/attack
    :argument/discussions ["discussion/cat-or-dog"]}
   {:db/id "argument/rambo-hates-cats"
    :argument/author "author/rambo"
    :argument/premises [{:db/id "statement/fire-cats"
                         :statement/author "author/rambo"
                         :statement/content (str "several cats of my friends are real"
                                                 " assholes")
                         :statement/version 1}]
    :argument/conclusion "statement/moody"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}])

(def ^:private cat-or-dog-discussion
  [{:db/id "discussion/cat-or-dog"
    :discussion/title "Cat or Dog?"
    :discussion/description "Should a person looking for a pet rather buy a dog or a cat?"
    :discussion/states [:discussion.state/open]
    :discussion/starting-arguments ["argument/watchdogs" "argument/tedious-dogs"
                                    "argument/both-is-fine" "argument/hate"
                                    "argument/independent-cats" "argument/no-taxes"
                                    "argument/moody-cats"]}
   {:db/id "discussion/tapir-or-ameisenbaer"
    :discussion/title "Tapir oder Ameisenbär?"
    :discussion/description "What do what do"
    :discussion/states [:discussion.state/open]
    :discussion/starting-arguments ["argument/tedious-dogs"]}])

(def ^:private graph-discussion
  [{:db/id "discussion/graph"
    :discussion/title "Wetter Graph"
    :discussion/description "Der Graph muss korrekt sein"
    :discussion/states [:discussion.state/open]
    :discussion/starting-arguments ["argument/warm" "argument/foo"]}
   {:db/id "argument/warm"
    :argument/author "author/rambo"
    :argument/premises [{:db/id "statement/GrossFoo"
                         :statement/author "author/rambo"
                         :statement/content "Foo"
                         :statement/version 1}]
    :argument/conclusion {:db/id "statement/warm"
                          :statement/author "author/rambo"
                          :statement/content "Es ist warm"
                          :statement/version 1}
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/graph"]}
   {:db/id "argument/B"
    :argument/author "author/schredder"
    :argument/premises [{:db/id "statement/B"
                         :statement/author "author/rambo"
                         :statement/content "B"
                         :statement/version 1}]
    :argument/conclusion "statement/GrossFoo"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/graph"]}
   {:db/id "argument/SonneScheint"
    :argument/author "author/schredder"
    :argument/premises [{:db/id "statement/SonneScheint"
                         :statement/author "author/rambo"
                         :statement/content "Die Sonne scheint!"
                         :statement/version 1}]
    :argument/conclusion "statement/warm"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/graph"]}
   {:db/id "argument/SonneScheintC"
    :argument/author "author/schredder"
    :argument/premises [{:db/id "statement/C"
                         :statement/author "author/rambo"
                         :statement/content "Die Sonne gibt Vitamin C"
                         :statement/version 1}]
    :argument/conclusion "statement/SonneScheint"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/graph"]}
   {:db/id "argument/foo"
    :argument/author "author/schredder"
    :argument/premises [{:db/id "statement/Bar"
                         :statement/author "author/rambo"
                         :statement/content "Bar"
                         :statement/version 1}]
    :argument/conclusion {:db/id "statement/foo"
                          :statement/author "author/rambo"
                          :statement/content "foo"
                          :statement/version 1}
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/graph"]}])

(def schnaq-test-data
  (concat cat-or-dog-authors-and-users cat-or-dog-statements cat-or-dog-arguments
          cat-or-dog-discussion meetings agendas
          graph-discussion))