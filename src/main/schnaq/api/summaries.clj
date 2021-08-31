(ns schnaq.api.summaries
  (:require [clj-http.client :as client]
            [clojure.spec.alpha :as s]
            [ghostwheel.core :refer [>defn-]]
            [muuntaja.core :as m]
            [reitit.core :as r]
            [ring.util.http-response :refer [ok]]
            [schnaq.api.dto-specs :as dto]
            [schnaq.api.toolbelt :as at]
            [schnaq.config.shared :as shared-config]
            [schnaq.config.summy :as summy-config]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.database.specs :as specs]
            [schnaq.export :as export]
            [schnaq.links :as links]
            [schnaq.mail.emails :as emails]
            [schnaq.mail.template :as template]
            [taoensso.timbre :as log]))

(declare summary-routes)

(>defn- respond-to-route
  "Look up route to receive results from summy."
  [route-name]
  [keyword? :ret string?]
  (str
    shared-config/api-url
    (:path
      (r/match-by-name (r/router summary-routes) route-name))))

(defn- request-bart-summary
  "Request a bart summary at summy."
  [share-hash]
  (let [url (summy-config/urls :summary/bart)
        respond-url (respond-to-route :summary/from-summy)]
    (log/info (format "Requesting bart-summary, endpoint: %s, share-hash: %s, respond_url: %s" url share-hash respond-url))
    (client/post url
                 {:body (m/encode "application/json"
                                  {:respond_url (respond-to-route :summary/from-summy)
                                   :share_hash share-hash
                                   :app_code summy-config/app-code
                                   :content (export/generate-fulltext share-hash)})
                  :as :json
                  :content-type :json})))


;; -----------------------------------------------------------------------------

(defn- request-summary
  "Request a summary of a discussion. Works only if person is in a beta group."
  [{:keys [parameters identity]}]
  (let [share-hash (get-in parameters [:body :share-hash])]
    (log/info "Requesting new summary for schnaq" share-hash)
    (request-bart-summary share-hash)
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
        (emails/send-mail-with-body
          (format "Deine schnaq-Zusammenfassung ist bereit 🥳 \"%s\"" (-> summary :summary/discussion :discussion/title))
          (-> summary :summary/requester :user.registered/email)
          (template/mail
            "Deine Zusammenfassung ist bereit"
            (str "Hallo " (-> summary :summary/requester :user.registered/display-name) ",")
            (format "eine neue Zusammenfassung wurde für deinen schnaq \"%s\" erstellt und kann und kann unter folgendem Link abgerufen werden: %s"
                    title (links/get-summary-link share-hash))))))
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
                       :middleware (cond->
                                     [:discussion/valid-share-hash? :user/authenticated?]
                                     (not shared-config/embedded?) (conj :user/beta-tester?))
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
     :name :summary/from-summy
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
