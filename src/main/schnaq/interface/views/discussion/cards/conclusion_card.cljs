(ns schnaq.interface.views.discussion.cards.conclusion-card
  (:require [re-frame.core :as rf]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.text.display-data :refer [fa]]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.discussion.view-elements :as view-elements]
            [schnaq.interface.views.discussion.logic :as logic]))

(defn up-down-vote-breaking
  "Add panel for up and down votes."
  [statement]
  (let [votes @(rf/subscribe [:local-votes])]
    [:<>
     [:div.d-flex
      [:div.vote-box.up-vote
       {:on-click (fn [e]
                    (js-wrap/stop-propagation e)
                    (rf/dispatch [:discussion/toggle-upvote statement]))}
       [:h6 [:i {:class (str "m-auto fas fa-lg " (fa :arrow-up))}]]]
      [:h6.ml-1.pt-1 (logic/calculate-votes statement :upvotes votes)]]
     [:div.d-flex
      [:div.vote-box.down-vote.align-bottom
       {:on-click (fn [e]
                    (js-wrap/stop-propagation e)
                    (rf/dispatch [:discussion/toggle-downvote statement]))}
       [:h6 [:i {:class (str "m-auto fas fa-lg " (fa :arrow-down))}]]]
      [:h6.ml-1.pt-1 (logic/calculate-votes statement :downvotes votes)]]]))

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

(defn- user-info
  "User info box containing relevant information for discussions."
  [username avatar-size]
  [:div.p-1 {:style {:width "100px"}}
   [:div.d-flex.flex-row.align-items-center
    [:div [common/avatar username avatar-size]]
    [:div.pl-2.pb-1 {:style {:line-height ".9rem"}}
     [:small username]]]])

(defn- statement-card
  [edit-hash {:keys [statement/content] :as statement} attitude]
  [:div.card.card-rounded.clickable.shadow-straight-light
   {:class (str "statement-card-" (name attitude))}
   [:div.card-view.card-body.py-0.pb-1
    [:div.d-flex.pt-3
     [:div.mr-auto [:p content]]
     [:div.pb-2 [up-down-vote-breaking statement]]]
    [:div.d-flex
     [:div.mr-auto [view-elements/extra-discussion-info-badges statement edit-hash]]
     [:div.float-right [user-info (-> statement :statement/author :author/nickname) 32]]]]])

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
        [statement-card edit-hash conclusion (logic/arg-type->attitude (:meta/argument-type conclusion))]])]))