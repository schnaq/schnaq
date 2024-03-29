(ns schnaq.api.activation
  (:require [clojure.spec.alpha :as s]
            [ring.util.http-response :refer [bad-request ok]]
            [schnaq.api.toolbelt :as at]
            [schnaq.database.activation :as activation-db]
            [schnaq.database.main :as db :refer [set-activation-focus]]
            [schnaq.database.specs :as specs]
            [taoensso.timbre :as log]))

(defn- start-activation
  "Start activation-feature for a discussion. Only creates a new one if none
  exists."
  [{{{:keys [share-hash]} :body} :parameters}]
  (log/info "Starting activation for" share-hash)
  (let [started-activation (activation-db/start-activation! share-hash)]
    (set-activation-focus [:discussion/share-hash share-hash] (:db/id started-activation))
    (ok {:activation started-activation})))

(defn- delete-activation
  "Delete an activation for a discussion."
  [{{{:keys [share-hash]} :body} :parameters}]
  (log/info "Deleting activation for" share-hash)
  (if-let [activation (activation-db/activation-by-share-hash share-hash)]
    (do (db/delete-entity! (:db/id activation))
        (ok {:deleted? true}))
    (bad-request (at/build-error-body :activation-not-found "No activation found to delete!"))))

(defn get-activation
  "Get the current activation for a discussion."
  [{{{:keys [share-hash]} :query} :parameters}]
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
                       :user/pro?
                       :discussion/user-moderator?]
          :name :activation/start
          :parameters {:body {:share-hash :discussion/share-hash}}
          :responses {200 {:body {:activation ::specs/activation}}
                      403 at/response-error-body}}]
     ["/delete" {:delete delete-activation
                 :description (at/get-doc #'delete-activation)
                 :middleware [:user/pro?
                              :discussion/user-moderator?]
                 :name :activation/delete
                 :parameters {:body {:share-hash :discussion/share-hash}}
                 :responses {200 {:body {:deleted? boolean?}}
                             400 at/response-error-body}}]
     ["/by-share-hash" {:get get-activation
                        :description (at/get-doc #'get-activation)
                        :name :activation/get
                        :parameters {:query {:share-hash :discussion/share-hash}}
                        :responses {200 {:body {:activation (s/or :activation ::specs/activation
                                                                  :no-activation nil?)}}
                                    400 at/response-error-body}}]
     ["/increment" {:put increment-activation-counter
                    :description (at/get-doc #'increment-activation-counter)
                    :name :activation/increment
                    :middleware [:discussion/valid-writeable-discussion?]
                    :parameters {:body {:share-hash :discussion/share-hash}}
                    :responses {200 {:body {:activation ::specs/activation}}
                                400 at/response-error-body}}]
     ["/reset" {:put reset-activation
                :description (at/get-doc #'reset-activation)
                :middleware [:user/pro?
                             :discussion/user-moderator?]
                :name :activation/reset
                :parameters {:body {:share-hash :discussion/share-hash}}
                :responses {200 {:body {:activation ::specs/activation}}
                            400 at/response-error-body}}]]]])
