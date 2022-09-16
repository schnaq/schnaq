(ns schnaq.api.poll
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [bad-request ok]]
            [schnaq.api.dto-specs :as dto]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.main :refer [fast-pull set-activation-focus]]
            [schnaq.database.poll :as poll-db]
            [schnaq.database.specs :as specs]
            [taoensso.timbre :as log]))

(defn- new-poll
  "Create a new poll.
  This can only be done by a registered user, that is also the moderator of the schnaq and
  has at least the pro subscription."
  [{:keys [parameters]}]
  (let [{:keys [title poll-type options share-hash hide-results?]} (:body parameters)
        poll-created (poll-db/new-poll! share-hash title poll-type options hide-results?)]
    (if (nil? poll-created)
      (do
        (log/warn (format "Creating poll with title %s and options %s failed for discussion %s" title options share-hash))
        (bad-request (at/build-error-body :poll/bad-parameters "Poll data not valid")))
      (do
        (log/info (format "Created a poll for discussion %s of type %s" share-hash poll-type))
        (set-activation-focus [:discussion/share-hash share-hash] (:db/id poll-created))
        (ok {:new-poll poll-created})))))

(defn polls-for-discussion
  "Returns all polls belonging to the `share-hash` in the payload."
  [{:keys [parameters]}]
  (let [share-hash (get-in parameters [:query :share-hash])]
    (ok {:polls (poll-db/polls share-hash)})))

(defn- cast-vote
  "Lets the user cast a vote."
  [{:keys [parameters]}]
  (let [{:keys [share-hash option-id]} (:body parameters)
        ;; For multiple or ranking option-id is a collection
        poll-id (get-in parameters [:path :poll-id])
        poll-type (-> (fast-pull poll-id '[{:poll/type [:db/ident]}]) :poll/type :db/ident)
        voting-fn (case poll-type
                    :poll.type/single-choice poll-db/vote!
                    :poll.type/multiple-choice poll-db/vote-multiple!
                    :poll.type/ranking poll-db/vote-ranking!)]
    (if (voting-fn share-hash poll-id option-id)
      (do
        (log/info "Vote cast for option(s)" option-id)
        (ok {:voted? true}))
      (do
        (log/debug "Something went wrong with a vote with option(s)" option-id
                   "for poll" poll-id "and share hash" share-hash)
        (bad-request (at/build-error-body :poll.vote/bad-parameters "This vote was not for a valid option or poll"))))))

(defn- delete-poll
  "Delete a poll."
  [{{{:keys [share-hash poll-id]} :body} :parameters}]
  (log/debug "Poll deletion for" poll-id)
  (poll-db/delete-poll! share-hash poll-id)
  (ok {:deleted? true}))

(defn get-poll
  "Query a poll."
  [{{{:keys [share-hash poll-id]} :query} :parameters}]
  (if-let [poll (poll-db/poll-from-discussion share-hash poll-id)]
    (ok {:poll poll})
    (bad-request
     (at/build-error-body :poll.get/invalid-params
                          (format "Could not find poll-id %d for %s" poll-id share-hash)))))

(defn- toggle-hide-results
  "Toggle result visibility for participants."
  [{{{:keys [share-hash poll-id hide-results?]} :body} :parameters}]
  (poll-db/toggle-hide-poll-results share-hash poll-id hide-results?)
  (ok {:hide-results? hide-results?}))

;; -----------------------------------------------------------------------------

(def poll-routes
  [["" {:swagger {:tags ["poll"]}}
    ["/poll"
     ["" {:name :api/poll
          :post {:handler new-poll
                 :description (at/get-doc #'new-poll)
                 :middleware [:discussion/user-moderator?
                              :user/pro?]
                 :parameters {:body {:title :poll/title
                                     :poll-type dto/poll-type
                                     :options (s/coll-of ::specs/non-blank-string)
                                     :share-hash :discussion/share-hash
                                     :hide-results? :poll/hide-results?}}
                 :responses {200 {:body {:new-poll ::dto/poll}}
                             400 at/response-error-body
                             403 at/response-error-body}}
          :get {:handler get-poll
                :description (at/get-doc #'get-poll)
                :parameters {:query {:share-hash :discussion/share-hash
                                     :poll-id :db/id}}
                :responses {200 {:body {:poll ::specs/poll}}}}}]
     ["/:poll-id" {:parameters {:path {:poll-id :db/id}}
                   :responses {400 at/response-error-body}}
      ["/vote"
       {:put cast-vote
        :description (at/get-doc #'cast-vote)
        :name :poll/vote!
        :middleware [:discussion/valid-writeable-discussion?]
        :parameters {:body {:share-hash :discussion/share-hash
                            :option-id (s/or :id :db/id
                                             :id-seq (s/coll-of :db/id))}}
        :responses {200 {:body {:voted? boolean?}}}}]]
     ["/hide-results"
      {:put toggle-hide-results
       :description (at/get-doc #'toggle-hide-results)
       :name :api.poll/hide-results
       :middleware [:discussion/valid-writeable-discussion?
                    :discussion/valid-credentials?]
       :parameters {:body {:share-hash :discussion/share-hash
                           :edit-hash :discussion/edit-hash
                           :poll-id :db/id
                           :hide-results? :poll/hide-results?}}
       :responses {200 {:body {:hide-results? :poll/hide-results?}}
                   400 at/response-error-body}}]
     ["/delete" {:delete delete-poll
                 :description (at/get-doc #'delete-poll)
                 :name :poll/delete
                 :parameters {:body {:poll-id :db/id
                                     :share-hash :discussion/share-hash
                                     :edit-hash :discussion/edit-hash}}
                 :responses {200 {:body {:deleted? boolean?}}
                             400 at/response-error-body}
                 :middleware [:user/authenticated?
                              :discussion/valid-credentials?]}]]
    ["/polls" {:get polls-for-discussion
               :description (at/get-doc #'polls-for-discussion)
               :name :polls/get
               :parameters {:query {:share-hash :discussion/share-hash}}
               :responses {200 {:body {:polls (s/coll-of ::dto/poll)}}}}]]])
