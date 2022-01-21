(ns schnaq.api.activation
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.activation :as activation-db]
            [schnaq.database.specs :as specs]
            [taoensso.timbre :as log]))

(defn- start-activation
  "Start activation-feature for a discussion. Only creates a new one if none
  exists."
  [{{{:keys [share-hash]} :body} :parameters}]
  (log/info "Starting activation for" share-hash)
  (ok {:activation (activation-db/start-activation! share-hash)}))

(defn- get-activation
  "Get the current activation for a discussion."
  [{{{:keys [share-hash]} :query} :parameters}]
  (log/info "Get activation for" share-hash)
  (ok {:activation (activation-db/activation-by-share-hash share-hash)}))

(defn- increment-activation-counter
  "Increment activation counter."
  [{{{:keys [share-hash]} :body} :parameters}]
  (log/info "Increment activation counter for" share-hash)
  (ok {:activation (activation-db/increment-activation! share-hash)}))

(defn- reset-activation
  "Reset activation counter."
  [{{{:keys [share-hash]} :body} :parameters}]
  (log/info "Reset activation counter for" share-hash)
  (ok {:activation (activation-db/reset-activation! share-hash)}))

(def activation-routes
  [["" {:swagger {:tags ["activation"]}}
    ["/activation"
     ["" {:put start-activation
          :description (at/get-doc #'start-activation)
          :middleware [:user/authenticated?
                       :user/beta-tester?
                       :discussion/valid-credentials?]
          :name :activation/start
          :parameters {:body {:share-hash :discussion/share-hash
                              :edit-hash :discussion/edit-hash}}
          :responses {200 {:body {:activation ::specs/activation}}
                      400 at/response-error-body}}]
     ["/by-share-hash" {:get get-activation
                        :description (at/get-doc #'get-activation)
                        :name :activation/get
                        :parameters {:query {:share-hash :discussion/share-hash}}
                        :responses {200 {:body {:activation  (s/or :activation ::specs/activation
                                                                   :no-activation nil?)}}
                                    400 at/response-error-body}}]
     ["/increment" {:put increment-activation-counter
                    :description (at/get-doc #'increment-activation-counter)
                    :name :activation/increment
                    :parameters {:body {:share-hash :discussion/share-hash}}
                    :responses {200 {:body {:activation ::specs/activation}}
                                400 at/response-error-body}}]
     ["/reset" {:put reset-activation
                :description (at/get-doc #'reset-activation)
                :middleware [:user/authenticated?
                             :user/beta-tester?
                             :discussion/valid-credentials?]
                :name :activation/reset
                :parameters {:body {:share-hash :discussion/share-hash
                                    :edit-hash :discussion/edit-hash}}
                :responses {200 {:body {:activation ::specs/activation}}
                            400 at/response-error-body}}]]]])
