(ns schnaq.api.poll
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok bad-request]]
            [schnaq.api.dto-specs :as dto]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.poll :as poll-db]
            [schnaq.database.main :refer [fast-pull]]
            [schnaq.database.specs :as specs]
            [taoensso.timbre :as log]))

(defn- new-poll
  "Create a new poll.
  This can only be done by a registered user, that is also the moderator of the schnaq and
  has at least the pro subscription."
  [{:keys [parameters]}]
  (let [{:keys [title poll-type options share-hash]} (:body parameters)
        discussion-id (:db/id (fast-pull [:discussion/share-hash share-hash] '[:db/id]))
        poll-created (poll-db/new-poll! title poll-type options discussion-id)]
    (if (nil? poll-created)
      (do
        (log/warn "Creating poll with title" title "and options" options "failed for discussion" discussion-id)
        (bad-request (at/build-error-body :poll/bad-parameters "Poll data not valid")))
      (do
        (log/info "Created a poll for discussion" discussion-id "of type" poll-type)
        (ok {:new-poll poll-created})))))

(defn- polls-for-discussion
  "Returns all polls belonging to the `share-hash` in the payload."
  [{:keys [parameters]}]
  (let [share-hash (get-in parameters [:query :share-hash])]
    (ok {:poll (poll-db/polls share-hash)})))

(defn- cast-vote
  "Lets the user cast a vote."
  [{:keys [parameters]}]
  (let [{:keys [share-hash option-id]} (:body parameters)
        poll-id (get-in parameters [:path :poll-id])
        poll-type (-> (fast-pull poll-id '[{:poll/type [:db/ident]}]) :poll/type :db/ident)
        voting-fn (case poll-type
                    :poll.type/single-choice poll-db/vote!
                    :poll.type/multiple-choice poll-db/vote-multiple!)]
    (if (voting-fn option-id poll-id share-hash)
      (do
        (log/info "Vote cast for option(s)" option-id)
        (ok {:voted? true}))
      (do
        (log/debug "Something went wrong with a vote with option(s)" option-id
                   "for poll" poll-id "and share hash" share-hash)
        (bad-request (at/build-error-body :poll.vote/bad-parameters "This vote was not for a valid option or poll"))))))

(def poll-routes
  [["" {:swagger {:tags ["poll"]}}
    ["/poll"
     ["" {:post new-poll
          :description (at/get-doc #'new-poll)
          :middleware [:user/authenticated?
                       :user/beta-tester?
                       :discussion/valid-credentials?]
          :name :poll/create
          :parameters {:body {:title :poll/title
                              :poll-type dto/poll-type
                              :options (s/coll-of ::specs/non-blank-string)
                              :share-hash :discussion/share-hash
                              :edit-hash :discussion/edit-hash}}
          :responses {200 {:body {:new-poll ::dto/poll}}
                      400 at/response-error-body}}]
     ["/:poll-id/vote" {:put cast-vote
                        :description (at/get-doc #'cast-vote)
                        :name :poll/vote!
                        :parameters {:body {:share-hash :discussion/share-hash
                                            :option-id (s/or :id :db/id
                                                             :id-seq (s/coll-of :db/id))}
                                     :path {:poll-id :db/id}}
                        :responses {200 {:body {:voted? boolean?}}
                                    400 at/response-error-body}}]]
    ["/polls" {:get polls-for-discussion
                 :description (at/get-doc #'polls-for-discussion)
                 :name :poll/get
                 :parameters {:query {:share-hash :discussion/share-hash}}
                 :responses {200 {:body {:poll (s/coll-of ::dto/poll)}}}}]]])
