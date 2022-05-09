(ns schnaq.interface.views.schnaq.activation-cards
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.views.schnaq.activation :as activation]
            [schnaq.interface.views.schnaq.poll :as poll]
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
  [:div.d-flex.justify-content-between
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
    [icon :chevron/right]]])

(rf/reg-sub
 :schnaq/activations?
 :<- [:routes.schnaq/start?]
 :<- [:schnaq/activation]
 :<- [:schnaq.wordcloud/show?]
 :<- [:schnaq/polls]
 (fn [[start? activation wordcloud? polls] _]
   (and start?
        (or wordcloud? (seq polls) activation))))

(defn activation-cards
  "A single card containing all activations, which can be switched through."
  []
  (let [show-index @(rf/subscribe [:schnaq.activations/show-index])
        top-level? @(rf/subscribe [:routes.schnaq/start?])
        activation-focus @(rf/subscribe [:schnaq/activation-focus])
        focus-poll @(rf/subscribe [:schnaq/poll activation-focus])
        activation @(rf/subscribe [:schnaq/activation])
        focus-activation? (and activation-focus (= activation-focus (:db/id activation)))
        focus-wordcloud? @(rf/subscribe [:schnaq.activations/focus-wordcloud?])
        polls (poll/poll-list (:db/id focus-poll))
        wordcloud? @(rf/subscribe [:schnaq.wordcloud/show?])
        activations-seq (cond-> []
                          focus-poll (conj [poll/poll-list-item focus-poll])
                          focus-activation? (conj [activation/activation-card])
                          (and wordcloud? focus-wordcloud?) (conj [wordcloud-card/wordcloud-card])
                          ;; Add non focused elements in order
                          (seq polls) ((comp vec concat) polls)
                          (and (not focus-activation?) activation) (conj [activation/activation-card])
                          (and wordcloud? (not focus-wordcloud?)) (conj [wordcloud-card/wordcloud-card]))
        activations-count (count activations-seq)
        active-index (mod show-index activations-count)]
    (when (and top-level? (seq activations-seq))
      [:<>
       (nth activations-seq active-index)
       (when (< 1 activations-count)
         [activation-card-controls activations-count active-index])])))
