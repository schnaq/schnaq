(ns schnaq.test-data)
;; TODO restructure test-data during step 3 switchup
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
    :meeting/edit-hash "graph-edit-hash"
    :meeting/author "user/mike"}
   {:db/id "meeting/ameisenbär"
    :meeting/title "ameisenbär-title"
    :meeting/description "ameisenbär-description"
    :meeting/start-date #inst "2019-10-01T01:01:01.000-00:00"
    :meeting/end-date #inst "2019-12-01T01:01:01.000-00:00"
    :meeting/share-hash "ameisenbär-hash"
    :meeting/edit-hash "ameisenbär-edit-hash"
    :meeting/author "user/mike"}
   {:db/id "meeting/cat-dog-only"
    :meeting/title "cat-dog-title"
    :meeting/description "cat-dog-description"
    :meeting/start-date #inst "2019-10-01T01:01:01.000-00:00"
    :meeting/end-date #inst "2019-12-01T01:01:01.000-00:00"
    :meeting/share-hash "cat-dog-hash"
    :meeting/edit-hash "cat-dog-edit-hash"
    :meeting/author "user/wegi"}])

(def ^:private agendas
  [{:db/id "agenda/first-agenda"
    :agenda/title "Top 1"
    :agenda/description "Top 2"
    :agenda/discussion "discussion/cat-or-dog"
    :agenda/rank 1
    :agenda/meeting "meeting/expansion"}
   {:db/id "agenda/second-agenda"
    :agenda/title "Top 2"
    :agenda/description "Top 2.2"
    :agenda/discussion "discussion/tapir-or-ameisenbaer"
    :agenda/rank 2
    :agenda/meeting "meeting/expansion"}
   {:db/id "agenda/graph-agenda"
    :agenda/title "Top Graph"
    :agenda/description "Description Graphical"
    :agenda/discussion "discussion/graph"
    :agenda/rank 1
    :agenda/meeting "meeting/graph"}
   {:db/id "agenda/ameisenbär-agenda"
    :agenda/title "Top 2222"
    :agenda/description "Top 2.2222"
    :agenda/discussion "discussion/tapir-or-ameisenbaer"
    :agenda/rank 1
    :agenda/meeting "meeting/ameisenbär"}
   {:db/id "agenda/cat-dog-only"
    :agenda/title "Top 1"
    :agenda/description "Top 2"
    :agenda/discussion "discussion/cat-or-dog"
    :agenda/rank 1
    :agenda/meeting "meeting/cat-dog-only"}])

(def ^:private cat-or-dog-authors-and-users
  [{:db/id "user/wegi"
    :user/nickname "Wegi"}
   {:db/id "user/mike"
    :user/nickname "Mike"}
   {:db/id "user/schredder"
    :user/nickname "Der Schredder"}
   {:db/id "user/rambo"
    :user/nickname "Christian"}
   {:db/id "user/stinky"
    :user/nickname "Der miese Peter"}
   {:db/id "user/test-person"
    :user/nickname "Test-person"}])

(def ^:private cat-or-dog-statements
  [{:db/id "statement/get-dog"
    :statement/author "user/wegi"                         ; Use the tempid above
    :statement/content "we should get a dog"
    :statement/version 1}
   {:db/id "statement/get-cat"
    :statement/author "user/schredder"                    ; Use the tempid above
    :statement/content "we should get a cat"
    :statement/version 1}
   {:db/id "statement/get-both"
    :statement/author "user/rambo"                        ; Use the tempid above
    :statement/content "we could get both, a dog and a cat"
    :statement/version 1}])

(def ^:private cat-or-dog-arguments
  [{:db/id "argument/watchdogs"
    :argument/author "user/wegi"
    :argument/premises [{:db/id "statement/watchdogs"
                         :statement/author "user/wegi"
                         :statement/content "dogs can act as watchdogs"
                         :statement/version 1}]
    :argument/conclusion "statement/get-dog"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog" "discussion/tapir-or-ameisenbaer"]}
   {:db/id "argument/tedious-dogs"
    :argument/author "user/schredder"
    :argument/premises [{:db/id "statement/walks"
                         :statement/author "user/schredder" ; Use the tempid above
                         :statement/content
                         "you have to take the dog for a walk every day, which is tedious"
                         :statement/version 1}]
    :argument/conclusion "statement/get-dog"
    :argument/version 1
    :argument/type :argument.type/attack
    :argument/discussions ["discussion/cat-or-dog" "discussion/tapir-or-ameisenbaer"]}
   {:argument/author "user/stinky"
    :argument/premises [{:db/id "statement/no-use"
                         :statement/author "user/stinky"
                         :statement/content "we have no use for a watchdog"
                         :statement/version 1}]
    :argument/conclusion "argument/watchdogs"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "user/stinky"
    :argument/premises [{:db/id "statement/exercise"
                         :statement/author "user/stinky"
                         :statement/content
                         "going for a walk with the dog every day is good for social interaction and physical exercise"
                         :statement/version 1}]
    :argument/conclusion "argument/tedious-dogs"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog" "discussion/tapir-or-ameisenbaer"]}
   {:db/id "argument/both-is-fine"
    :argument/author "user/rambo"
    :argument/premises [{:db/id "statement/no-problem"
                         :statement/author "user/rambo"
                         :statement/content "it would be no problem"
                         :statement/version 1}]
    :argument/conclusion "statement/get-both"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "user/wegi"
    :argument/premises [{:db/id "statement/moneeey"
                         :statement/author "user/wegi"
                         :statement/content "we do not have enough money for two pets"
                         :statement/version 1}]
    :argument/conclusion "statement/no-problem"
    :argument/version 1
    :argument/type :argument.type/attack
    :argument/discussions ["discussion/cat-or-dog"]}
   ;; Here be premise groups
   {:db/id "argument/hate"
    :argument/author "user/stinky"
    :argument/premises [{:db/id "statement/best-friends"
                         :statement/author "user/stinky"
                         :statement/content "won't be best friends"
                         :statement/version 1}
                        {:db/id "statement/strong-hate"
                         :statement/author "user/stinky"
                         :statement/content
                         "a cat and a dog will generally not get along well"
                         :statement/version 1}]
    :argument/conclusion "argument/both-is-fine"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog"]}
   {:db/id "argument/independent-cats"
    :argument/author "user/schredder"
    :argument/premises [{:db/id "statement/independent"
                         :statement/author "user/schredder"
                         :statement/content "cats are very independent"
                         :statement/version 1}]
    :argument/conclusion "statement/get-cat"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "user/wegi"
    :argument/premises [{:db/id "statement/take-care-baby"
                         :statement/author "user/wegi"
                         :statement/content
                         "the purpose of a pet is to have something to take care of"
                         :statement/version 1}]
    :argument/conclusion "argument/independent-cats"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "user/wegi"
    :argument/premises [{:db/id "statement/overbred"
                         :statement/author "user/wegi"
                         :statement/content "this is not true for overbred races"
                         :statement/version 1}]
    :argument/conclusion "statement/independent"
    :argument/version 1
    :argument/type :argument.type/attack
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "user/schredder"
    :argument/premises [{:db/id "statement/darwin-likes"
                         :statement/author "user/schredder"
                         :statement/content "this lies in their natural conditions"
                         :statement/version 1}]
    :argument/conclusion "statement/independent"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   {:db/id "argument/hunters"
    :argument/author "user/rambo"
    :argument/premises [{:db/id "statement/ancestry"
                         :statement/author "user/rambo"
                         :statement/content
                         (str "cats ancestors are animals in wildlife, who are"
                              " hunting alone and not in groups")
                         :statement/version 1}]
    :argument/conclusion "statement/independent"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "user/wegi"
    :argument/premises [{:db/id "statement/wild-thang"
                         :statement/author "user/wegi"
                         :statement/content "house cats are not wild cats anymore"
                         :statement/version 1}]
    :argument/conclusion "argument/hunters"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog"]}
   {:db/id "argument/no-taxes"
    :argument/author "user/schredder"
    :argument/premises [{:db/id "statement/taxes"
                         :statement/author "user/schredder"
                         :statement/content "a cat does not cost taxes like a dog does"
                         :statement/version 1}]
    :argument/conclusion "statement/get-cat"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "user/stinky"
    :argument/premises [{:db/id "statement/credibility"
                         :statement/author "user/stinky"
                         :statement/content "thats what you just say without a credible source"
                         :statement/version 1}]
    :argument/conclusion "argument/no-taxes"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "user/rambo"
    :argument/premises [{:db/id "statement/germoney"
                         :statement/author "user/rambo"
                         :statement/content "in germany a second dog costs even
                         more taxes"
                         :statement/version 1}]
    :argument/conclusion "statement/taxes"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "user/rambo"
    :argument/premises [{:db/id "statement/doggo-same"
                         :statement/author "user/rambo"
                         :statement/content
                         "other costs of living for cats and dogs are nearly the same"
                         :statement/version 1}]
    :argument/conclusion "statement/taxes"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   {:db/id "argument/moody-cats"
    :argument/author "user/wegi"
    :argument/premises [{:db/id "statement/moody"
                         :statement/author "user/wegi"
                         :statement/content "cats are capricious"
                         :statement/version 1}]
    :argument/conclusion "statement/get-cat"
    :argument/version 1
    :argument/type :argument.type/attack
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "user/schredder"
    :argument/premises [{:db/id "statement/race-dogs"
                         :statement/author "user/schredder"
                         :statement/content
                         (str "this is based on the cats race and on the breeding"
                              ", and is not inherent for cats.")
                         :statement/version 1}]
    :argument/conclusion "argument/moody-cats"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "user/schredder"
    :argument/premises [{:db/id "statement/stinky-cats"
                         :statement/author "user/schredder"
                         :statement/content
                         "cats are only moody if youre stinky."
                         :statement/version 1}]
    :argument/conclusion "argument/moody-cats"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "user/schredder"
    :argument/premises [{:db/id "statement/catcatcatcat"
                         :statement/author "user/schredder"
                         :statement/content
                         (str "the fact, that cats are capricious, is based on the"
                              " cats race")
                         :statement/version 1}]
    :argument/conclusion "statement/moody"
    :argument/version 1
    :argument/type :argument.type/attack
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "user/schredder"
    :argument/premises [{:db/id "statement/not-all-cats"
                         :statement/author "user/schredder"
                         :statement/content "not every cat is capricious"
                         :statement/version 1}]
    :argument/conclusion "statement/moody"
    :argument/version 1
    :argument/type :argument.type/attack
    :argument/discussions ["discussion/cat-or-dog"]}
   {:db/id "argument/rambo-hates-cats"
    :argument/author "user/rambo"
    :argument/premises [{:db/id "statement/fire-cats"
                         :statement/author "user/rambo"
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
    :discussion/share-hash "cat-dog-hash"
    :discussion/author "user/wegi"
    :discussion/starting-statements ["statement/get-dog" "statement/get-both" "statement/get-cat"]}
   {:db/id "discussion/tapir-or-ameisenbaer"
    :discussion/title "Tapir oder Ameisenbär?"
    :discussion/share-hash "ameisenbär-hash"
    :discussion/description "What do what do"
    :discussion/states [:discussion.state/open]
    :discussion/starting-statements ["statement/get-dog"]}])

(def ^:private graph-discussion
  [{:db/id "discussion/graph"
    :discussion/title "Wetter Graph"
    :discussion/share-hash "graph-hash"
    :discussion/edit-hash "secreeeet"
    :discussion/author "user/wegi"
    :discussion/description "Der Graph muss korrekt sein"
    :discussion/states [:discussion.state/open]
    :discussion/starting-statements ["statement/warm" "statement/foo"]}
   {:db/id "argument/warm"
    :argument/author "user/rambo"
    :argument/premises [{:db/id "statement/GrossFoo"
                         :statement/author "user/rambo"
                         :statement/content "Foo"
                         :statement/version 1}]
    :argument/conclusion {:db/id "statement/warm"
                          :statement/author "user/rambo"
                          :statement/content "Es ist warm"
                          :statement/version 1}
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/graph"]}
   {:db/id "argument/B"
    :argument/author "user/schredder"
    :argument/premises [{:db/id "statement/B"
                         :statement/author "user/rambo"
                         :statement/content "B"
                         :statement/version 1}]
    :argument/conclusion "statement/GrossFoo"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/graph"]}
   {:db/id "argument/SonneScheint"
    :argument/author "user/schredder"
    :argument/premises [{:db/id "statement/SonneScheint"
                         :statement/author "user/rambo"
                         :statement/content "Die Sonne scheint!"
                         :statement/version 1}]
    :argument/conclusion "statement/warm"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/graph"]}
   {:db/id "argument/SonneScheintC"
    :argument/author "user/schredder"
    :argument/premises [{:db/id "statement/C"
                         :statement/author "user/rambo"
                         :statement/content "Die Sonne gibt Vitamin C"
                         :statement/version 1}]
    :argument/conclusion "statement/SonneScheint"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/graph"]}
   {:db/id "argument/foo"
    :argument/author "user/schredder"
    :argument/premises [{:db/id "statement/Bar"
                         :statement/author "user/rambo"
                         :statement/content "Bar"
                         :statement/version 1}]
    :argument/conclusion {:db/id "statement/foo"
                          :statement/author "user/rambo"
                          :statement/content "foo"
                          :statement/version 1}
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/graph"]}])

(def ^:private simple-discussion
  [{:db/id "meeting/simple"
    :meeting/title "Wir wollen simple Meetings"
    :meeting/description "Das ist ein simples meeting"
    :meeting/start-date #inst "2019-10-01T01:01:01.000-00:00"
    :meeting/end-date #inst "2019-12-01T01:01:01.000-00:00"
    :meeting/share-hash "simple-hash"
    :meeting/author "user/wegi"}
   {:db/id "agenda/simple-agenda"
    :agenda/title "Simple top"
    :agenda/description "Simple top top"
    :agenda/discussion "discussion/simple"
    :agenda/rank 1
    :agenda/meeting "meeting/simple"}
   {:db/id "discussion/simple"
    :discussion/title "Simple Discussion"
    :discussion/share-hash "simple-hash"
    :discussion/edit-hash "simple-hash-secret"
    :discussion/description "A very simple discussion"
    :discussion/states [:discussion.state/open]
    :discussion/starting-statements ["statement/brainstorm"]}
   {:db/id "argument/simple-start"
    :argument/author "user/rambo"
    :argument/premises [{:db/id "statement/denken"
                         :statement/author "user/rambo"
                         :statement/content "Man denkt viel nach dabei"
                         :statement/version 1}]
    :argument/conclusion {:db/id "statement/brainstorm"
                          :statement/author "user/rambo"
                          :statement/content "Brainstorming ist total wichtig"
                          :statement/version 1}
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/simple"]}
   {:db/id "argument/denken-nix-brainstorm"
    :argument/author "user/schredder"
    :argument/premises [{:db/id "statement/denken-nix-brainstorm"
                         :statement/author "user/rambo"
                         :statement/content "Brainstorm hat nichts mit aktiv denken zu tun"
                         :statement/version 1}]
    :argument/conclusion "argument/simple-start"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/simple"]}
   {:db/id "argument/denken-tut-weh"
    :argument/author "user/schredder"
    :argument/premises [{:db/id "statement/denken-tut-weh"
                         :statement/author "user/rambo"
                         :statement/content "Denken sorgt nur für Kopfschmerzen. Lieber den Donaldo machen!"
                         :statement/version 1}]
    :argument/conclusion "statement/denken"
    :argument/version 1
    :argument/type :argument.type/attack
    :argument/discussions ["discussion/simple"]}])

(def schnaq-test-data
  (concat cat-or-dog-authors-and-users cat-or-dog-statements cat-or-dog-arguments
          cat-or-dog-discussion meetings agendas
          graph-discussion simple-discussion))