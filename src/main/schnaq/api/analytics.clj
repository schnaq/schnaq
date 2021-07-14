(ns schnaq.api.analytics
  (:require [ring.util.http-response :refer [ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.auth :as auth]
            [schnaq.database.analytics :as analytics-db]
            [schnaq.database.specs :as specs]
            [schnaq.toolbelt :as toolbelt]))

(defn- number-of-discussions
  "Returns the number of all discussions."
  [_]
  (ok {:discussions-num (analytics-db/number-of-discussions)}))

(defn- number-of-usernames
  "Returns the number of all anonymous usernames created."
  [_]
  (ok {:usernames-num (analytics-db/number-of-usernames)}))

(defn- number-of-registered-users
  "Returns the number of registered users on the plattform."
  [_]
  (ok {:registered-users-num (analytics-db/number-or-registered-users)}))

(defn- statements-per-discussion
  "Returns the average numbers of statements per discussion."
  [_]
  (ok {:average-statements (float (analytics-db/average-number-of-statements))}))

(defn- number-of-statements
  "Returns the number of statements"
  [_]
  (ok {:statements-num (analytics-db/number-of-statements)}))

(defn- number-of-active-users
  "Returns the number of statements"
  [_]
  (ok {:active-users-num (analytics-db/number-of-active-discussion-users)}))

(defn- statement-lengths-stats
  "Returns statistics about the statement length."
  [_]
  (ok {:statement-length-stats (analytics-db/statement-length-stats)}))

(defn- statement-type-stats
  "Returns statistics about the statement length."
  [_]
  (ok {:statement-type-stats (analytics-db/statement-type-stats)}))

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
          :statement-type-stats (analytics-db/statement-type-stats timestamp-since)
          :registered-users-num (analytics-db/number-or-registered-users)}})))


;; -----------------------------------------------------------------------------

(def analytics-routes
  ["/admin/analytics"
   {:swagger {:tags ["analytics" "admin"]}
    :middleware [auth/auth-middleware auth/admin?-middleware]
    :responses {401 at/response-error-body}}
   ["" {:get all-stats
        :parameters {:query {:days-since nat-int?}}
        :responses {200 {:body {:statistics ::specs/statistics}}}}]
   ["/active-users" {:get number-of-active-users}]
   ["/statements-per-discussion" {:get statements-per-discussion}]
   ["/statement-types" {:get statement-type-stats}]
   ["/discussions" {:get number-of-discussions}]
   ["/statement-lengths" {:get statement-lengths-stats}]
   ["/statements" {:get number-of-statements}]
   ["/usernames" {:get number-of-usernames}]
   ["/registered-users" {:get number-of-registered-users}]])
