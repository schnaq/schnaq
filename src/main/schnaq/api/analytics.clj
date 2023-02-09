(ns schnaq.api.analytics
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.analytics :as analytics-db]
            [schnaq.database.specs :as specs]
            [schnaq.toolbelt :as toolbelt]))

(defn- all-stats
  "Returns all statistics at once."
  [{:keys [parameters]}]
  (let [timestamp-since (toolbelt/now-minus-days (get-in parameters [:query :days-since]))]
    (ok {:statistics
         {:discussions-sum (analytics-db/number-of-discussions timestamp-since)
          :usernames-sum (analytics-db/number-of-usernames timestamp-since)
          :average-statements-num (float (analytics-db/average-number-of-statements timestamp-since))
          :statements-num (analytics-db/number-of-statements timestamp-since)
          :active-users-num (analytics-db/number-of-active-discussion-users timestamp-since)
          :statement-length-stats (analytics-db/statement-length-stats timestamp-since)
          :statement-percentiles (analytics-db/statistical-statement-num-data timestamp-since)
          :statement-type-stats (analytics-db/statement-type-stats timestamp-since)
          :registered-users-num (analytics-db/number-or-registered-users timestamp-since)
          :pro-users-num (analytics-db/number-of-pro-users timestamp-since)
          :labels-stats (analytics-db/labels-stats timestamp-since)
          :usage (analytics-db/schnaq-usage timestamp-since)
          :users (analytics-db/users-created-since timestamp-since)}})))

(defn- by-emails
  "Takes a list of email patterns and returns statistics for all users matching."
  [{:keys [parameters]}]
  (let [patterns (get-in parameters [:query :patterns])
        patterns-col (if (string? patterns) [patterns] patterns)
        regexs (map re-pattern patterns-col)]
    (ok {:statistics (analytics-db/statistics-for-users-by-email-patterns regexs)})))

;; -----------------------------------------------------------------------------

(def analytics-routes
  ["/admin/analytics"
   {:swagger {:tags ["analytics" "admin"]}
    :middleware [:user/authenticated? :user/analytics-admin?]
    :responses {401 at/response-error-body}}
   ["" {:get all-stats
        :parameters {:query {:days-since nat-int?}}
        :responses {200 {:body {:statistics ::specs/statistics}}}}]
   ["/by-emails"
    {:get by-emails
     :parameters {:query {:patterns any?}}
     :responses {200 {:body {:statistics (s/or :statistics map?
                                               :nothing-found nil?)}}}}]])
