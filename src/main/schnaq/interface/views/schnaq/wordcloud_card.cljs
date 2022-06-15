(ns schnaq.interface.views.schnaq.wordcloud-card
  (:require [re-frame.core :as rf]
            [schnaq.export :as export]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.components.wordcloud :as wordcloud]
            [schnaq.interface.matomo :as matomo]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.views.schnaq.dropdown-menu :as dropdown-menu]
            [schnaq.shared-toolbelt :as stools]))

(defn wordcloud-tab
  "Wordcloud tab menu to hide and show a wordcloud."
  []
  (let [display-wordcloud? @(rf/subscribe [:schnaq.wordcloud/show?])]
    [:div.pt-2
     [:div.text (labels :schnaq.wordcloud/label)]
     [:div.text-center.pt-2
      (if display-wordcloud?
        [:button.btn.btn-dark.w-75
         {:on-click #(rf/dispatch [:schnaq.wordcloud/toggle false])}
         (labels :schnaq.wordcloud/hide)]
        [:button.btn.btn-secondary.w-75
         {:on-click (fn [_e]
                      (rf/dispatch [:schnaq.wordcloud/toggle true])
                      (matomo/track-event "Active User", "Action", "Create Wordcloud"))}
         (labels :schnaq.wordcloud/show)])]]))

(defn wordcloud-card
  "Displays a wordcloud in a card."
  []
  (when @(rf/subscribe [:schnaq.wordcloud/show?])
    [motion/fade-in-and-out
     [:section.statement-card
      [:div.d-flex.mt-3
       [:h4.mx-auto.mt-3
        (labels :schnaq.wordcloud/title)]
       [dropdown-menu/moderator
        {:id "wordcloud-dropdown-id"}
        [:<>
         [dropdown-menu/item :bullseye
          :schnaq.admin.focus/button
          (fn []
            (rf/dispatch [:schnaq.wordcloud/toggle true])
            (rf/dispatch [:schnaq.admin.focus.entity/success]))]
         [dropdown-menu/item :trash
          :schnaq.wordcloud/hide
          #(rf/dispatch [:schnaq.wordcloud/toggle false])]]]]
      [wordcloud/wordcloud]]]))

;; -----------------------------------------------------------------------------

(rf/reg-sub
 :schnaq.wordcloud/show?
 ;; Checks if the wordcloud shall be displayed.
 (fn [db _]
   (get-in db [:schnaq :selected :discussion/wordcloud :wordcloud/visible?] false)))

(rf/reg-event-fx
 :schnaq.wordcloud/toggle
 (fn [{:keys [db]} [_ display-wordcloud?]]
   {:fx [(http/xhrio-request db :put "/wordcloud/discussion"
                             [:schnaq.wordcloud.toggle/success]
                             {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])
                              :edit-hash (get-in db [:schnaq :selected :discussion/edit-hash])
                              :display-wordcloud? display-wordcloud?})]}))

(rf/reg-event-fx
 :schnaq.wordcloud.toggle/success
 (fn [{:keys [db]} [_ {:keys [display-wordcloud?]}]]
   {:db (assoc-in db [:schnaq :selected :discussion/wordcloud :wordcloud/visible?] display-wordcloud?)
    :fx [(when display-wordcloud? [:dispatch [:schnaq.wordcloud/from-current-premises]])]}))

(rf/reg-event-fx
 :schnaq.wordcloud/from-current-premises
 (fn [{:keys [db]}]
   (let [all-premises (get-in db [:schnaq :statements])
         current-premise-ids (get-in db [:schnaq :statement-slice :current-level])
         premises (stools/select-values all-premises current-premise-ids)
         children-ids (flatten (map :statement/children premises))
         premises-with-children (remove nil? (concat premises (stools/select-values all-premises children-ids)))
         locked-statements-removed (remove :statement/locked? premises-with-children)]
     {:fx [[:dispatch [:wordcloud/store-words
                       {:string-representation (export/generate-fulltext locked-statements-removed)}]]]})))

(rf/reg-sub
 :schnaq.wordcloud/focus?
 (fn [db _]
   (let [{:keys [db/id]} (get-in db [:schnaq :selected :discussion/wordcloud])
         focus-id (get-in db [:schnaq :selected :discussion/activation-focus])]
     (= id focus-id))))

(rf/reg-event-db
 :schnaq.wordcloud/from-backend
 (fn [db [_ {:keys [wordcloud]}]]
   (assoc-in db [:schnaq :selected :discussion/wordcloud] wordcloud)))
