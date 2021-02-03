(ns schnaq.database.discussion-test-data)

(def public-discussions
  [{:discussion/title "Public Test"
    :discussion/share-hash "public-share-hash"
    :discussion/edit-hash "secret-public-hash"
    :discussion/author "user/testomensch"
    :discussion/states [:discussion.state/public :discussion.state/open]}
   {:discussion/title "Public Test - Deleted"
    :discussion/share-hash "public-share-hash-deleted"
    :discussion/edit-hash "secret-public-hash-deleted"
    :discussion/author "user/testomensch"
    :discussion/states [:discussion.state/public :discussion.state/open :discussion.state/deleted]}

   {:db/id "user/testomensch"
    :user/nickname "Testomensch"}])