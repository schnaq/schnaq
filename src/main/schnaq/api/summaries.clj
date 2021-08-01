(ns schnaq.api.summaries
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok]]
            [schnaq.api.dto-specs :as dto]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.specs :as specs]
            [schnaq.emails :as emails]
            [schnaq.links :as links]
            [schnaq.validator :as validator]
            [taoensso.timbre :as log]))

(defn- request-summary
  "Request a summary of a discussion. Works only if person is in a beta group."
  [{:keys [parameters identity]}]
  (let [share-hash (get-in parameters [:body :share-hash])]
    (log/info "Requesting new summary for schnaq" share-hash)
    (emails/send-mail
      "[SUMMARY] Es wurde eine neue Summary angefragt 🐳"
      (format "Bitte im Chat absprechen und Zusammenfassung zu folgendem schnaq anlegen: %s%n%nLink zu den Summaries: %s" (links/get-share-link share-hash) "https://schnaq.com/admin/summaries")
      "info@schnaq.com")
    (ok {:summary (discussion-db/summary-request share-hash (:id identity))})))

(defn- get-summary
  "Return a summary for the specified share-hash."
  [{:keys [parameters]}]
  (let [share-hash (get-in parameters [:query :share-hash])]
    (ok {:summary (discussion-db/summary share-hash)})))

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

(defn- all-summaries
  "Returns all summaries and their discussions."
  [_]
  (ok {:summaries (discussion-db/all-summaries-with-discussions)}))

;; -----------------------------------------------------------------------------

(def summary-routes
  [["/schnaq/summary" {:swagger {:tags ["summaries" "beta"]}
                       :middleware [:user/authenticated? :user/beta-tester?
                                    :discussion/valid-share-hash?]
                       :responses {401 at/response-error-body}}
    ["" {:get get-summary
         :description (at/get-doc #'get-summary)
         :parameters {:query {:share-hash :discussion/share-hash}}
         :responses {200 {:body {:summary (s/or :summary ::dto/summary
                                                :not-found nil?)}}}}]
    ["/request" {:post request-summary
                 :description (at/get-doc #'request-summary)
                 :parameters {:body {:share-hash :discussion/share-hash}}
                 :responses {200 {:body {:summary ::specs/summary}}}}]]
   ["/admin" {:swagger {:tags ["summaries" "admin" "beta"]}
              :middleware [:user/authenticated? :user/admin?]
              :responses {401 at/response-error-body}}
    ["/summary/send" {:put new-summary
                      :description (at/get-doc #'new-summary)
                      :parameters {:body {:share-hash :discussion/share-hash
                                          :new-summary-text :summary/text}}
                      :responses {200 {:body {:new-summary ::specs/summary}}}}]
    ["/summaries" {:get all-summaries
                   :description (at/get-doc #'all-summaries)
                   :responses {200 {:body {:summaries (s/or :collection (s/coll-of ::dto/summary)
                                                            :empty nil?)}}}}]]])
