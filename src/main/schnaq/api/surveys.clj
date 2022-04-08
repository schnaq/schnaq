(ns schnaq.api.surveys
  (:require [ring.util.http-response :refer [ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.surveys :as survey-db]))

(defn- using-schnaq-for
  "Participate in the survey asking for the topics you are using."
  [{{{:keys [topics]} :body} :parameters
    {:keys [sub]} :identity}]
  (survey-db/participate-using-schnaq-for-survey sub topics)
  (ok {:participated? true}))

;; -----------------------------------------------------------------------------

(def survey-routes
  [["/surveys" {:swagger {:tags ["surveys"]}
                :responses {400 at/response-error-body}}
    ["/participate"
     ["/using-schnaq-for"
      {:post using-schnaq-for
       :description (at/get-doc #'using-schnaq-for)
       :middleware [:user/authenticated?]
       :name :api.surveys.participate/using-schnaq-for
       :parameters {:body {:topics :surveys.using-schnaq-for/topics}}
       :responses {200 {:body {:participated? boolean?}}}}]]]])
