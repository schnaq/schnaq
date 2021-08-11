(ns schnaq.interface.views.discussion.card-view
  (:require [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.views.discussion.card-elements :as elements]
            [schnaq.interface.views.discussion.input :as input]
            [schnaq.interface.views.pages :as pages]))

(rf/reg-event-fx
  :discussion.statements/search
  (fn [{:keys [db]} [_ search-string]]
    (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
      {:db (assoc-in db [:search :schnaq :current :search-string] search-string)
       :fx [(http/xhrio-request db :get "/discussion/statements/search" [:discussion.statements.search/success]
                                {:share-hash share-hash
                                 :search-string search-string})
            [:dispatch [:navigation/navigate :routes.search/schnaq {:share-hash share-hash}]]]})))

(rf/reg-event-db
  :discussion.statements.search/success
  (fn [db [_ {:keys [matching-statements]}]]
    (assoc-in db [:search :schnaq :current :result] matching-statements)))

(defn- discussion-start-view
  "The first step after starting a discussion."
  []
  (let [schnaq @(rf/subscribe [:schnaq/selected])
        {:discussion/keys [title author created-at]} schnaq
        current-starting @(rf/subscribe [:discussion.conclusions/starting])
        input-form [input/input-form "statement-text"]
        content {:statement/content title :statement/author author :statement/created-at created-at}]
    [elements/discussion-view
     schnaq content input-form nil current-starting]))

(defn- selected-conclusion-view
  "The first step after starting a discussion."
  []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])
        current-premises @(rf/subscribe [:discussion.premises/current])
        history @(rf/subscribe [:discussion-history])
        current-conclusion (last history)
        info-content [elements/info-content-conclusion
                      current-conclusion (:discussion/edit-hash current-discussion)]
        input-form [input/input-form "premise-text"]]
    [elements/discussion-view
     current-discussion current-conclusion input-form info-content current-premises]))

(rf/reg-sub
  :discussion.premises/current
  (fn [db _]
    (get-in db [:discussion :premises :current] [])))

(rf/reg-sub
  :discussion.conclusions/starting
  (fn [db _]
    (get-in db [:discussion :conclusions :starting] [])))

;; -----------------------------------------------------------------------------

(defn derive-view []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])
        current-route-name @(rf/subscribe [:navigation/current-route-name])
        wrapping-view (if shared-config/embedded? pages/embeddable-view pages/with-discussion-header)]
    [wrapping-view
     {:page/heading (:discussion/title current-discussion)}
     (if (= :routes.schnaq/start current-route-name)
       [discussion-start-view]
       [selected-conclusion-view])]))

(defn view []
  [derive-view])