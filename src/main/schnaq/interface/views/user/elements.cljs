(ns schnaq.interface.views.user.elements
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [fa labels]]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.common :as common]))

(defn about-button [label href-link]
  [:div.btn-block
   [:a.btn.btn-outline-primary.rounded-2.w-100 {:href href-link}
    (labels label)]])

(defn extra-information []
  [:div.feed-extra-info.text-right
   [:div.btn-group-vertical
    [about-button :router/privacy (reitfe/href :routes/privacy)]
    [about-button :coc/heading (reitfe/href :routes/code-of-conduct)]
    [about-button :how-to/button (reitfe/href :routes/how-to)]]])

(defn- current-user [user]
  (let [user-name (get-in user [:names :display])]
    [:<>
     [:h6.text-gray-600.mb-4 (labels :user.settings)]
     [:div.pl-4
      [common/avatar-with-nickname-right user-name 40]]]))

(defn- edit-user-navigation-button [label icon route focused?]
  (let [button-class (if focused? "feed-button-focused" "feed-button")]
    [:div
     [:button
      {:class button-class :type "button"
       :on-click #(rf/dispatch [:navigation/navigate route])}
      [:div
       [:i.mr-4 {:class (str "fas " (fa icon))}]
       [:span (labels label)]]]]))


(defn- user-navigation []
  (let [current-page @(rf/subscribe [:account-manage/get-current-page])
        manage-account? (= current-page :manage-account)
        manage-hubs? (= current-page :manage-hubs)]
    [:<>
     [edit-user-navigation-button :user.settings/info :user/edit :routes/user-manage-account manage-account?]
     [edit-user-navigation-button :user.settings/hubs :user/group-edit :routes/user-manage-hubs manage-hubs?]]))

(defn- user-panel [user]
  [:div
   [current-user user]
   [:hr.my-4]
   [user-navigation]])

(defn- user-view-desktop [user content]
  [:div.container-fluid
   [:div.row.px-0.mx-0
    [:div.col-3.py-4.px-5
     [user-panel user]]
    [:div.col-6.py-4
     content]
    [:div.col-3.py-4
     [extra-information]]]])

(defn user-view [user content]
  [toolbelt/desktop-mobile-switch
   [user-view-desktop user content]
   content])

;; events

(rf/reg-event-db
  :account-manage/store-current-page
  (fn [db [_ manage-page]]
    ;; store either :manage-account or :manage-hubs feed
    (assoc-in db [:account-manage :current] manage-page)))

(rf/reg-sub
  :account-manage/get-current-page
  (fn [db _]
    (get-in db [:account-manage :current])))