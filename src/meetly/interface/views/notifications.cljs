(ns meetly.interface.views.notifications
  "Prints notifications on the screen. Usage:
  `(rf/dispatch
     [:notification/add
       #:notification{:title \"Hello, World!\"
                      :body \"I am a toast\"
                      :context :primary
                      :autohide? false}])`"
  (:require [cljs.spec.alpha :as s]
            [clojure.string :as string]
            [ghostwheel.core :refer [>defn-]]
            [goog.string :as gstring]
            [meetly.interface.text.display-data :refer [fa]]
            [reagent.dom]
            [re-frame.core :as rf]
            ["framer-motion" :refer [motion AnimatePresence]]))

(def ^:private display-time
  "Milliseconds, that a notification stays visible."
  5000)

(defn- toast-classes
  "Specify toast classes, depending on the context it is being used."
  [context]
  (let [common-classes "show toast"]
    (if context
      (gstring/format "%s toast-%s" common-classes (str (name context)))
      common-classes)))

(>defn- toast
  "Adds a toast to the screen. Has a title and a body, id is randomly generated.
   The context uses the same classes as it is known from bootstrap (e.g. primary,
   secondary, ...)."
  [{:notification/keys [title body id context]}]
  [::notification :ret associative?]
  [:> (.-div motion)
   {:initial {:opacity 0}
    :animate {:opacity 1}
    :exit {:opacity 0}}
   [:div
    {:class-name (toast-classes context)
     :aria-atomic "true", :aria-live "assertive", :role "alert"}
    [:div.toast-header
     [:strong.mr-auto title]
     [:button.close {:type "button"
                     :on-click #(rf/dispatch [:notification/remove id])}
      [:span {:aria-hidden "true"}
       [:i {:class (str " m-auto fas fa-xs " (fa :delete-icon))}]]]]
    [:div.toast-body body]]])

(defn view
  "Presenting all notifications to the user."
  []
  (let [notifications @(rf/subscribe [:notifications/all])]
    [:div#notifications-wrapper
     {:aria-live "polite"
      :aria-atomic true}
     [:> AnimatePresence
      (for [notification notifications]
        [:div {:key (:notification/id notification)}
         [toast notification]])]]))


;; -----------------------------------------------------------------------------

(s/def ::non-blank-string (s/and string? (complement string/blank?)))

(s/def :notification/title ::non-blank-string)
(s/def :notification/body ::non-blank-string)
(s/def :notification/id string?)
(s/def :notification/context #{:primary :secondary :success :danger :warning :info})
(s/def ::notification
  (s/keys :req [:notification/title :notification/body]
          :opt [:notification/id :notification/context]))


;; -----------------------------------------------------------------------------

(rf/reg-event-fx
  :notification/add
  (fn [{:keys [db]} [_ notification]]
    (let [notification-id (str (random-uuid))
          notification' (assoc notification :notification/id notification-id)]
      {:db (update db :notifications conj notification')
       :notification/timed-remove notification-id})))

(rf/reg-fx
  :notification/timed-remove
  (fn [notification-id]
    (js/setTimeout #(rf/dispatch [:notification/remove notification-id])
                   display-time)))

(rf/reg-event-db
  :notification/remove
  (fn [db [_ notification-id]]
    (when-let [notifications (:notifications db)]
      (assoc db :notifications
                (remove (fn [{:notification/keys [id]}]
                          (= id notification-id))
                        notifications)))))

(rf/reg-event-db
  :notifications/reset
  (fn [db [_]]
    (assoc db :notifications [])))

(rf/reg-sub
  :notifications/all
  (fn [db]
    (get db :notifications)))