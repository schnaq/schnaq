(ns schnaq.interface.views.discussion.conclusion-card
  (:require [re-frame.core :as rf]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.text.display-data :refer [#_labels fa #_img-path]]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.discussion.view-elements :as view-elements]
            [schnaq.interface.views.discussion.logic :as logic]))


(defn- up-down-vote
  "Add panel for up and down votes."
  [statement]
  (let [votes @(rf/subscribe [:local-votes])]
    [:div.d-flex.flex-row.float-right
     [:div.vote-box.up-vote.text-center
      {:on-click (fn [e]
                   (js-wrap/stop-propagation e)
                   (rf/dispatch [:discussion/toggle-upvote statement]))}
      [:h6 [:i {:class (str "m-auto fas fa-lg " (fa :arrow-up))}]]]
     [:h6.ml-1.mr-3.pt-1 (logic/calculate-votes statement :upvotes votes)]
     [:div.vote-box.down-vote.text-center.align-bottom
      {:on-click (fn [e]
                   (js-wrap/stop-propagation e)
                   (rf/dispatch [:discussion/toggle-downvote statement]))}
      [:h6 [:i {:class (str "m-auto fas fa-lg " (fa :arrow-down))}]]]
     [:h6.ml-1.pt-1 (logic/calculate-votes statement :downvotes votes)]]))

(defn- statement-card [edit-hash {:keys [statement/content] :as statement}]
  [:div.card.card-view-rounded.clickable.shadow-straight-light
   [:div.card-body.py-0
    [:div.row.pt-4
     [:div.col-10 [:p.my-0 content]]
     [:div.col-2 [common/avatar (-> statement :statement/author :author/nickname) 50]]]
    [:div.row.mb-2
     [:div.col-6 [view-elements/extra-discussion-info-badges statement edit-hash]]
     [:div.col-6 [up-down-vote statement]]]]])

(defn conclusion-cards-list
  "Displays a list of conclusions."
  [conclusions share-hash]
  (let [path-params (:path-params @(rf/subscribe [:navigation/current-route]))
        admin-access-map @(rf/subscribe [:meetings/load-admin-access])
        edit-hash (get admin-access-map share-hash)]
    [:div.card-columns.my-3.my-md-5
     (for [conclusion conclusions]
       [:div {:key (:db/id conclusion)
              :on-click (fn [_e]
                          (rf/dispatch [:discussion.select/conclusion conclusion])
                          (rf/dispatch [:discussion.history/push conclusion])
                          (rf/dispatch [:navigation/navigate :routes.discussion.select/statement
                                        (assoc path-params :statement-id (:db/id conclusion))]))}
        [statement-card edit-hash conclusion :neutral]])]))