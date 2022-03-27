(ns schnaq.interface.views.schnaq.wordcloud-card
  (:require [re-frame.core :as rf]
            [schnaq.export :as export]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.components.wordcloud :as wordcloud]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.schnaq.dropdown-menu :as dropdown-menu]))

(defn wordcloud-tab
  "Wordcloud tab menu to hide and show a wordcloud."
  []
  (let [display-wordcloud? @(rf/subscribe [:schnaq/show-wordcloud?])]
    [:div.pt-2
     [:div.text (labels :schnaq.wordcloud/label)]
     [:div.text-center.pt-2
      (if display-wordcloud?
        [:button.btn.btn-dark.w-75
         {:on-click #(rf/dispatch [:wordcloud/display? false])}
         (labels :schnaq.wordcloud/hide)]
        [:button.btn.btn-secondary.w-75
         {:on-click (fn [_e] (rf/dispatch [:wordcloud/display? true]))}
         (labels :schnaq.wordcloud/show)])]]))

(defn wordcloud-card
  "Displays a wordcloud in a card."
  []
  (when @(rf/subscribe [:schnaq/show-wordcloud?])
    [motion/fade-in-and-out
     [:div.statement-column
      [:section.statement-card
       [:div.d-flex.mt-3
        [:h4.mx-auto.mt-3
         (labels :schnaq.wordcloud/title)]
        [dropdown-menu/moderator
         "wordcloud-dropdown-id"
         [dropdown-menu/item :trash
          :schnaq.wordcloud/hide
          #(rf/dispatch [:wordcloud/display? false])]]]
       [wordcloud/wordcloud]]]]))

;; -----------------------------------------------------------------------------

(rf/reg-sub
 :schnaq/show-wordcloud?
 ;; Checks if the wordcloud shall be displayed.
 (fn [db _]
   (get-in db [:schnaq :current :display-wordcloud?] false)))

(rf/reg-event-fx
 :wordcloud/display?
 (fn [{:keys [db]} [_ display-wordcloud?]]
   {:fx [(http/xhrio-request db :put "/wordcloud/discussion"
                             [:schnaq.wordcloud.toggle/success]
                             {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])
                              :edit-hash (get-in db [:schnaq :selected :discussion/edit-hash])
                              :display-wordcloud? display-wordcloud?})]}))

(rf/reg-event-fx
 :schnaq.wordcloud.toggle/success
 (fn [{:keys [db]} [_ {:keys [display-wordcloud?]}]]
   {:db (assoc-in db [:schnaq :current :display-wordcloud?] display-wordcloud?)
    :fx [[:dispatch [:schnaq.wordcloud/calculate]]]}))

(rf/reg-event-fx
 :schnaq.wordcloud/calculate
 (fn [{:keys [db]} [_ _]]
   (when (get-in db [:schnaq :current :display-wordcloud?] false)
     {:fx [[:dispatch [:schnaq.wordcloud/for-selected-discussion]]]})))

(defn- show-wordcloud-for-selected?
  "Check in app db at selected schnaq whether to display a word cloud."
  [db]
  (some
   #(= % :discussion.visible.entities/wordcloud)
   (get-in db [:schnaq :selected :discussion.visible/entities])))

(rf/reg-event-fx
 :schnaq.wordcloud/for-selected-discussion
 (fn [{:keys [db]} [_ _]]
   {:db (assoc-in db [:schnaq :current :display-wordcloud?]
                  (show-wordcloud-for-selected? db))
    :fx [[:dispatch [:schnaq.wordcloud/from-current-premises]]]}))

(rf/reg-event-fx
 :schnaq.wordcloud/from-current-premises
 (fn [{:keys [db]}]
   (let [premises (tools/convert-premises db)
         premises-with-children (remove nil? (concat premises (flatten (map :statement/children premises))))]
     {:fx [[:dispatch [:wordcloud/store-words
                       {:string-representation (export/generate-fulltext premises-with-children)}]]]})))
