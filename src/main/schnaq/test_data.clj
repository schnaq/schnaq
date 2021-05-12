(ns schnaq.test-data)

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
    :statement/discussions ["discussion/cat-or-dog"]}])

(def ^:private cat-or-dog-arguments
  [{:db/id "argument/watchdogs"
    :argument/author "user/wegi"
    :argument/premises [{:db/id "statement/watchdogs"
                         :statement/author "user/wegi"
                         :statement/content "dogs can act as watchdogs"
                         :statement/created-at #inst "2020-01-01"
                         :statement/parent "statement/get-dog"
                         :statement/type :statement.type/support
                         :statement/version 1
                         :statement/discussions ["discussion/cat-or-dog" "discussion/tapir-or-ameisenbaer"]}]
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
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/get-dog"
                         :statement/type :statement.type/attack
                         :statement/discussions ["discussion/cat-or-dog" "discussion/tapir-or-ameisenbaer"]}]
    :argument/conclusion "statement/get-dog"
    :argument/version 1
    :argument/type :argument.type/attack
    :argument/discussions ["discussion/cat-or-dog" "discussion/tapir-or-ameisenbaer"]}
   ;; todo undercut, marked for deletion
   {:argument/author "user/stinky"
    :argument/premises [{:db/id "statement/no-use"
                         :statement/author "user/stinky"
                         :statement/content "we have no use for a watchdog"
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1}]
    :argument/conclusion "argument/watchdogs"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog"]}
   ;; todo undercut, marked for deletion
   {:argument/author "user/stinky"
    :argument/premises [{:db/id "statement/exercise"
                         :statement/author "user/stinky"
                         :statement/content
                         "going for a walk with the dog every day is good for social interaction and physical exercise"
                         :statement/created-at #inst "2020-01-01"
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
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/get-both"
                         :statement/type :statement.type/support
                         :statement/discussions ["discussion/cat-or-dog"]}]
    :argument/conclusion "statement/get-both"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "user/wegi"
    :argument/premises [{:db/id "statement/moneeey"
                         :statement/author "user/wegi"
                         :statement/content "we do not have enough money for two pets"
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/no-problem"
                         :statement/type :statement.type/attack
                         :statement/discussions ["discussion/cat-or-dog"]}]
    :argument/conclusion "statement/no-problem"
    :argument/version 1
    :argument/type :argument.type/attack
    :argument/discussions ["discussion/cat-or-dog"]}
   ;; Here be premise groups
   ;; todo undercut, marked for deletion
   {:db/id "argument/hate"
    :argument/author "user/stinky"
    :argument/premises [{:db/id "statement/best-friends"
                         :statement/author "user/stinky"
                         :statement/content "won't be best friends"
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1}
                        {:db/id "statement/strong-hate"
                         :statement/author "user/stinky"
                         :statement/content
                         "a cat and a dog will generally not get along well"
                         :statement/created-at #inst "2020-01-01"
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
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/get-cat"
                         :statement/type :statement.type/support
                         :statement/discussions ["discussion/cat-or-dog"]}]
    :argument/conclusion "statement/get-cat"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   ;; todo undercut, marked for deletion
   {:argument/author "user/wegi"
    :argument/premises [{:db/id "statement/take-care-baby"
                         :statement/author "user/wegi"
                         :statement/content
                         "the purpose of a pet is to have something to take care of"
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1}]
    :argument/conclusion "argument/independent-cats"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "user/wegi"
    :argument/premises [{:db/id "statement/overbred"
                         :statement/author "user/wegi"
                         :statement/content "this is not true for overbred races"
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/independent"
                         :statement/type :statement.type/attack
                         :statement/discussions ["discussion/cat-or-dog"]}]
    :argument/conclusion "statement/independent"
    :argument/version 1
    :argument/type :argument.type/attack
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "user/schredder"
    :argument/premises [{:db/id "statement/darwin-likes"
                         :statement/author "user/schredder"
                         :statement/content "this lies in their natural conditions"
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/independent"
                         :statement/type :statement.type/support
                         :statement/discussions ["discussion/cat-or-dog"]}]
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
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/independent"
                         :statement/type :statement.type/support
                         :statement/discussions ["discussion/cat-or-dog"]}]
    :argument/conclusion "statement/independent"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   ;; todo undercut, marked for deletion
   {:argument/author "user/wegi"
    :argument/premises [{:db/id "statement/wild-thang"
                         :statement/author "user/wegi"
                         :statement/content "house cats are not wild cats anymore"
                         :statement/created-at #inst "2020-01-01"
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
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/get-cat"
                         :statement/type :statement.type/support
                         :statement/discussions ["discussion/cat-or-dog"]}]
    :argument/conclusion "statement/get-cat"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   ;; todo undercut, marked for deletion
   {:argument/author "user/stinky"
    :argument/premises [{:db/id "statement/credibility"
                         :statement/author "user/stinky"
                         :statement/content "thats what you just say without a credible source"
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1}]
    :argument/conclusion "argument/no-taxes"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "user/rambo"
    :argument/premises [{:db/id "statement/germoney"
                         :statement/author "user/rambo"
                         :statement/content "in germany a second dog costs even more taxes"
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/taxes"
                         :statement/type :statement.type/support
                         :statement/discussions ["discussion/cat-or-dog"]}]
    :argument/conclusion "statement/taxes"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "user/rambo"
    :argument/premises [{:db/id "statement/doggo-same"
                         :statement/author "user/rambo"
                         :statement/content "other costs of living for cats and dogs are nearly the same"
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/taxes"
                         :statement/type :statement.type/support
                         :statement/discussions ["discussion/cat-or-dog"]}]
    :argument/conclusion "statement/taxes"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/cat-or-dog"]}
   {:db/id "argument/moody-cats"
    :argument/author "user/wegi"
    :argument/premises [{:db/id "statement/moody"
                         :statement/author "user/wegi"
                         :statement/content "cats are capricious"
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/get-cat"
                         :statement/type :statement.type/attack
                         :statement/discussions ["discussion/cat-or-dog"]}]
    :argument/conclusion "statement/get-cat"
    :argument/version 1
    :argument/type :argument.type/attack
    :argument/discussions ["discussion/cat-or-dog"]}
   ;; todo undercut, marked for deletion
   {:argument/author "user/schredder"
    :argument/premises [{:db/id "statement/race-dogs"
                         :statement/author "user/schredder"
                         :statement/content
                         (str "this is based on the cats race and on the breeding"
                              ", and is not inherent for cats.")
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1}]
    :argument/conclusion "argument/moody-cats"
    :argument/version 1
    :argument/type :argument.type/undercut
    :argument/discussions ["discussion/cat-or-dog"]}
   ;; todo undercut, marked for deletion
   {:argument/author "user/schredder"
    :argument/premises [{:db/id "statement/stinky-cats"
                         :statement/author "user/schredder"
                         :statement/content "cats are only moody if youre stinky."
                         :statement/created-at #inst "2020-01-01"
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
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/moody"
                         :statement/type :statement.type/attack
                         :statement/discussions ["discussion/cat-or-dog"]}]
    :argument/conclusion "statement/moody"
    :argument/version 1
    :argument/type :argument.type/attack
    :argument/discussions ["discussion/cat-or-dog"]}
   {:argument/author "user/schredder"
    :argument/premises [{:db/id "statement/not-all-cats"
                         :statement/author "user/schredder"
                         :statement/content "not every cat is capricious"
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/moody"
                         :statement/type :statement.type/attack
                         :statement/discussions ["discussion/cat-or-dog"]}]
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
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/moody"
                         :statement/type :statement.type/support
                         :statement/discussions ["discussion/cat-or-dog"]}]
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
    :discussion/created-at #inst "2019-01-01"
    :discussion/starting-statements ["statement/get-dog" "statement/get-both" "statement/get-cat"]}
   {:db/id "discussion/tapir-or-ameisenbaer"
    :discussion/title "Tapir oder Ameisenbär?"
    :discussion/created-at #inst "2019-01-01"
    :discussion/share-hash "ameisenbär-hash"
    :discussion/description "What do what do"
    :discussion/author "user/wegi"
    :discussion/states [:discussion.state/open]
    :discussion/starting-statements ["statement/get-dog"]}])

(def ^:private graph-discussion
  [{:db/id "discussion/graph"
    :discussion/title "Wetter Graph"
    :discussion/created-at #inst "2019-01-01"
    :discussion/share-hash "graph-hash"
    :discussion/edit-hash "graph-edit-hash"
    :discussion/author "user/wegi"
    :discussion/description "Der Graph muss korrekt sein"
    :discussion/states [:discussion.state/open]
    :discussion/starting-statements ["statement/warm" "statement/foo"]}
   {:db/id "argument/warm"
    :argument/author "user/rambo"
    :argument/premises [{:db/id "statement/GrossFoo"
                         :statement/author "user/rambo"
                         :statement/content "Foo"
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/warm"
                         :statement/type :statement.type/support
                         :statement/discussions ["discussion/graph"]}]
    :argument/conclusion {:db/id "statement/warm"
                          :statement/author "user/rambo"
                          :statement/content "Es ist warm"
                          :statement/created-at #inst "2020-01-01"
                          :statement/version 1
                          :statement/discussions ["discussion/graph"]}
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/graph"]}
   {:db/id "argument/B"
    :argument/author "user/schredder"
    :argument/premises [{:db/id "statement/B"
                         :statement/author "user/rambo"
                         :statement/content "B"
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/GrossFoo"
                         :statement/type :statement.type/support
                         :statement/discussions ["discussion/graph"]}]
    :argument/conclusion "statement/GrossFoo"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/graph"]}
   {:db/id "argument/SonneScheint"
    :argument/author "user/schredder"
    :argument/premises [{:db/id "statement/SonneScheint"
                         :statement/author "user/rambo"
                         :statement/content "Die Sonne scheint!"
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/warm"
                         :statement/type :statement.type/support
                         :statement/discussions ["discussion/graph"]}]
    :argument/conclusion "statement/warm"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/graph"]}
   {:db/id "argument/SonneScheintC"
    :argument/author "user/schredder"
    :argument/premises [{:db/id "statement/C"
                         :statement/author "user/rambo"
                         :statement/content "Die Sonne gibt Vitamin C"
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/SonneScheint"
                         :statement/type :statement.type/support
                         :statement/discussions ["discussion/graph"]}]
    :argument/conclusion "statement/SonneScheint"
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/graph"]}
   {:db/id "argument/foo"
    :argument/author "user/schredder"
    :argument/premises [{:db/id "statement/Bar"
                         :statement/author "user/rambo"
                         :statement/content "Bar"
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/foo"
                         :statement/type :statement.type/support
                         :statement/discussions ["discussion/graph"]}]
    :argument/conclusion {:db/id "statement/foo"
                          :statement/author "user/rambo"
                          :statement/content "foo"
                          :statement/created-at #inst "2020-01-01"
                          :statement/version 1
                          :statement/discussions ["discussion/graph"]}
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/graph"]}])

(def ^:private simple-discussion
  [{:db/id "discussion/simple"
    :discussion/title "Simple Discussion"
    :discussion/created-at #inst "2019-01-01"
    :discussion/share-hash "simple-hash"
    :discussion/author "user/wegi"
    :discussion/edit-hash "simple-hash-secret"
    :discussion/description "A very simple discussion"
    :discussion/states [:discussion.state/open]
    :discussion/starting-statements ["statement/brainstorm"]}
   {:db/id "argument/simple-start"
    :argument/author "user/rambo"
    :argument/premises [{:db/id "statement/denken"
                         :statement/author "user/rambo"
                         :statement/content "Man denkt viel nach dabei"
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/brainstorm"
                         :statement/type :statement.type/support
                         :statement/discussions ["discussion/simple"]}]
    :argument/conclusion {:db/id "statement/brainstorm"
                          :statement/author "user/rambo"
                          :statement/content "Brainstorming ist total wichtig"
                          :statement/created-at #inst "2020-01-01"
                          :statement/creation-secret "secret-creation-secret"
                          :statement/version 1
                          :statement/discussions ["discussion/simple"]}
    :argument/version 1
    :argument/type :argument.type/support
    :argument/discussions ["discussion/simple"]}
   ;; todo undercut, marked for deletion
   {:db/id "argument/denken-nix-brainstorm"
    :argument/author "user/schredder"
    :argument/premises [{:db/id "statement/denken-nix-brainstorm"
                         :statement/author "user/rambo"
                         :statement/content "Brainstorm hat nichts mit aktiv denken zu tun"
                         :statement/created-at #inst "2020-01-01"
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
                         :statement/created-at #inst "2020-01-01"
                         :statement/version 1
                         :statement/parent "statement/denken"
                         :statement/type :statement.type/attack
                         :statement/discussions ["discussion/simple"]}]
    :argument/conclusion "statement/denken"
    :argument/version 1
    :argument/type :argument.type/attack
    :argument/discussions ["discussion/simple"]}])

(def ^:private registered-users
  [{:db/id "user.registered/alex"
    :user.registered/keycloak-id "59456d4a-6950-47e8-88d8-a1a6a8de9276",
    :user.registered/display-name "A. Schneider",
    :user.registered/email "alexander@schneider.gg",
    :user.registered/last-name "Schneider",
    :user.registered/first-name "Alexander"
    :user.registered/groups ["test-group"]}])

(def schnaq-test-data
  (concat cat-or-dog-authors-and-users cat-or-dog-statements cat-or-dog-arguments
          cat-or-dog-discussion
          graph-discussion simple-discussion
          registered-users))