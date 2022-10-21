(ns schnaq.interface.views.schnaq.reactions
  (:require [com.fulcrologic.guardrails.core :refer [>defn >defn- ?]]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [schnaq.interface.auth :as auth]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.matomo :as matomo]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.localstorage :refer [from-localstorage]]))

(>defn calculate-votes
  "Calculates the votes without needing to reload."
  [statement local-votes]
  [map? map? :ret number?]
  (let [up-vote-change (get-in local-votes [:up (:db/id statement)] 0)
        down-vote-change (get-in local-votes [:down (:db/id statement)] 0)]
    (-
     (+ (:statement/upvotes statement) up-vote-change)
     (+ (:statement/downvotes statement) down-vote-change))))

(>defn- get-up-votes
  "Calculates the up-votes without needing to reload."
  [statement local-votes]
  [(? map?) (? map?) :ret number?]
  (let [up-vote-change (get-in local-votes [:up (:db/id statement)] 0)]
    (+ (:statement/upvotes statement) up-vote-change)))

(>defn- get-down-votes
  "Calculates the down-votes without needing to reload."
  [statement local-votes]
  [(? map?) (? map?) :ret number?]
  (let [down-vote-change (get-in local-votes [:down (:db/id statement)] 0)]
    (+ (:statement/downvotes statement) down-vote-change)))

(defn up-down-vote
  "Add inline panel for up and down votes."
  [statement]
  (let [votes @(rf/subscribe [:local-votes])
        [local-upvote? local-downvote?] @(rf/subscribe [:votes/upvoted-or-downvoted (:db/id statement)])
        ;; Do not use or shortcut, since the value can be false and should be preferably selected over backend value
        upvoted? (if (nil? local-upvote?) (:meta/upvoted? statement) local-upvote?)
        downvoted? (if (nil? local-downvote?) (:meta/downvoted? statement) local-downvote?)
        authenticated? @(rf/subscribe [:user/authenticated?])
        read-only? @(rf/subscribe [:schnaq.state/read-only?])]
    [:div.d-flex.flex-row.align-items-center
     [:div.me-1
      (cond->
       {:class (if upvoted? "badge badge-upvote-selected" "badge badge-upvote")}
        (not read-only?) (merge {:on-click (fn [e]
                                             (.stopPropagation e)
                                             (if authenticated?
                                               (rf/dispatch [:discussion/toggle-upvote statement])
                                               (rf/dispatch [:schnaq.vote/toggle-anonymous statement :upvote]))
                                             (matomo/track-event "Active User", "Action", "Vote: Upvote"))}))
      [icon :arrow-up "vote-arrow m-auto" (when read-only? {:style {:cursor "unset"}})]]
     [:span.me-2 (get-up-votes statement votes)]
     [:div.me-1
      (cond->
       {:class (if downvoted? "badge badge-downvote-selected" "badge badge-downvote")}
        (not read-only?) (merge {:on-click (fn [e]
                                             (.stopPropagation e)
                                             (if authenticated?
                                               (rf/dispatch [:discussion/toggle-downvote statement])
                                               (rf/dispatch [:schnaq.vote/toggle-anonymous statement :downvote]))
                                             (matomo/track-event "Active User", "Action", "Vote: Downvote"))}))
      [icon :arrow-down "vote-arrow m-auto" (when read-only? {:style {:cursor "unset"}})]]
     [:span (get-down-votes statement votes)]]))

(defn up-down-vote-vertical
  "Vertical panel for up and down votes."
  [props statement]
  (let [votes @(rf/subscribe [:local-votes])
        [local-upvote? local-downvote?] @(rf/subscribe [:votes/upvoted-or-downvoted (:db/id statement)])
        ;; Do not use or shortcut, since the value can be false and should be preferably selected over backend value
        upvoted? (if (nil? local-upvote?) (:meta/upvoted? statement) local-upvote?)
        downvoted? (if (nil? local-downvote?) (:meta/downvoted? statement) local-downvote?)
        authenticated? @(rf/subscribe [:user/authenticated?])
        read-only? @(rf/subscribe [:schnaq.state/read-only?])]
    [:div props
     [:div.d-flex.flex-row
      [:div
       (cond->
        {:class (if upvoted?
                  "badge badge-upvote-selected px-1 me-1"
                  "badge badge-upvote px-1 me-1")}
         (not read-only?) (merge {:on-click (fn [e]
                                              (.stopPropagation e)
                                              (if authenticated?
                                                (rf/dispatch [:discussion/toggle-upvote statement])
                                                (rf/dispatch [:schnaq.vote/toggle-anonymous statement :upvote]))
                                              (matomo/track-event "Active User", "Action", "Vote: Upvote"))}))
       [icon :arrow-up "vote-arrow m-auto" (when read-only? {:style {:cursor "unset"}})]]
      [:div (get-up-votes statement votes)]]
     [:div.d-flex.flex-row
      [:div
       (cond->
        {:class (if downvoted?
                  "badge badge-downvote-selected px-1 me-1"
                  "badge badge-downvote px-1 me-1")}
         (not read-only?) (merge {:on-click (fn [e]
                                              (.stopPropagation e)
                                              (if authenticated?
                                                (rf/dispatch [:discussion/toggle-downvote statement])
                                                (rf/dispatch [:schnaq.vote/toggle-anonymous statement :downvote]))
                                              (matomo/track-event "Active User", "Action", "Vote: Downvote"))}))
       [icon :arrow-down "vote-arrow m-auto" (when read-only? {:style {:cursor "unset"}})]]
      [:div (get-down-votes statement votes)]]]))

(rf/reg-sub
 :local-votes
 :-> :votes)

(rf/reg-event-fx
 :discussion/toggle-upvote
 (fn [{:keys [db]} [_ {:keys [db/id meta/upvoted?] :as statement}]]
   {:db (-> db
            (update-in [:votes :own :up id] #(not (if (nil? %) upvoted? %)))
            (assoc-in [:votes :own :down id] false))
    :fx [(http/xhrio-request db :post "/discussion/statement/vote/up" [:upvote-success statement]
                             {:statement-id id
                              :share-hash (-> db :schnaq :selected :discussion/share-hash)
                              :inc-or-dec nil}
                             [:ajax.error/as-notification])]}))

(rf/reg-event-fx
 :discussion/toggle-downvote
 (fn [{:keys [db]} [_ {:keys [db/id meta/downvoted?] :as statement}]]
   {:db (-> db
            (assoc-in [:votes :own :up id] false)
            (update-in [:votes :own :down id] #(not (if (nil? %) downvoted? %))))
    :fx [(http/xhrio-request db :post "/discussion/statement/vote/down" [:downvote-success statement]
                             {:statement-id id
                              :share-hash (-> db :schnaq :selected :discussion/share-hash)
                              :inc-or-dec nil}
                             [:ajax.error/as-notification])]}))

(rf/reg-event-fx
 :schnaq.vote/toggle-anonymous
 (fn [{:keys [db]} [_ statement current-click]]
   (let [recorded-vote (get-in db [:votes :device (:db/id statement)])
         effects
         (cond-> []
           (and (= current-click :upvote) (not= :upvote recorded-vote))
           (conj [:dispatch [:schnaq.vote/send-anonymous statement :up :inc]])
           (= :upvote recorded-vote)
           (conj [:dispatch [:schnaq.vote/send-anonymous statement :up :dec]])
           (and (= current-click :downvote) (not= :downvote recorded-vote))
           (conj [:dispatch [:schnaq.vote/send-anonymous statement :down :inc]])
           (= :downvote recorded-vote)
           (conj [:dispatch [:schnaq.vote/send-anonymous statement :down :dec]]))
         new-vote (case recorded-vote
                    nil current-click
                    :downvote (when (= :upvote current-click) :upvote)
                    :upvote (when (= :downvote current-click) :downvote))]
     {:db (if new-vote
            (assoc-in db [:votes :device (:db/id statement)] new-vote)
            (update-in db [:votes :device] dissoc (:db/id statement)))
      :fx (vec effects)})))

(rf/reg-event-fx
 :schnaq.vote/send-anonymous
 (fn [{:keys [db]} [_ statement up-or-down inc-or-dec]]
   {:fx [(http/xhrio-request db :post (gstring/format "/discussion/statement/vote/%s" (name up-or-down))
                             [:anonymous-vote-success statement up-or-down inc-or-dec]
                             {:statement-id (:db/id statement)
                              :share-hash (-> db :schnaq :selected :discussion/share-hash)
                              :inc-or-dec inc-or-dec}
                             [:ajax.error/as-notification])]}))

(rf/reg-event-fx
 :anonymous-vote-success
 (fn [{:keys [db]} [_ {:keys [db/id]} up-or-down inc-or-dec _response]]
   {:db (if (= :up up-or-down)
          (if (= :inc inc-or-dec)
            (update-in db [:votes :up id] inc)
            (update-in db [:votes :up id] dec))
          (if (= :inc inc-or-dec)
            (update-in db [:votes :down id] inc)
            (update-in db [:votes :down id] dec)))
    :fx [[:localstorage/assoc [:device.reactions/votes (get-in db [:votes :device])]]]}))

(rf/reg-event-fx
 :schnaq.votes/load-from-localstorage
 (fn [{:keys [db]} _]
   (when-let [votes (from-localstorage :device.reactions/votes)]
     {:db (assoc-in db [:votes :device] votes)})))

(rf/reg-event-db
 :upvote-success
 (fn [db [_ {:keys [db/id]} {:keys [operation]}]]
   (case operation
     :added (update-in db [:votes :up id] inc)
     :removed (update-in db [:votes :up id] dec)
     :switched (-> db
                   (update-in [:votes :up id] inc)
                   (update-in [:votes :down id] dec)))))

(rf/reg-event-db
 :downvote-success
 (fn [db [_ {:keys [db/id]} {:keys [operation]}]]
   (case operation
     :added (update-in db [:votes :down id] inc)
     :removed (update-in db [:votes :down id] dec)
     :switched (-> db
                   (update-in [:votes :down id] inc)
                   (update-in [:votes :up id] dec)))))

(rf/reg-sub
 :votes/upvoted-or-downvoted
 (fn [db [_ statement-id]]
   (if (auth/user-authenticated? db)
     [(get-in db [:votes :own :up statement-id])
      (get-in db [:votes :own :down statement-id])]
     [(= :upvote (get-in db [:votes :device statement-id]))
      (= :downvote (get-in db [:votes :device statement-id]))])))

(rf/reg-event-db
 :votes.local/reset
 (fn [db _]
   ;; Do not delete completely. Device votes need to stay!
   (update db :votes
           dissoc :up
           dissoc :down
           dissoc :own)))
