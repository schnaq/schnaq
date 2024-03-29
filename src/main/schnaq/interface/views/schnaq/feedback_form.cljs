(ns schnaq.interface.views.schnaq.feedback-form
  (:require ["react-bootstrap/Button" :as Button]
            ["react-bootstrap/Form" :as Form]
            ["react-bootstrap/Table" :as Table]
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
   {:inline true :type "radio"
    :name name :label label :className "scale-radio-button position-relative" :id (str name "-" label) :data-value label
    :style {:z-index 900}}])

(defn- scale-input
  "A scale input with 5 radio buttons."
  [question-ordinal]
  [:div.border.rounded.p-3.text-center
   [:div.d-inline-block.position-relative.bg-transparent.scale-wrapper
    [:div.scale-gradient]
    (for [label (range 1 6)]
      (with-meta
        [scale-radio-button (str "feedback-item-" question-ordinal) (str label)]
        {:key (str "feedback-item-" question-ordinal "-" label)}))]])

(defn- feedback-form
  "A dynamically generated form where the user can answer a few questions for feedback purposes."
  []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])
        feedback (:discussion/feedback current-discussion)
        items (sort-by :feedback.item/ordinal (:feedback/items feedback))
        loading? @(rf/subscribe [:schnaq.feedback.answer/loading?])
        user-participated? (contains? (localstorage/from-localstorage :discussion/feedbacks) (:db/id feedback))]
    [pages/with-discussion-header
     {:page/heading (:discussion/title current-discussion)}
     [:div.p-4.text-center.panel-white.centered-form.mb-5.mt-2
      [:h1 (gstring/format (labels :feedback.answer/title) (:discussion/title current-discussion))]
      [:p.text-muted (labels :feedback.answer/title-hint)]
      (if (:feedback/visible feedback)
        (if user-participated?
          [:div.text-center.alert.alert-secondary
           [:p.h6 (labels :feedback.answer/already-participated)]
           [:> Button
            {:variant "primary"
             :href (navigation/href :routes.schnaq/start {:share-hash (:discussion/share-hash current-discussion)})
             :className "mt-4"}
            (labels :feedback.answer.already-participated/button-text)]]
          [:div.text-start
           [:> Form
            {:on-submit (fn [e]
                          (.preventDefault e)
                          (when-not (or loading? user-participated?)
                            (rf/dispatch [:schnaq.feedback/submit (oget e [:target :elements]) items])))}
            (for [question items]
              [:> FormGroup
               {:controlId (str "feedback-item-" (:feedback.item/ordinal question))
                :className "my-4"
                :key (str "feedback-item-" (:feedback.item/ordinal question))}
               [:> FormLabel (:feedback.item/label question)]
               (if (= (:feedback.item/type question) :feedback.item.type/text)
                 [:> FormControl {:placeholder (labels :feedback.answer.text/placeholder)}]
                 ;; Scale
                 [scale-input (:feedback.item/ordinal question)])])
            [:div.text-center
             [:> Button {:variant "primary" :type "submit" :className "mt-4"}
              (if @(rf/subscribe [:schnaq.feedback.answer/loading?])
                [loading/spinner-icon]
                (labels :feedback.answer.submit/button-text))]]]])
        [:div.text-center.alert.alert-secondary
         [:h4 (labels :feedback.answer/feedback-invisible)]
         [:> Button
          {:variant "primary"
           :href (navigation/href :routes.schnaq/start {:share-hash (:discussion/share-hash current-discussion)})
           :className "mt-4"}
          (labels :feedback.answer.already-participated/button-text)]])]]))

(defn- text-results
  "Display the results of the free form feedback field"
  [question results]
  (let [fitting-results (filter #(= (:db/id question) (:feedback.answer/item %)) results)]
    (if (empty? fitting-results)
      [:ul [:li "–"]]
      [:ul
       (for [answer fitting-results]
         [:li {:key (str "text-result-" (:db/id answer))} (:feedback.answer/text answer)])])))

(defn- scale-results
  "Display the results of the scale rating feedback field"
  [question results]
  (let [fitting-results (filter #(= (:db/id question) (:feedback.answer/item %)) results)
        grouped-results (group-by :feedback.answer/scale-five fitting-results)
        rating-values (sort (map :feedback.answer/scale-five fitting-results))
        average (when (seq fitting-results)
                  (/ (reduce + rating-values)
                     (count fitting-results)))
        median (when (seq fitting-results) (nth rating-values (int (/ (count rating-values) 2))))]
    (if (empty? fitting-results)
      [:p "–"]
      [:> Table {:striped true :hover true :bordered true :size "sm"}
       [:thead
        [:tr
         [:th (labels :feedback.answer.results.scale/value)]
         [:th (labels :feedback.answer.results.scale/result)]]]
       [:tbody
        (for [answer grouped-results]
          [:tr {:key (str "text-result-" (:db/id question))}
           [:td (first answer)]
           [:td (count (second answer))]])
        [:tr
         [:td (labels :feedback.answer.results.scale/average)]
         [:td (or average "–")]]
        [:tr
         [:td (labels :feedback.answer.results.scale/median)]
         [:td (or median "–")]]]])))

(defn- feedback-form-results
  "Display the results of the feedback form for moderators."
  []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])
        feedback (:discussion/feedback current-discussion)
        items (sort-by :feedback.item/ordinal (:feedback/items feedback))
        results @(rf/subscribe [:feedback/results])
        title (gstring/format (labels :feedback.answer/title) (:discussion/title current-discussion))]
    [pages/with-discussion-header
     {:page/heading title}
     [:div.p-4.panel-white.centered-form.mb-5.mt-2
      [:h1.text-center title]
      [:div
       (for [question items]
         [:> FormGroup
          {:controlId (str "feedback-item-" (:feedback.item/ordinal question))
           :className "my-4"
           :key (str "feedback-item-" (:feedback.item/ordinal question))}
          [:> FormLabel {:className "h5"} (:feedback.item/label question)]
          (if (= (:feedback.item/type question) :feedback.item.type/text)
            [text-results question results]
            ;; Scale
            [scale-results question results])])]]]))

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

;; -----------------------------------------------------------------------------

(defn results-view []
  [feedback-form-results])

(defn feedback-form-view []
  [feedback-form])

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :schnaq.feedback/submit
 (fn [{:keys [db]} [_ form items]]
   (let [answers (remove nil? (map-indexed #(extract-answer %2 form (inc %1)) items))
         share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     (when (seq answers)
       {:db (assoc-in db [:schnaq :feedback-form :answer :loading?] true)
        :fx [(http/xhrio-request db :post "/discussion/feedback"
                                 [:schnaq.feedback.submit/success]
                                 {:share-hash share-hash
                                  :answers answers}
                                 [:schnaq.feedback.submit/failure])]}))))

(rf/reg-event-fx
 :schnaq.feedback.submit/failure
 (fn [{:keys [db]} [_ _response]]
   {:db (assoc-in db [:schnaq :feedback-form :answer :loading?] false)
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
         feedback-id (get-in db [:schnaq :selected :discussion/feedback :db/id])]
     {:db (assoc-in db [:schnaq :feedback-form :answer :loading?] false)
      :fx [[:dispatch [:navigation/navigate :routes.schnaq/start
                       {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])}]]
           [:dispatch [:notification/add
                       #:notification{:title (labels :feedback.answer.submit.success/title)
                                      :body [:<>
                                             (labels :feedback.answer.submit.success/message)]
                                      :context :success
                                      :stay-visible? false}]]
           [:localstorage/assoc [:discussion/feedbacks (into #{} (conj old-feedbacks feedback-id))]]]})))

(rf/reg-sub
 :schnaq.feedback.answer/loading?
 (fn [db _]
   (get-in db [:schnaq :feedback-form :answer :loading?] false)))

(rf/reg-event-fx
 :schnaq.feedback/load-moderator-results
 (fn [{:keys [db]} [_ share-hash]]
   {:fx [(http/xhrio-request db :get "/discussion/feedback/results"
                             [:schnaq.feedback.load-moderator-results/success]
                             {:share-hash share-hash})]}))

(rf/reg-event-db
 :schnaq.feedback.load-moderator-results/success
 (fn [db [_ response]]
   (assoc-in db [:schnaq :feedback-form :results] (get-in response [:feedback-form :feedback/answers]))))

(rf/reg-sub
 :feedback/results
 (fn [db _]
   (get-in db [:schnaq :feedback-form :results])))
