(ns schnaq.interface.views.schnaq.feedback-card
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.translations :refer [labels]]))

(defn- feedback-entry [item-count]
  (let [radio-name (keyword (str "feedback-item-type-" item-count))
        id-text (str "radio-item-type-text-" item-count)
        id-scale-5 (str "radio-item-type-scale-five-" item-count)]
    [:div
     [:div.form-check.form-check-inline
      [:input.form-check-input
       {:id id-text
        :type "radio"
        :name radio-name
        :value :text
        :defaultChecked true}]
      [:label.form-check-label
       {:for (keyword id-text)} (labels :schnaq.feedback.create/text)]]
     [:div.form-check.form-check-inline
      [:input.form-check-input
       {:id id-scale-5
        :type "radio"
        :name radio-name
        :value :scale-5}]
      [:label.form-check-label
       {:for (keyword id-scale-5)} (labels :schnaq.feedback.create/rating)]]]))

(defn- feedback-tab-entries []
  (let [feedback-count @(rf/subscribe [:feedback.create/item-count])]
    [:<>
     (for [item-count (range 0 feedback-count)]
       (with-meta
         [feedback-entry item-count]
         {:key (str "feedback-entry-key-" item-count)}))]))

(defn- feedback-tab-add-remove-entry-buttons []
  (let [feedback-count @(rf/subscribe [:feedback.create/item-count])]
    [:<>
     [:button.btn.btn-dark.me-2
      {:type :button
       :on-click #(rf/dispatch [:feedback.create/set-item-count (inc feedback-count)])}
      [icon :plus] " " (labels :feedback.create/add-button)]
     (when (> feedback-count 1)
       [:button.btn.btn-dark.me-2
        {:type :button
         :on-click #(rf/dispatch [:feedback.create/set-item-count (dec feedback-count)])}
        [icon :minus] " " (labels :feedback.create/remove-button)])]))

(defn feedback-tab
  "Feedback tab menu to create a Feedback form."
  []
  [:div.pt-2
   [feedback-tab-entries]
   [:div.text-center.mb-3
    [feedback-tab-add-remove-entry-buttons]]])

(rf/reg-sub
 :feedback.create/item-count
 (fn [db _]
   (get-in db [:feedback :create :item-count] 1)))

(rf/reg-event-db
 :feedback.create/set-item-count
 (fn [db [_ new-count]]
   (assoc-in db [:feedback :create :item-count] new-count)))
