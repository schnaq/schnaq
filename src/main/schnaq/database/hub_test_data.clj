(ns schnaq.database.hub-test-data)

(def ^:private hub-schnaqs
  [{:discussion/title "Hub Discussion"
    :discussion/share-hash "public-share-hash"
    :discussion/edit-hash "secret-public-hash"
    :discussion/author "user/hub-tester"
    :discussion/states [:discussion.state/public :discussion.state/open]}
   {:discussion/title "Another Hub Discussion"
    :discussion/share-hash "public-share-hash-hubby"
    :discussion/edit-hash "secret-public-hash-hubby"
    :discussion/author "user/hub-tester"
    :discussion/states [:discussion.state/public :discussion.state/open]}])

(def hub-test-data
  [{:hub/keycloak-name "test-keycloak"
    :hub/name "YouHub"
    :hub/schnaqs hub-schnaqs}
   {:hub/keycloak-name "some-empty-hub"
    :hub/name "Phub"}

   {:db/id "user/hub-tester"
    :user/nickname "Hub Tester"}])