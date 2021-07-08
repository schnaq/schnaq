(ns schnaq.interface.views.discussion.card-view
  (:require [re-frame.core :as rf]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.discussion.card-elements :as elements]
            [schnaq.interface.views.discussion.input :as input]
            [schnaq.interface.views.pages :as pages]))

(rf/reg-event-fx
  :schnaq/search
  (fn [{:keys [db]} [_ search-string]]
    (let [share-hash (get-in db [:current-route :path-params :share-hash])]
      {:db (assoc-in db [:search :schnaq :current :search-string] search-string)
       :fx [(http/xhrio-request db :get "/schnaq/search" [:schnaq.search/success]
                                {:share-hash share-hash
                                 :search-string search-string})
            [:dispatch [:navigation/navigate :routes.search/schnaq {:share-hash share-hash}]]]})))

(rf/reg-event-db
  :schnaq.search/success
  (fn [db [_ {:keys [matching-statements]}]]
    (assoc-in db [:search :schnaq :current :result] matching-statements)))

(defn- discussion-start-view
  "The first step after starting a discussion."
  []
  (let [schnaq @(rf/subscribe [:schnaq/selected])
        {:discussion/keys [title author created-at]} schnaq
        current-starting @(rf/subscribe [:discussion.conclusions/starting])
        input-form [input/input-form "statement-text"]
        content {:statement/content title :statement/author author :statement/created-at created-at}
        badges [badges/static-info-badges schnaq]]
    [toolbelt/desktop-mobile-switch
     [elements/discussion-view-desktop
      schnaq content input-form badges nil current-starting nil]
     [elements/discussion-view-mobile
      schnaq content input-form badges nil current-starting nil]]))

(defn- selected-conclusion-view
  "The first step after starting a discussion."
  []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])
        current-premises @(rf/subscribe [:discussion.premises/current])
        history @(rf/subscribe [:discussion-history])
        current-conclusion (last history)
        info-content [elements/info-content-conclusion
                      current-conclusion (:discussion/edit-hash current-discussion)]
        badges [badges/extra-discussion-info-badges
                current-conclusion (:discussion/edit-hash current-discussion)]
        input-form [input/input-form "premise-text"]]
    [toolbelt/desktop-mobile-switch
     [elements/discussion-view-desktop
      current-discussion current-conclusion input-form badges info-content current-premises history]
     [elements/discussion-view-mobile
      current-discussion current-conclusion input-form badges info-content current-premises history]]))

(rf/reg-sub
  :discussion.premises/current
  (fn [db _]
    (get-in db [:discussion :premises :current] [])))

(rf/reg-sub
  :discussion.conclusions/starting
  (fn [db _]
    (get-in db [:discussion :conclusions :starting] [])))

;; -----------------------------------------------------------------------------

(defn- derive-view []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])
        current-route-name @(rf/subscribe [:navigation/current-route-name])]
    [pages/with-discussion-header
     {:page/heading (:discussion/title current-discussion)}
     (if (= :routes.schnaq/start current-route-name)
       [discussion-start-view]
       [selected-conclusion-view])]))

(defn view []
  [derive-view])