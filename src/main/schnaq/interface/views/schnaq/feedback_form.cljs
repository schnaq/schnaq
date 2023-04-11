(ns schnaq.interface.views.schnaq.feedback-form
  (:require ["react-bootstrap/Button" :as Button]
            ["react-bootstrap/Form" :as Form]
            [goog.string :as gstring]
            [oops.core :refer [oget oget+]]
            [re-frame.core :as rf]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.localstorage :as localstorage]
            [schnaq.interface.views.loading :as loading]
            [schnaq.interface.views.pages :as pages]))

(def ^:private FormGroup (oget Form :Group))
(def ^:private FormControl (oget Form :Control))
(def ^:private FormLabel (oget Form :Label))
(def ^:private FormCheck (oget Form :Check))

(defn- scale-radio-button
  "A typical text input for the feedback form."
  [name label]
  [:> FormCheck
   {:inline true :type "radio" :name name :label label :class "scale-radio-button" :id (str name "-" label) :data-value label}])

(defn- scale-input
  "A scale input with 5 radio buttons."
  [question-ordinal]
  [:div.border.rounded.p-3.text-center
   (for [label (range 1 6)]
     (with-meta
       [scale-radio-button (str "feedback-item-" question-ordinal) (str label)]
       {:key (str "feedback-item-" question-ordinal "-" label)}))])

(defn- feedback-form
  "A dynamically generated form where the user can answer a few questions for feedback purposes."
  []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])
        feedback (:discussion/feedback current-discussion)
        items (sort-by :feedback.item/ordinal (:feedback/items feedback))
        loading? @(rf/subscribe [:schnaq.feedback.answer/loading?])
        user-participated? (contains? (localstorage/from-localstorage :discussion/feedbacks)
                                      (:discussion/share-hash current-discussion))]
    [pages/with-discussion-header
     {:page/heading (:discussion/title current-discussion)}
     [:div.panel-white.p-4.text-center
      [:h1 (gstring/format (labels :feedback.answer/title) (:discussion/title current-discussion))]
      [:p.text-muted (labels :feedback.answer/title-hint)]
      (if user-participated?
        [:div.text-center.centered-form.alert.alert-secondary
         [:h4 (labels :feedback.answer/already-participated)]
         [:> Button
          {:variant "primary"
           :href (navigation/href :routes.schnaq/start {:share-hash (:discussion/share-hash current-discussion)})
           :class "mt-4"}
          (labels :feedback.answer.already-participated/button-text)]]
        [:div.text-start.centered-form
         [:> Form
          {:on-submit (fn [e]
                        (.preventDefault e)
                        (when-not (or loading? user-participated?)
                          (rf/dispatch [:schnaq.feedback/submit (oget e [:target :elements]) items])))}
          (for [question items]
            [:> FormGroup
             {:controlId (str "feedback-item-" (:feedback.item/ordinal question))
              :class "my-4"
              :key (str "feedback-item-" (:feedback.item/ordinal question))}
             [:> FormLabel (:feedback.item/label question)]
             (if (= (:feedback.item/type question) :feedback.item.type/text)
               [:> FormControl {:placeholder (labels :feedback.answer.text/placeholder)}]
               ;; Scale
               [scale-input (:feedback.item/ordinal question)])])
          [:div.text-center
           [:> Button {:variant "primary" :type "submit" :class "mt-4"}
            (if @(rf/subscribe [:schnaq.feedback.answer/loading?])
              [loading/spinner-icon]
              (labels :feedback.answer.submit/button-text))]]]])]]))

(defn feedback-form-view []
  [feedback-form])

(defn- extract-text
  "Extracts text from a text input field that was built dynamically for the feedback form."
  [item form index]
  (let [raw-answer {:feedback.answer/item (:db/id item)}
        form-answer (oget+ form (str "feedback-item-" index) [:value])]
    (when (seq form-answer)
      (assoc raw-answer :feedback.answer/text form-answer))))

(defn- extract-scale
  "Extracts a scale answer from a a form input field consisting of multiple radio buttons. Returns nil, when no answer
  was given."
  [item form index]
  (let [raw-answer {:feedback.answer/item (:db/id item)}
        form-answers (oget+ form (str "feedback-item-" index))
        checked-answer (when (seq form-answers)
                         (first (filter #(oget % :checked) form-answers)))]
    (when checked-answer
      (assoc raw-answer :feedback.answer/scale-five (js/parseInt (oget checked-answer [:dataset :value]))))))

(defn- extract-answer
  "Extracts a single answer from the input field. Checks, whether the answer was given or not and removes empty ones."
  [item form index]
  (if (= (:feedback.item/type item) :feedback.item.type/text)
    (extract-text item form index)
    (extract-scale item form index)))

(rf/reg-event-fx
 :schnaq.feedback/submit
 (fn [{:keys [db]} [_ form items]]
   (let [answers (remove nil? (map-indexed #(extract-answer %2 form (inc %1)) items))
         share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     (when (seq answers)
       {:db (assoc-in db [:schnaq :feedback-form :answer :loading] true)
        :fx [(http/xhrio-request db :post "/discussion/feedback"
                                 [:schnaq.feedback.submit/success]
                                 {:share-hash share-hash
                                  :answers answers}
                                 [:schnaq.feedback.submit/failure])]}))))

(rf/reg-event-fx
 :schnaq.feedback.submit/failure
 (fn [{:keys [db]} [_ _response]]
   {:db (assoc-in db [:schnaq :feedback-form :answer :loading] false)
    :fx [[:dispatch [:notification/add
                     #:notification{:title (labels :feedback.answer.submit.failure/title)
                                    :body [:<>
                                           (labels :feedback.answer.submit.failure/message)]
                                    :context :warning
                                    :stay-visible? false}]]]}))


(rf/reg-event-fx
 :schnaq.feedback.submit/success
 (fn [{:keys [db]} [_ _response]]
   (let [old-feedbacks (localstorage/from-localstorage :discussion/feedbacks)
         share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:db (assoc-in db [:schnaq :feedback-form :answer :loading] false)
      :fx [[:dispatch [:navigation/navigate :routes.schnaq/start
                       {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])}]]
           [:dispatch [:notification/add
                       #:notification{:title (labels :feedback.answer.submit.success/title)
                                      :body [:<>
                                             (labels :feedback.answer.submit.success/message)]
                                      :context :success
                                      :stay-visible? false}]]
           [:localstorage/assoc [:discussion/feedbacks (into #{} (conj old-feedbacks share-hash))]]]})))

(rf/reg-sub
 :schnaq.feedback.answer/loading?
 (fn [db _]
   (get-in db [:schnaq :feedback-form :answer :loading] false)))