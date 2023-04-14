(ns schnaq.interface.views.schnaq.feedback-card
  (:require ["react-bootstrap/Button" :as Button]
            ["react-bootstrap/Form" :as Form]
            [goog.string :as gstring]
            [oops.core :refer [oget oget+]]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.inputs :as inputs]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.matomo :as matomo]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.localstorage :as localstorage]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.schnaq.dropdown-menu :as dropdown-menu]))

(def ^:private FormCheck (oget Form :Check))
(def ^:private default-feedback-background
  "https://s3.schnaq.com/schnaq-common/background/layered_background_secondary.webp")

(defn- feedback-entry
  "Render a single feedback entry."
  [item-count]
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

(defn- feedback-tab-entries
  "Render all feedback entries."
  []
  (let [feedback-count @(rf/subscribe [:feedback.create/total-item-count])]
    [:<>
     (for [item-count (range 0 feedback-count)]
       (with-meta
         [feedback-entry item-count]
         {:key (str "feedback-entry-key-" item-count)}))]))

(defn- feedback-tab-add-remove-entry-buttons
  "Add and remove buttons for feedback entries."
  []
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

(defn feedback-dropdown-menu
  "Moderator Menu for the feedback card"
  []
  (let [{:keys [db/id feedback/visible]} @(rf/subscribe [:feedback/current])]
    [dropdown-menu/moderator
     {:id "feedback-dropdown-id"}
     [:<>
      [dropdown-menu/item :bullseye
       :schnaq.admin.focus/button
       #(rf/dispatch [:schnaq.moderation.focus/entity id])]
      [dropdown-menu/item (if visible :eye :eye-slash)
       (if visible :feedback.card.dropdown/make-invisible :feedback.card.dropdown/make-visible)
       #(rf/dispatch [:schnaq.feedback.update/toggle-visibility])]
      [dropdown-menu/item :trash
       :feedback.card.dropdown/delete-button
       #(rf/dispatch [:schnaq.feedback/delete])]]]))

(defn feedback-card
  "Displays the link to the Feedback Form."
  []
  (let [user-moderator? @(rf/subscribe [:user/moderator?])]
    [motion/fade-in-and-out
     [:section.activation-card.card-with-background
      {:style {:background-image (gstring/format "url('%s')" default-feedback-background)}}
      [:div.mx-4.my-2
       [:div.d-flex
        [:h4.pb-2.text-center.mx-auto (labels :feedback.card/title)]
        [feedback-dropdown-menu]]
       [:div.text-center
        [:p.text-center.my-5 (labels :feedback.card/primer)]
        [:a.btn.btn-lg.btn-primary
         {:href
          (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
            (if user-moderator?
              (navigation/href :routes.schnaq.feedback/results {:share-hash share-hash})
              (navigation/href :routes.schnaq/feedback {:share-hash share-hash})))}
         (if user-moderator?
           (labels :feedback.card/button-text-moderator)
           (labels :feedback.card/button-text))]
       ;; Only mods see the card when its invisible anyways, so no need for additional mod check.
        (when-not (:feedback/visible @(rf/subscribe [:feedback/current]))
          [:div.d-inline-block.alert.alert-primary.mt-5
           [icon :eye-slash "me-1"]
           [:span (labels :feedback.card/invisibility-reminder)]])]]]]))

(rf/reg-sub
 :feedback/current
 (fn [db _]
   (get-in db [:schnaq :selected :discussion/feedback])))

(rf/reg-sub
 :feedback.current/item-label
 ;; Retrieve an item on position 'order' from the current feedback-items.
 (fn [db [_ order]]
   (let [feedback-items (get-in db [:schnaq :selected :discussion/feedback :feedback/items] [])
         item (nth feedback-items order nil)]
     (get item :feedback.item/label))))

(rf/reg-sub
 :feedback.create/temp-item-count
 ;; Get the temporary items count. This value is used to determine the number of
 ;; feedback items when editing in relation to the number of items in the
 ;; current feedback-form.
 (fn [db _]
   ;; If there are no non-temp items report the count  as 1, because at least one input is always shown.
   (let [current-item-count (count (get-in db [:schnaq :selected :discussion/feedback :feedback/items] []))]
     (get-in db [:feedback-form :create :item-count] (if (zero? current-item-count) 1 0)))))

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
     {:db (assoc-in db [:feedback-form :create :temp-items] feedback-items)
      :fx [(http/xhrio-request db :post "/discussion/feedback/form"
                               [:schnaq.feedback.create/success]
                               params)]})))

(rf/reg-event-fx
 ;; Update the feedback form with the given feedback-items. If no visibility is provided currently set visibility is used.
 :schnaq.feedback/update
 (fn [{:keys [db]} [_ feedback-items visible?]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         visible? (if (nil? visible?) (get-in db [:schnaq :selected :discussion/feedback :feedback/visible] false) visible?)
         params {:share-hash share-hash :items feedback-items :visible? (true? visible?)}]
     {:db (assoc-in db [:feedback-form :create :temp-items] feedback-items)
      :fx [(http/xhrio-request db :put "/discussion/feedback/form"
                               [:schnaq.feedback.update/success]
                               params)]})))

(rf/reg-event-fx
 ;; Leaves everything as is, just changes visibility to false if currently true and otherwise.
 :schnaq.feedback.update/toggle-visibility
 (fn [{:keys [db]} _]
   (let [current-items (get-in db [:schnaq :selected :discussion/feedback :feedback/items] [])
         toggled-visibility (not (get-in db [:schnaq :selected :discussion/feedback :feedback/visible] false))]
     {:fx [[:dispatch [:schnaq.feedback/update current-items toggled-visibility]]]})))

(rf/reg-event-fx
 :schnaq.feedback/delete
 (fn [{:keys [db]} _]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:fx [(http/xhrio-request db :delete "/discussion/feedback/form"
                               [:schnaq.feedback.delete/success]
                               {:share-hash share-hash})]})))

(rf/reg-event-db
 :schnaq.feedback.delete/success
 (fn [db [_ response]]
   (if (:deleted? response)
     (update-in db [:schnaq :selected] dissoc :discussion/feedback)
     db)))

(rf/reg-event-db
 :feedback.create/reset-item-count
 (fn [db _]
   (assoc-in db [:feedback-form :create :item-count] 0)))

(rf/reg-event-db
 :feedback.create/set-temporary-as-items
 (fn [db _]
   (let [temp-items (get-in db [:feedback-form :create :temp-items])]
     (assoc-in db [:schnaq :selected :discussion/feedback :feedback/items] temp-items))))

(rf/reg-event-fx
 :schnaq.feedback.create/success
 (fn [{:keys [db]} [_ {:keys [feedback-form-id]}]]
   {:db (-> db
            (assoc-in [:schnaq :selected :discussion/feedback :db/id] feedback-form-id)
            (assoc-in [:schnaq :selected :discussion/feedback :feedback/visible] true)
            (tools/new-activation-focus feedback-form-id))
    :fx [[:dispatch [:feedback.create/set-temporary-as-items]]
         [:dispatch [:feedback.create/reset-item-count]]
         [:dispatch [:notification/add
                     #:notification{:title (labels :feedback.create.success/title)
                                    :body [:<>
                                           (labels :feedback.create.success/message)]
                                    :context :success
                                    :stay-visible? false}]]]}))

(rf/reg-event-fx
 :schnaq.feedback.update/success
 (fn [{:keys [db]} [_ {:keys [updated-form]}]]
   {:db (assoc-in db [:schnaq :selected :discussion/feedback] updated-form)
    :fx [[:dispatch [:feedback.create/reset-item-count]]
         [:dispatch [:notification/add
                     #:notification{:title (labels :feedback.update.success/title)
                                    :body [:<>
                                           (labels :feedback.update.success/message)]
                                    :context :success
                                    :stay-visible? false}]]]}))

(rf/reg-sub
 :schnaq.feedback/exists-for-user?
 :<- [:feedback/current]
 :<- [:user/moderator?]
 (fn [[feedback is-moderator?] _]
   (or is-moderator?
       (and (not (contains? (localstorage/from-localstorage :discussion/feedbacks) (:db/id feedback)))
            (:feedback/visible feedback)))))
