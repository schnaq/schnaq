(ns schnaq.api.analytics
  (:require [compojure.core :refer [GET routes wrap-routes context]]
            [ring.util.http-response :refer [ok]]
            [schnaq.auth :as auth]
            [schnaq.database.analytics :as analytics-db]
            [schnaq.toolbelt :as toolbelt]))

(defn- number-of-discussions
  "Returns the number of all meetings."
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
  "Returns the average numbers of meetings"
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

(defn- argument-type-stats
  "Returns statistics about the statement length."
  [_]
  (ok {:argument-type-stats (analytics-db/argument-type-stats)}))

(defn- all-stats
  "Returns all statistics at once."
  [{:keys [params]}]
  (let [timestamp-since (toolbelt/now-minus-days (Integer/parseInt (:days-since params)))]
    (ok {:stats {:discussions-num (analytics-db/number-of-discussions timestamp-since)
                 :usernames-num (analytics-db/number-of-usernames timestamp-since)
                 :average-statements (float (analytics-db/average-number-of-statements timestamp-since))
                 :statements-num (analytics-db/number-of-statements timestamp-since)
                 :active-users-num (analytics-db/number-of-active-discussion-users timestamp-since)
                 :statement-length-stats (analytics-db/statement-length-stats timestamp-since)
                 :argument-type-stats (analytics-db/argument-type-stats timestamp-since)
                 :registered-users-num (analytics-db/number-or-registered-users)}})))


;; -----------------------------------------------------------------------------

(def analytics-routes
  (->
    (routes
      (context "/analytics" []
        (GET "/" [] all-stats)                              ;; matches /analytics and /analytics/
        (GET "/active-users" [] number-of-active-users)
        (GET "/statements-per-discussion" [] statements-per-discussion)
        (GET "/argument-types" [] argument-type-stats)
        (GET "/discussions" [] number-of-discussions)
        (GET "/statement-lengths" [] statement-lengths-stats)
        (GET "/statements" [] number-of-statements)
        (GET "/usernames" [] number-of-usernames)
        (GET "/registered-users" [] number-of-registered-users)))
    (wrap-routes auth/is-admin-middleware)
    (wrap-routes auth/auth-middleware)
    (wrap-routes auth/wrap-jwt-authentication)))
