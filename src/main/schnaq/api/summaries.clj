(ns schnaq.api.summaries
  (:require [compojure.core :refer [GET POST PUT routes wrap-routes]]
            [ring.util.http-response :refer [ok]]
            [schnaq.auth :as auth]
            [schnaq.config.shared :refer [beta-tester-groups]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.emails :as emails]
            [schnaq.links :as links]
            [schnaq.validator :as validator]
            [taoensso.timbre :as log]))

(defn- request-summary
  "Request a summary of a discussion. Works only if person is in a beta group."
  [{:keys [params identity]}]
  (if identity
    (if (and (some beta-tester-groups (:groups identity))
             (validator/valid-discussion? (:share-hash params)))
      (ok {:summary (discussion-db/summary-request (:share-hash params) (:id identity))})
      (validator/deny-access "You are not allowed to use this feature"))
    (validator/deny-access "You need to be logged in to access this endpoint.")))

(defn- get-summary
  "Return a summary for the specified share-hash."
  [{:keys [params identity]}]
  (if identity
    (if (and (some beta-tester-groups (:groups identity))
             (validator/valid-discussion? (:share-hash params)))
      (ok {:summary (discussion-db/summary (:share-hash params))})
      (validator/deny-access "You are not allowed to use this feature"))
    (validator/deny-access "You need to be logged in to access this endpoint.")))

(defn new-summary
  "Update a summary. If a text exists, it is overwritten. Admin access is already checked by middleware."
  [{:keys [params]}]
  (log/info "Updating Summary for" (:share-hash params))
  (let [summary (discussion-db/update-summary (:share-hash params) (:new-summary-text params))]
    (when (:summary/requester summary)
      (let [title (-> summary :summary/discussion :discussion/title)
            share-hash (-> summary :summary/discussion :discussion/share-hash)]
        (emails/send-mail
          (format "Deine schnaq-Zusammenfassung ist bereit 🥳 \"%s\"" (-> summary :summary/discussion :discussion/title))
          (format "Hallo,%n
eine neue Zusammenfassung wurde für deinen schnaq \"%s\" erstellt und kann und kann unter folgendem Link abgerufen werden: %s

Viele Grüße

Dein schnaq Team"
                  title (links/get-summary-link share-hash))
          (-> summary :summary/requester :user.registered/email))))
    (ok {:new-summary summary})))

(def summary-routes
  (->
    (routes
      (GET "/schnaq/summary" [] get-summary)
      (POST "/schnaq/summary/request" [] request-summary))
    (wrap-routes auth/auth-middleware)))

(def summary-admin-routes
  (-> (PUT "/admin/summary/send" [] new-summary)
      (wrap-routes auth/is-admin-middleware)
      (wrap-routes auth/auth-middleware)))