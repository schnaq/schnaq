(ns schnaq.api.common
  (:require [ring.util.http-response :refer [ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.discussion :as discussion-db]
            [schnaq.export :as export]
            [taoensso.timbre :as log]))

(defn- ping
  "Route to ping the API. Used in our monitoring system."
  [_]
  (ok {:text "🧙‍♂️"}))

(defn- check-credentials-opt-add-as-admin!
  "The middleware calling this function checks for the validity of the credentials.
  If the user is logged in, add them as admin."
  [{:keys [parameters identity]}]
  (let [{:keys [share-hash]} (:body parameters)
        keycloak-id (:sub identity)]
    (when keycloak-id
      (discussion-db/add-admin-to-discussion share-hash keycloak-id))
    (ok {:valid-credentials? true})))

(defn- export-as-argdown
  "Exports the complete discussion in an argdown-formatted file."
  [{{{:keys [share-hash]} :query} :parameters}]
  (let [statements (discussion-db/all-statements share-hash)]
    (log/info "User is generating an argdown export for discussion" share-hash)
    (ok {:string-representation (export/generate-argdown statements)})))

(defn- export-as-fulltext
  "Exports the complete discussion as an fulltext file."
  [{{{:keys [share-hash]} :query} :parameters}]
  (let [statements (discussion-db/all-statements share-hash)]
    (log/info "User is generating a fulltext export for discussion" share-hash)
    (ok {:string-representation (export/generate-fulltext statements)})))

;; -----------------------------------------------------------------------------

(def other-routes
  [["" {:swagger {:tags ["other"]}}
    ["/ping" {:get ping
              :name :api.other/ping
              :description (at/get-doc #'ping)
              :responses {200 {:body {:text string?}}}}]
    ["/export" {:middleware [:discussion/valid-share-hash?]
                :parameters {:query {:share-hash :discussion/share-hash}}
                :responses {200 {:body {:string-representation string?}}
                            404 at/response-error-body}}
     ["/argdown" {:get export-as-argdown
                  :description (at/get-doc #'export-as-argdown)}]
     ["/fulltext" {:get export-as-fulltext
                   :description (at/get-doc #'export-as-fulltext)}]]
    ["/credentials/validate" {:post check-credentials-opt-add-as-admin!
                              :description (at/get-doc #'check-credentials-opt-add-as-admin!)
                              :middleware [:discussion/valid-credentials?]
                              :responses {200 {:body {:valid-credentials? boolean?}}
                                          403 at/response-error-body}
                              :parameters {:body {:share-hash :discussion/share-hash
                                                  :edit-hash :discussion/edit-hash}}}]]])
