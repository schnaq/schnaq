(ns schnaq.api.survey
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok bad-request]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.specs :as specs]
            [schnaq.database.survey :as survey-db]
            [taoensso.timbre :as log]))

(defn new-survey
  "Create a new survey.
  This can only be done by a registered user, that is also the moderator of the schnaq and
  has at least the pro subscription."
  ;; TODO check the conditions in the description
  [{:keys [parameters]}]
  (let [{:keys [title survey-type options discussion-id]} parameters
        survey-created (survey-db/new-survey title survey-type options discussion-id)]
    (if (nil? survey-created)
      (do
        (log/warn "Creating survey with title" title "and options" options "fialed for discussion" discussion-id)
        (bad-request (at/build-error-body :survey/bad-parameters "Survey data not valid")))
      (do
        (log/info "Created survey for discussion" discussion-id "of type" survey-type)
        (ok {:new-survey survey-created})))))

(def survey-routes
  [["survey" {:swagger {:tags ["survey"]}}
    ["" {:post new-survey
         :description (at/get-doc #'new-survey)
         :name :survey/create
         :parameters {:body {:title :survey/title
                             :survey-type :survey/type
                             :options (s/coll-of ::specs/non-blank-string)
                             :discussion-id :db/id}}
         :responses {200 {:body {:new-survey ::specs/survey}}
                     400 at/response-error-body}}]]])
