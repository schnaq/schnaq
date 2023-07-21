(ns schnaq.interface.views.schnaq.activation-cards
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.utils.localstorage :as localstorage]
            [schnaq.interface.views.schnaq.activation :as activation]
            [schnaq.interface.views.schnaq.feedback-card :as feedback-card]
            [schnaq.interface.views.schnaq.poll :as poll]
            [schnaq.interface.views.schnaq.qa-box :as qa-box]
            [schnaq.interface.views.schnaq.wordcloud-card :as wordcloud-card]))

(rf/reg-sub
 :schnaq/activation-focus
 (fn [db _]
   (get-in db [:schnaq :selected :discussion/activation-focus])))

(rf/reg-sub
 :schnaq.activations/show-index
 ;; The index of the current activation that is shown
 (fn [db _]
   (get-in db [:schnaq :activations :show-index] 0)))

(rf/reg-event-db
 :schnaq.activations.show-index/update
 (fn [db [_ update-fn]]
   (update-in db [:schnaq :activations :show-index] update-fn)))

(defn- activation-card-controls
  "The controls allowing the user to switch between activation cards."
  [activations-count active-index]
  [motion/fade-in-and-out
   [:div.d-flex.justify-content-between.panel-white-sm
    [:button.btn.btn-transparent.ms-1
     {:on-click #(rf/dispatch [:schnaq.activations.show-index/update (fnil dec 0)])}
     [icon :chevron/left]]
    [:div.d-flex.align-items-center
     (for [index (range activations-count)
           :let [default-classes "tiny me-1"]]
       (with-meta
         [icon :circle (if (= index active-index) (str default-classes " text-primary") default-classes)]
         {:key (str "index-activation-" index)}))]
    [:button.btn.btn-transparent.me-1
     {:on-click #(rf/dispatch [:schnaq.activations.show-index/update (fnil inc 0)])}
     [icon :chevron/right]]]])

(rf/reg-sub
 :schnaq/activations?
 :<- [:routes.schnaq/start?]
 :<- [:schnaq/activation]
 :<- [:schnaq.wordcloud/show?]
 :<- [:schnaq/polls]
 :<- [:schnaq.wordclouds/local]
 :<- [:schnaq.feedback/exists-for-user?]
 (fn [[start? activation wordcloud? polls local-wordclouds feedback-exists?] _]
   (and start?
        (or wordcloud? (seq polls) (seq local-wordclouds) activation feedback-exists?))))

(defn activation-cards
  "A single card containing all activations, which can be switched through."
  []
  (let [show-index @(rf/subscribe [:schnaq.activations/show-index])
        top-level? @(rf/subscribe [:routes.schnaq/start?])
        activation-focus @(rf/subscribe [:schnaq/activation-focus])
        focus-poll @(rf/subscribe [:schnaq/poll activation-focus])
        focus-qa-box @(rf/subscribe [:qa-box activation-focus])
        edit-polls @(rf/subscribe [:schnaq.polls.edit/actives])
        activation @(rf/subscribe [:schnaq/activation])
        focus-activation? (and activation-focus (= activation-focus (:db/id activation)))
        focus-wordcloud? @(rf/subscribe [:schnaq.wordcloud/focus?])
        polls (poll/poll-list (:db/id focus-poll))
        qa-boxes @(rf/subscribe [:qa-boxes])
        wordcloud? @(rf/subscribe [:schnaq.wordcloud/show?])
        focus-local-wordcloud @(rf/subscribe [:schnaq.wordcloud/local activation-focus])
        local-wordclouds (wordcloud-card/wordcloud-list focus-local-wordcloud)
        user-moderator? @(rf/subscribe [:user/moderator?])
        current-feedback @(rf/subscribe [:feedback/current])
        user-participated-in-feedback? (contains? (localstorage/from-localstorage :discussion/feedbacks) (:db/id current-feedback))
        ;; Admins always see the feedback-form. The rest only if they have not filled it our yet.
        feedback-form (when (or user-moderator?
                                (and (not user-participated-in-feedback?) (:feedback/visible current-feedback)))
                        current-feedback)
        focus-feedback? (and activation-focus (= activation-focus (:db/id feedback-form)))
        activations-seq (cond-> []
                          focus-poll (conj (if (edit-polls (:db/id focus-poll))
                                             [poll/poll-edit-card (:db/id focus-poll)]
                                             [poll/poll-list-item focus-poll]))
                          focus-qa-box (conj [qa-box/qa-box-card focus-qa-box])
                          focus-local-wordcloud (conj [wordcloud-card/local-wordcloud-card focus-local-wordcloud])
                          focus-activation? (conj [activation/activation-card])
                          (and wordcloud? focus-wordcloud?) (conj [wordcloud-card/wordcloud-card])
                          focus-feedback? (conj [feedback-card/feedback-card])
                          ;; Add non focused elements in order
                          (seq local-wordclouds) ((comp vec concat) local-wordclouds)
                          (seq polls) ((comp vec concat) polls)
                          (seq qa-boxes) ((comp vec concat)
                                          (for [question-box-data (remove #(= (:db/id focus-qa-box) (:db/id %)) qa-boxes)]
                                            [:section
                                             {:key (str "qa-box-" (:db/id question-box-data))}
                                             [qa-box/qa-box-card question-box-data]]))
                          (and (not focus-activation?) activation) (conj [activation/activation-card])
                          (and wordcloud? (not focus-wordcloud?)) (conj [wordcloud-card/wordcloud-card])
                          (and feedback-form (not focus-feedback?)) (conj [feedback-card/feedback-card]))
        activations-count (count activations-seq)
        active-index (mod show-index activations-count)]
    (when (and top-level? (seq activations-seq))
      [:<>
       (nth activations-seq active-index)
       (when (< 1 activations-count)
         [activation-card-controls activations-count active-index])])))
