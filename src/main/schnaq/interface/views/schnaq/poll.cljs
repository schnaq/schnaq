(ns schnaq.interface.views.schnaq.poll
  (:require [goog.string :as gstring]
            [hodgepodge.core :refer [local-storage]]
            [oops.core :refer [oget oget+]]
            [re-frame.core :as rf]
            [schnaq.interface.components.colors :as colors]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.inputs :as inputs]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.schnaq.dropdown-menu :as dropdown-menu]))

(defn results-graph
  "A graph displaying the results of the poll."
  [{:poll/keys [options type]} cast-votes]
  [:section.row
   (for [index (range (count options))]
     (let [{:keys [option/votes db/id option/value]} (get options index)
           total-votes (apply + (map :option/votes options))
           percentage (if (zero? total-votes)
                        "0%"
                        (str (.toFixed (* 100 (/ votes total-votes)) 2) "%"))
           single-choice? (= :poll.type/single-choice type)
           votes-set (if single-choice? #{cast-votes} (set cast-votes))
           option-voted? (votes-set id)]
       [:<>
        {:key (str "option-" id)}
        (when-not cast-votes
          [:div.col-1
           [:input.form-check-input.mt-3.mx-auto
            (cond->
             {:type (if single-choice? "radio" "checkbox")
              :name :option-choice
              :value id}
              (and (zero? index) single-choice?) (assoc :defaultChecked true))]])
        [:div.my-1
         {:class (if cast-votes "col-12" "col-11")}
         [:div.percentage-bar.rounded-1
          {:class (when option-voted? "percentage-bar-highlight")
           :style {:background-color (colors/get-graph-color index)
                   :width percentage
                   :height "30px"}}]
         [:p.small.ms-1
          {:class (when option-voted? "font-italic")}
          value
          [:span.float-end
           [:span.me-3 votes " " (labels :schnaq.poll/votes)]
           percentage]]]]))])

(defn- dropdown-menu
  "Dropdown menu for poll configuration."
  [poll-id]
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [dropdown-menu/moderator
     (str "poll-dropdown-id-" poll-id)
     [:<>
      [dropdown-menu/item :play/circle
       :view/present
       #(rf/dispatch [:navigation/navigate :routes.present/entity
                      {:share-hash share-hash :entity-id poll-id}])]
      [dropdown-menu/item :trash
       :schnaq.poll/delete-button
       #(rf/dispatch [:poll/delete poll-id])]]]))

(defn poll-list
  "Displays all polls of the current schnaq."
  []
  (let [polls @(rf/subscribe [:schnaq/polls])]
    ;; This doall is needed, for the reactive deref inside to work
    (doall
     (for [poll polls]
       (let [poll-id (:db/id poll)
             cast-votes @(rf/subscribe [:schnaq/vote-cast poll-id])]
         [:div.statement-column
          {:key (str "poll-result-" poll-id)}
          [motion/fade-in-and-out
           [:section.statement-card
            [:div.mx-4.my-2
             [:div.d-flex
              [:h6.pb-2.text-center.mx-auto (:poll/title poll)]
              [dropdown-menu poll-id]]
             [results-graph (:poll/options poll) cast-votes]
             [:form
              {:on-submit (fn [e]
                            (.preventDefault e)
                            (rf/dispatch [:schnaq.poll/cast-vote (oget e [:target :elements]) poll]))}
              (when-not cast-votes
                [:div.text-center
                 [:button.btn.btn-primary.btn-sm
                  {:type :submit}
                  (labels :schnaq.poll/vote!)]])]]]
           motion/card-fade-in-time]])))))

(defn- poll-option
  "Returns a single option component. Can contain a button for removal of said component."
  ([placeholder rank]
   [inputs/text placeholder (str "poll-option-" rank)]))

(rf/reg-event-db
 :polls.create/set-option-count
 (fn [db [_ new-count]]
   (assoc-in db [:poll :create :option-count] new-count)))

(rf/reg-event-db
 :polls.create/reset-option-count
 (fn [db _]
   (assoc-in db [:poll :create :option-count] 2)))

(rf/reg-sub
 :polls.create/option-count
 (fn [db _]
   (get-in db [:poll :create :option-count] 2)))

(defn poll-form
  "Input form to create a poll with multiple options."
  []
  (let [option-count @(rf/subscribe [:polls.create/option-count])]
    [:form.pt-2
     {:on-submit (fn [event]
                   (.preventDefault event)
                   (rf/dispatch [:schnaq.poll/create-new
                                 (oget event [:target :elements])
                                 option-count]))}
     [:div.mb-3
      [:label.form-label {:for :poll-topic} (labels :schnaq.poll.create/topic-label)]
      [inputs/text (labels :schnaq.poll.create/placeholder) :poll-topic]
      [:small.form-text.text-muted (labels :schnaq.poll.create/hint)]]
     [:div.mb-3
      [:label.form-label (labels :schnaq.poll.create/options-label)]
      [poll-option "Pyrrhus" 1]
      [poll-option "Surus" 2]
      (for [rank (range 3 (inc option-count))]
        (with-meta
          [poll-option (str (labels :schnaq.poll.create/options-placeholder) " " rank) rank]
          {:key (str "poll-option-key-" rank)}))]
     [:div.text-center.mb-3
      [:button.btn.btn-dark.me-2
       {:type :button
        :on-click #(rf/dispatch [:polls.create/set-option-count (inc option-count)])}
       [icon :plus] " " (labels :schnaq.poll.create/add-button)]
      (when (> option-count 2)
        [:button.btn.btn-dark
         {:type :button
          :on-click #(rf/dispatch [:polls.create/set-option-count (dec option-count)])}
         [icon :minus] " " (labels :schnaq.poll.create/remove-button)])]
     [:div.form-check.form-check-inline
      [:input#radio-single-choice.form-check-input
       {:type "radio"
        :name :radio-type-choice
        :value :single
        :defaultChecked true}]
      [:label.form-check-label
       {:for :radio-single-choice} (labels :schnaq.poll.create/single-choice-label)]]
     [:div.form-check.form-check-inline
      [:input#radio-multiple-choice.form-check-input
       {:type "radio"
        :name :radio-type-choice
        :value :multiple}]
      [:label.form-check-label
       {:for :radio-multiple-choice} (labels :schnaq.poll.create/multiple-choice-label)]]
     [:div.form-check.form-check-inline
      [:input#radio-ranking-choice.form-check-input
       {:type "radio"
        :name :radio-type-choice
        :value :ranking}]
      [:label.form-check-label
       {:for :radio-ranking-choice} (labels :schnaq.poll.create/ranking-label)]]
     [:div.text-center.pt-2
      [:button.btn.btn-primary.w-75 {:type "submit"} (labels :schnaq.poll.create/submit-button)]]]))

(rf/reg-event-fx
 :schnaq.poll/create-new
 (fn [{:keys [db]} [_ form-elements number-of-options]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         params {:title (oget form-elements :poll-topic :value)
                 :poll-type (case (oget form-elements :radio-type-choice :value)
                              "multiple" :poll.type/multiple-choice
                              "single" :poll.type/single-choice
                              "ranking" :poll.type/ranking)
                 :options (mapv #(oget+ form-elements (str "poll-option-" %) :value)
                                (range 1 (inc number-of-options)))
                 :share-hash share-hash
                 :edit-hash (get-in db [:schnaqs :admin-access share-hash])}]
     {:fx [(http/xhrio-request db :post "/poll"
                               [:schnaq.poll.create-new/success form-elements]
                               params)]})))

(rf/reg-event-fx
 :schnaq.poll/cast-vote
 (fn [{:keys [db]} [_ form-elements poll]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         single-choice? (= :poll.type/single-choice (:poll/type poll))
         poll-id (:db/id poll)
         ;; TODO explicitly target single and multiple choice here, when the voting for rankings is in
         chosen-option (if (and single-choice? (oget form-elements :option-choice))
                         (js/parseInt (oget form-elements :option-choice :value))
                         (tools/checked-values (oget form-elements :option-choice)))
         poll-update-fn (case (:poll/type poll)
                          :poll.type/single-choice
                          #(if (= chosen-option (:db/id %))
                             (update % :option/votes inc)
                             %)
                          :poll.type/multiple-choice
                          #(if (contains? (set chosen-option) (:db/id %))
                             (update % :option/votes inc)
                             %)
                          :poll.type/ranking
                          #(let [weighted-options
                                 (->> (range (count (:poll/options poll)) 0 -1)
                                      (interleave chosen-option)
                                      (apply hash-map))]
                             (if (contains? (set chosen-option) (:db/id %))
                               (update % :option/votes + (weighted-options (:db/id %)))
                               %)))
         poll (update poll :poll/options #(mapv poll-update-fn %))]
     {:db (-> db
              (update-in [:schnaq :current :polls]
                         (fn [polls]
                           (map #(if (= poll-id (:db/id %)) poll %) polls)))
              (assoc-in [:schnaq :polls :past-votes poll-id] chosen-option))
      :fx [(http/xhrio-request db :put (gstring/format "/poll/%s/vote" poll-id)
                               [:schnaq.poll.cast-vote/success]
                               {:share-hash share-hash
                                :option-id chosen-option}
                               [:schnaq.poll.cast-vote/failure poll-id])]})))

(rf/reg-event-fx
 :schnaq.poll.cast-vote/success
 (fn [{:keys [db]} _]
   {:fx [[:localstorage/assoc [:poll/cast-votes (get-in db [:schnaq :polls :past-votes])]]]}))

(rf/reg-event-db
 :schnaq.poll.cast-vote/failure
 (fn [db [_ poll-id]]
   (update-in db [:schnaq :polls :past-votes] dissoc poll-id)))

(rf/reg-event-fx
 :schnaq.poll.create-new/success
 (fn [{:keys [db]} [_ form-elements response]]
   {:db (update-in db [:schnaq :current :polls]
                   #(if (nil? %1)
                      [%2]
                      (conj %1 %2))
                   (:new-poll response))
    :fx [[:form/clear form-elements]
         [:dispatch [:polls.create/reset-option-count]]]}))

(rf/reg-sub
 :schnaq/polls
 ;; Returns all polls of the selected schnaq.
 (fn [db _]
   (get-in db [:schnaq :current :polls] [])))

(rf/reg-event-fx
 :schnaq.poll/load-from-query
 (fn [{:keys [db]} _]
   {:fx [(http/xhrio-request
          db :get "/poll"
          [:schnaq.poll.load-from-query/success]
          {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])
           :poll-id (get-in db [:current-route :parameters :path :entity-id])})]}))

(rf/reg-event-db
 :schnaq.poll.load-from-query/success
 (fn [db [_ {:keys [poll]}]]
   (assoc-in db [:present :poll] poll)))

(rf/reg-sub
 :schnaq/vote-cast
 ;; Show whether and what a user has already voted for a certain poll
 (fn [db [_ poll-id]]
   (get-in db [:schnaq :polls :past-votes poll-id])))

(rf/reg-event-fx
 :schnaq.polls/load-from-backend
 (fn [{:keys [db]} _]
   {:fx [(http/xhrio-request
          db :get "/polls"
          [:schnaq.polls.load-from-backend/success]
          {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])})]}))

(rf/reg-event-db
 :schnaq.polls.load-from-backend/success
 (fn [db [_ response]]
   (when-let [polls (:polls response)]
     (assoc-in db [:schnaq :current :polls] polls))))

(rf/reg-event-db
 :schnaq.polls/load-past-votes
 ;; Load past votes from localstorage
 (fn [db _]
   (assoc-in db [:schnaq :polls :past-votes] (:poll/cast-votes local-storage))))

(rf/reg-event-fx
 :poll/delete
 (fn [{:keys [db]} [_ poll-id]]
   {:db (let [polls (get-in db [:schnaq :current :polls])]
          (assoc-in db [:schnaq :current :polls]
                    (remove #(= poll-id (:db/id %)) polls)))
    :fx [(http/xhrio-request
          db :delete "/poll/delete"
          [:no-op]
          {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])
           :edit-hash (get-in db [:schnaq :selected :discussion/edit-hash])
           :poll-id poll-id})]}))
