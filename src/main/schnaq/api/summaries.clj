(ns schnaq.api.summaries
  (:require [clj-http.client :as client]
            [clojure.spec.alpha :as s]
            [muuntaja.core :as m]
            [ring.util.http-response :refer [ok]]
            [schnaq.api.dto-specs :as dto]
            [schnaq.api.toolbelt :as at]
            [schnaq.config :as config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.specs :as specs]
            [schnaq.emails :as emails]
            [schnaq.export :as export]
            [schnaq.links :as links]
            [schnaq.validator :as validator]
            [taoensso.timbre :as log]))

(defn- request-bart-summary [share-hash]
  (client/post
    (config/summy-urls :summary/bart)
    {:body (m/encode "application/json"
                     {:share_hash share-hash
                      :content (export/generate-text-export share-hash)})
     :as :json
     :content-type :json}))


;; -----------------------------------------------------------------------------

(defn- request-summary
  "Request a summary of a discussion. Works only if person is in a beta group."
  [{:keys [parameters identity]}]
  (let [share-hash (get-in parameters [:body :share-hash])]
    (log/info "Requesting new summary for schnaq" share-hash)
    (if (validator/valid-discussion? share-hash)
      (do
        (request-bart-summary share-hash)
        (emails/send-mail
          "[SUMMARY] Es wurde eine neue Summary angefragt 🐳"
          (format "Die Summary wird gerade generiert. Bitte überprüfen und ggf. anpassen. Bitte im Chat absprechen: %s%n%nLink zu den Summaries: %s" (links/get-share-link share-hash) "https://schnaq.com/admin/summaries")
          "info@schnaq.com")
        (ok {:summary (discussion-db/summary-request share-hash (:id identity))}))
      (validator/deny-access "You are not allowed to use this feature"))))

(defn- get-summary
  "Return a summary for the specified share-hash."
  [{:keys [parameters]}]
  (let [share-hash (get-in parameters [:query :share-hash])]
    (if (validator/valid-discussion? share-hash)
      (ok {:summary (discussion-db/summary share-hash)})
      (validator/deny-access "You are not allowed to use this feature"))))

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

(defn- summary-from-summy
  "Route for summy to return summarization results."
  [{:keys [parameters]}]
  (let [share-hash (get-in parameters [:body :share-hash])
        summary-text (get-in parameters [:body :summary])]
    (log/info (format "Received new summary for %s, length: %d" share-hash (count summary-text)))
    (new-summary {:parameters {:body {:share-hash share-hash
                                      :new-summary-text summary-text}}})
    (ok {:status :ok})))


;; -----------------------------------------------------------------------------

(def summary-routes
  [["/schnaq/summary" {:swagger {:tags ["summaries" "beta"]}
                       :middleware [:user/authenticated? :user/beta-tester?]
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
   ["/schnaq/summary/from-summy"
    {:swagger {:tags ["summaries"]}
     :post summary-from-summy
     :middleware [:app/valid-code?]
     :description (at/get-doc #'summary-from-summy)
     :parameters {:body {:share-hash :discussion/share-hash
                         :summary :summary/text
                         :app-code :app/code}}
     :responses {200 {:body {:status keyword?}}}}]
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



(load-file "src/main/schnaq/api.clj")