(ns schnaq.api.summaries
  (:require [ring.util.http-response :refer [ok]]
            [schnaq.auth :as auth]
            [schnaq.config.shared :refer [beta-tester-groups]]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.emails :as emails]
            [schnaq.links :as links]
            [schnaq.validator :as validator]
            [taoensso.timbre :as log]))

(defn- request-summary
  "Request a summary of a discussion. Works only if person is in a beta group."
  [{:keys [parameters identity]}]
  (let [share-hash (get-in parameters [:body :share-hash])]
    (log/info "Requesting new summary for schnaq" share-hash)
    (if identity
      (if (and (some beta-tester-groups (:groups identity))
               (validator/valid-discussion? share-hash))
        (do
          (emails/send-mail
            "[SUMMARY] Es wurde eine neue Summary angefragt 🐳"
            (format "Bitte im Chat absprechen und Zusammenfassung zu folgendem schnaq anlegen: %s%n%nLink zu den Summaries: %s" (links/get-share-link share-hash) "https://schnaq.com/admin/summaries")
            "info@schnaq.com")
          (ok {:summary (discussion-db/summary-request share-hash (:id identity))}))
        (validator/deny-access "You are not allowed to use this feature"))
      (validator/deny-access "You need to be logged in to access this endpoint."))))

(defn- get-summary
  "Return a summary for the specified share-hash."
  [{:keys [parameters identity]}]
  (if identity
    (let [share-hash (get-in parameters [:query :share-hash])]
      (if (and (some beta-tester-groups (:groups identity))
               (validator/valid-discussion? share-hash))
        (ok {:summary (discussion-db/summary share-hash)})
        (validator/deny-access "You are not allowed to use this feature")))
    (validator/deny-access "You need to be logged in to access this endpoint.")))

(defn new-summary
  "Update a summary. If a text exists, it is overwritten. Admin access is already checked by middleware."
  [{:keys [parameters]}]
  (let [share-hash (get-in parameters [:body :share-hash])
        new-summary-text (get-in parameters [:body :new-summary-text])
        summary (discussion-db/update-summary share-hash new-summary-text)]
    (log/info "Updating Summary for" share-hash)
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


;; -----------------------------------------------------------------------------

(def summary-routes
  [["/schnaq/summary" {:swagger {:tags ["summaries"]}
                       :middleware [auth/auth-middleware]
                       :parameters {:body {:share-hash string?}}}
    ["" {:get get-summary}]
    ["/request" {:post request-summary}]]
   ["/admin/summary/send" {:swagger {:tags ["summaries"]}
                           :middleware [auth/auth-middleware auth/is-admin-middleware]
                           :parameters {:body {:share-hash :discussion/share-hash
                                               :new-summary-text :summary/text}}
                           :put new-summary}]])
