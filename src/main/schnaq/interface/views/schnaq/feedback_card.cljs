(ns schnaq.interface.views.schnaq.feedback-card
  (:require ["react-bootstrap/Button" :as Button]
            ["react-bootstrap/Form" :as Form]
            [oops.core :refer [oget oget+]]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.inputs :as inputs]
            [schnaq.interface.matomo :as matomo]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]))

(def ^:private FormCheck (oget Form :Check))

(defn- feedback-entry [item-count]
  (let [radio-name (keyword (str "feedback-item-type-" item-count))
        id-text (str "radio-item-type-text-" item-count)
        id-scale-5 (str "radio-item-type-scale-five-" item-count)
        placeholder (labels :feedback.create/placeholder-text)
        default-value @(rf/subscribe [:feedback.current/item-label item-count])]
    [:div.my-4
     [inputs/floating placeholder (str "feedback-item-label-" item-count)
      {:required true
       :defaultValue default-value}]
     [:p.me-3.d-inline (labels :schnaq.feedback.create/which-type)]
     [:> FormCheck
      {:id id-text
       :inline true
       :type "radio"
       :name radio-name
       :value :text
       :defaultChecked true
       :label (labels :schnaq.feedback.create/text)}]
     [:> FormCheck
      {:id id-scale-5
       :inline true
       :type "radio"
       :name radio-name
       :value :scale-five
       :label (labels :schnaq.feedback.create/rating)}]]))

(defn- feedback-tab-entries []
  (let [feedback-count @(rf/subscribe [:feedback.create/total-item-count])]
    [:<>
     (for [item-count (range 0 feedback-count)]
       (with-meta
         [feedback-entry item-count]
         {:key (str "feedback-entry-key-" item-count)}))]))

(defn- feedback-tab-add-remove-entry-buttons []
  (let [feedback-count @(rf/subscribe [:feedback.create/total-item-count])
        temp-feedback-count @(rf/subscribe [:feedback.create/temp-item-count])]
    [:<>
     [:> Button {:variant "dark"
                 :className "me-2"
                 :type :button
                 :on-click #(rf/dispatch [:feedback.create/set-item-count (inc temp-feedback-count)])}
      [icon :plus "me-1"] (labels :feedback.create/add-button)]
     (when (> feedback-count 1)
       [:> Button {:variant "dark"
                   :type :button
                   :on-click #(rf/dispatch [:feedback.create/set-item-count (dec temp-feedback-count)])}
        [icon :minus "me-1"] (labels :feedback.create/remove-button)])]))

(defn- extract-feedback-from-form
  "Extract information from a feedback form."
  [form ordinal]
  (let [type (case (oget+ form (str "feedback-item-type-" ordinal) :value)
               "text" :feedback.item.type/text
               "scale-five" :feedback.item.type/scale-five)
        label (oget+ form (str "feedback-item-label-" ordinal) :value)]
    {:feedback.item/label label
     :feedback.item/type type
     :feedback.item/ordinal (inc ordinal)})) ;; ordinal starts at 1 not 0

(defn- extract-all-feedback-from-form
  "Map over all feedback form items and retrieve labels and types."
  [form item-count]
  (map #(extract-feedback-from-form form %) (range 0 item-count)))

(defn feedback-tab
  "Feedback tab menu to create a Feedback form."
  []
  (let [current-feedback @(rf/subscribe [:feedback/current])
        feedback-count @(rf/subscribe [:feedback.create/total-item-count])
        submit-event (if current-feedback :schnaq.feedback/update :schnaq.feedback/create)]
    [:> Form {:on-submit (fn [event]
                           (.preventDefault event)
                           (let [form (oget event [:target :elements])]
                             (rf/dispatch [submit-event (extract-all-feedback-from-form form feedback-count)])))}
     [feedback-tab-entries]
     [:div.text-center
      [feedback-tab-add-remove-entry-buttons]
      [:> Button {:variant "primary"
                  :className "w-75 mt-3 mx-auto d-block"
                  :type :submit
                  :on-click #(matomo/track-event "Active User" "Action" "Create FeedbackForm")}
       (labels :feedback.create/submit-button)]]]))

(rf/reg-sub
 :feedback/current
 (fn [db _]
   (get-in db [:schnaq :selected :discussion/feedback] nil)))

(rf/reg-sub
 :feedback.current/item-label
 ;; Retrieve an item on position 'order' from the current feedback-items.
 (fn [db [_ order]]
   (let [feedback-items (get-in db [:schnaq :selected :discussion/feedback :feedback/items] [])
         item (nth feedback-items order nil)]
     (nth feedback-items order nil)
     (get item :feedback.item/label ""))))

(rf/reg-sub
 :feedback.create/temp-item-count
 ;; Get the temporary items count. This value is used to determine the number of
 ;; feedback items when editing in relation to the number of items in the
 ;; current feedback-form.
 (fn [db _]
   (get-in db [:feedback-form :create :item-count] 0)))

(rf/reg-sub
 :feedback.create/total-item-count
 ;; Get the number of feedback-items currently displayed in the editing tab.
 ;; Consists of the number of items in the current feedback form and the
 ;; temporary item count. If both are 0 (meaning no feedback-form exists
 ;; for this schnaq) return a '1' to display at least one element.
 (fn [db _]
   (let [current-item-count (count (get-in db [:schnaq :selected :discussion/feedback :feedback/items] []))
         temp-item-count (get-in db [:feedback-form :create :item-count] 0)
         total-item-count (+ current-item-count temp-item-count)]
     (max 1 total-item-count))))

(rf/reg-event-db
 :feedback.create/set-item-count
 ;; Update the temporary counter. Use this when changing the number of displayed
 ;; feedback items.
 (fn [db [_ new-count]]
   (assoc-in db [:feedback-form :create :item-count] new-count)))

(rf/reg-event-fx
 :schnaq.feedback/create
 (fn [{:keys [db]} [_ feedback-items]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         params {:share-hash share-hash :items feedback-items}]
     {:db (-> db
              (assoc-in [:feedback-form :create :temp-items] feedback-items))
      :fx [(http/xhrio-request db :post "/discussion/feedback/form"
                               [:schnaq.feedback.create/success]
                               params)]})))

(rf/reg-event-fx
 :schnaq.feedback/update
 (fn [{:keys [db]} [_ feedback-items]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         params {:share-hash share-hash :items feedback-items :visible? true}]
     {:db (-> db
              (assoc-in [:feedback-form :create :temp-items] feedback-items))
      :fx [(http/xhrio-request db :put "/discussion/feedback/form"
                               [:schnaq.feedback.create/success]
                               params)]})))

(rf/reg-event-db
 :feedback.create/reset-item-count
 (fn [db _]
   (let [temp-items (get-in db [:feedback-form :create :temp-items])]
     (-> db
         (assoc-in [:schnaq :selected :discussion/feedback :feedback/items] temp-items)
         (assoc-in [:feedback-form :create :item-count] 0)))))

(rf/reg-event-fx
 :schnaq.feedback.create/success
 (fn [{:keys [db]} [_ {:keys [feedback-form-id]}]]
   {:db (-> db
            (assoc-in [:schnaq :feedback-form :id] feedback-form-id)
            (tools/new-activation-focus feedback-form-id))
    :fx [[:dispatch [:feedback.create/reset-item-count]]]}))
