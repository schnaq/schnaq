(ns schnaq.interface.views.schnaq.poll
  (:require [cljs.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [=> >defn >defn-]]
            [goog.string :as gstring]
            [hodgepodge.core :refer [local-storage]]
            [oops.core :refer [oget oget+]]
            [re-frame.core :as rf]
            [schnaq.database.specs :as specs]
            [schnaq.interface.components.colors :as colors]
            [schnaq.interface.components.common :as common]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.inputs :as inputs]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.matomo :as matomo]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.schnaq.dropdown-menu :as dropdown-menu]
            [schnaq.shared-toolbelt :as shared-tools]))

(defn- percentage-bar
  "An springy-animated percentage bar for graphs"
  [votes percentage color-index]
  [motion/spring-transition
   [tooltip/text
    (str votes " " (labels :schnaq.poll.ranking/points))
    [:div.percentage-bar.rounded-1
     {:style {:background-color (colors/get-graph-color color-index)
              :height "35px"}}]]
   {:width percentage}])

(defn- results-hidden-message
  "Show a message to the user, that the she voted, but is not allowed to see the
  results."
  []
  [common/hint-text (labels :schnaq.poll/hidden-results-hint)])

(defn- show-results-information
  "Information text to indicate that the poll's results are presented to the
  users."
  [hide-results?]
  (when-not hide-results?
    [:div.text-center.text-muted.small
     [icon :eye "me-2"]
     (labels :schnaq.poll.hide-results/hint-text)]))

(defn- results-graph
  "A graph displaying the results of the poll."
  [{:poll/keys [options type hide-results?]} cast-votes]
  (let [read-only? @(rf/subscribe [:schnaq.selected/read-only?])
        edit-hash @(rf/subscribe [:schnaq/edit-hash])
        voted? (or cast-votes read-only?)
        show-results? (or edit-hash (not hide-results?))]
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
          (when-not voted?
            [:div.col-1
             [:input.form-check-input.mx-auto
              (cond->
               {:type (if single-choice? "radio" "checkbox")
                :name :option-choice
                :value id
                :class (if show-results? "mt-3" "mt-2")}
                (and (zero? index) single-choice?) (assoc :defaultChecked true))]])
          [:div.my-1
           {:class (if cast-votes "col-12" "col-11")}
           (when show-results? [percentage-bar votes percentage index])
           [:p.small.ms-1
            {:class (when option-voted? "text-decoration-underline text-secondary")}
            value
            (when show-results?
              [:span.float-end
               [:span.me-3 votes " " (labels :schnaq.poll/votes)]
               percentage])]]]))
     (when (and voted? (not show-results?))
       [results-hidden-message])]))

(defn- ranking-item
  "A single graph-bar in ranking results"
  [sorted-options old-indices index]
  (let [{:keys [option/votes db/id option/value]} (nth sorted-options index)
        total-votes (apply + (map :option/votes sorted-options))
        percentage (if (zero? total-votes)
                     "0%"
                     (str (.toFixed (* 100 (/ votes total-votes)) 2) "%"))
        presentation-mode? (= :routes.present/entity @(rf/subscribe [:navigation/current-route-name]))]
    [motion/animated-list-item
     [:div.row
      [:div
       {:class (if presentation-mode? "col-1" "col-2")}
       [:p.my-auto.mt-2.h5
        (str (inc index)) "."]]
      [:div
       {:class (if presentation-mode? "col-11" "col-10")}
       [percentage-bar votes percentage (get old-indices id)]
       [:p.small.ms-1.mb-1 value]]]]))

(defn ranking-results
  "Show ranking results in a graph."
  [{:poll/keys [options]}]
  [:section.row
   (let [sorted-options (reverse (sort-by :option/votes options))
         old-indices (into {} (map-indexed (fn [idx option] [(:db/id option) idx]) options))]
     [motion/animated-list
      (for [index (range (count sorted-options))]
        (with-meta
          [ranking-item sorted-options old-indices index]
          {:key (str "ranking-option-" (:db/id (nth sorted-options index)))}))])])

(defn- dropdown-menu
  "Dropdown menu for poll configuration."
  [poll]
  (let [poll-id (:db/id poll)
        {:poll/keys [hide-results?]} poll
        share-hash @(rf/subscribe [:schnaq/share-hash])]
    [dropdown-menu/moderator
     {:id (str "poll-dropdown-id-" poll-id)}
     [:<>
      [dropdown-menu/item :play/circle
       :view/present
       #(rf/dispatch [:navigation/navigate :routes.present/entity
                      {:share-hash share-hash :entity-id poll-id}])]
      [dropdown-menu/item :bullseye
       :schnaq.admin.focus/button
       #(rf/dispatch [:schnaq.admin.focus/entity poll-id])]
      [dropdown-menu/item (if hide-results? :eye :eye-slash)
       (if hide-results? :schnaq.poll/show-results-button :schnaq.poll/hide-results-button)
       #(rf/dispatch [:schnaq.poll/hide-results poll-id (not hide-results?)])]
      [dropdown-menu/item :trash
       :schnaq.poll/delete-button
       #(when (js/confirm (labels :schnaq.poll/delete-confirmation))
          (rf/dispatch [:poll/delete poll-id]))]]]))

(>defn- ranking-select
  "Show a select input to choose from the current poll options."
  [poll index]
  [::specs/poll nat-int? => :re-frame/component]
  (let [selected-options @(rf/subscribe [:schnaq.ranking/selected-options (:db/id poll)])
        selected (get selected-options index)
        used-selects (disj (set (vals selected-options)) selected)
        form-id (str "select-field" (:db/id poll) index)]
    [:<>
     [:label.h5.mt-3
      {:for form-id}
      (gstring/format (labels :schnaq.ranking/choose-place) index)]
     [:select.form-select.form-control
      {:id form-id
       :key (str (:db/id poll) "-" index "-" (when selected-options (selected-options index)))
       :defaultValue (when selected-options (selected-options index))
       :on-change (fn [event]
                    (rf/dispatch [:schnaq.ranking/add-selected-options!
                                  (:db/id poll)
                                  index
                                  (js/parseInt (oget event :target :value))]))}
      (when-not selected
        [:option
         {:value :not-selected} "-"])
      (for [voting-option (:poll/options poll)]
        (let [option-id (:db/id voting-option)]
          (when-not (contains? used-selects option-id)
            [:option
             {:value option-id
              :key option-id}
             (:option/value voting-option)])))]]))

(>defn- ranking-input
  "Create a form with multiple selection fields to choose from the poll options."
  [poll]
  [::specs/poll => :re-frame/component]
  (let [poll-id (:db/id poll)
        selected-options @(rf/subscribe [:schnaq.ranking/selected-options poll-id])]
    [:form
     {:on-submit (fn [event]
                   (.preventDefault event)
                   (rf/dispatch [:schnaq.ranking/cast-vote poll (map second (sort-by first selected-options))]))}
     [ranking-select poll 1]
     (for [voted-rankings-index (keys selected-options)
           :while (< voted-rankings-index (count (:poll/options poll)))]
       (with-meta
         [ranking-select poll (inc voted-rankings-index)]
         {:key (str poll-id voted-rankings-index)}))
     (when-not (empty? selected-options)
       [:div.d-flex.justify-content-end
        [:a.btn.btn-transparent
         {:role "button"
          :on-click #(rf/dispatch [:schnaq.ranking/delete-vote poll-id (apply max (keys selected-options))])}
         [icon :backspace] " " (labels :schnaq.rankings/delete-last-choice)]])
     [:button.btn.btn-dark.mt-3.mx-auto.d-block
      {:disabled (not (and selected-options (seq selected-options)))
       :on-click #(matomo/track-event "Active User", "Action", "Vote on Poll")}
      (labels :schnaq.poll/vote!)]]))

(defn- poll-content
  "The content of a single or multiple choice poll. Can be either only the results or results and ability to vote."
  [poll]
  (let [cast-votes @(rf/subscribe [:schnaq/vote-cast (:db/id poll)])
        read-only? @(rf/subscribe [:schnaq.selected/read-only?])
        edit-hash @(rf/subscribe [:schnaq/edit-hash])
        voted? (or cast-votes read-only?)]
    [:form
     {:on-submit (fn [e]
                   (.preventDefault e)
                   (rf/dispatch [:schnaq.poll/cast-vote (oget e [:target :elements]) poll]))}
     [results-graph poll cast-votes]
     (when-not voted?
       [:div.text-center
        [:button.btn.btn-primary.btn-sm
         {:type :submit
          :on-click #(matomo/track-event "Active User" "Action" "Vote on Poll")}
         (labels :schnaq.poll/vote!)]])
     (when edit-hash
       [show-results-information (:poll/hide-results? poll)])]))

(>defn input-or-results
  "Toggle if there should be an input or the results of the poll."
  [poll]
  [::specs/poll => :re-frame/component]
  (let [cast-votes @(rf/subscribe [:schnaq/vote-cast (:db/id poll)])
        read-only? @(rf/subscribe [:schnaq.selected/read-only?])
        edit-hash @(rf/subscribe [:schnaq/edit-hash])
        voted? (or cast-votes read-only?)
        show-results? (or edit-hash (not (:poll/hide-results? poll)))]
    [:<>
     (if (= :poll.type/ranking (:poll/type poll))
       (if voted?
         (if show-results?
           [ranking-results poll cast-votes]
           [results-hidden-message])
         [ranking-input poll])
       [poll-content poll])
     (when edit-hash
       [show-results-information (:poll/hide-results? poll)])]))

(>defn- ranking-card
  "Show a ranking card."
  [poll]
  [::specs/poll => :re-frame/component]
  [:section.activation-card
   [:div.mx-4.my-2
    [:div.d-flex
     [:h6.pb-2.text-center.mx-auto (:poll/title poll)]
     [dropdown-menu poll]]
    [input-or-results poll]]])

(>defn- poll-card
  "Show a poll card, where users can cast their votes."
  [poll]
  [::specs/poll => :re-frame/component]
  [:section.statement-card
   [:div.mx-4.my-2
    [:div.d-flex
     [:h6.pb-2.text-center.mx-auto (:poll/title poll)]
     [dropdown-menu poll]]
    [poll-content poll]]])

(defn poll-list-item
  "A single item in the poll list. Contains a ranking or poll inside an animation."
  [poll]
  [motion/fade-in-and-out
   (if (= :poll.type/ranking (:poll/type poll))
     [ranking-card poll]
     [poll-card poll])
   motion/card-fade-in-time])

(>defn poll-list
  "Displays all polls of the current schnaq excluding the one in `exclude`."
  [exclude]
  [:db/id :ret (s/coll-of :re-frame/component)]
  (for [poll (remove #(= exclude (:db/id %)) @(rf/subscribe [:schnaq/polls]))]
    [:article
     {:key (str "poll-card-" (:db/id poll))}
     [poll-list-item poll]]))

(defn- poll-option
  "Returns a single option component. Can contain a button for removal of said component."
  [placeholder rank]
  [inputs/text placeholder {:id (str "poll-option-" rank)
                            :required true}])

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

(defn- extract-poll-from-form
  "Extract information from a poll form."
  [form option-count]
  (let [poll-type (case (oget form :radio-type-choice :value)
                    "multiple" :poll.type/multiple-choice
                    "single" :poll.type/single-choice
                    "ranking" :poll.type/ranking)
        options (mapv #(oget+ form (str "poll-option-" %) :value)
                      (range 1 (inc option-count)))
        hide-results? (oget form :hide-results? :checked)]
    {:title (oget form :poll-topic :value)
     :poll-type poll-type
     :options options
     :hide-results? hide-results?}))

(defn poll-form
  "Input form to create a poll with multiple options."
  []
  (let [option-count @(rf/subscribe [:polls.create/option-count])]
    [:form.pt-2
     {:on-key-down (fn [event] (when (= "Enter" (oget event :key))
                                 (.preventDefault event)
                                 false))
      :on-submit (fn [event]
                   (.preventDefault event)
                   (let [form (oget event [:target :elements])]
                     (rf/dispatch [:schnaq.poll/create (extract-poll-from-form form option-count)])
                     (rf/dispatch [:form/should-clear form])))}
     [:div.mb-3
      [:p (labels :schnaq.poll.create/topic-label)]
      [inputs/floating (labels :schnaq.poll.create/placeholder) :poll-topic {:required true :autoFocus true}]
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

     [:section.py-3
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
      [:div.form-check.form-check-inline.pb-1
       [:input#radio-ranking-choice.form-check-input
        {:type "radio"
         :name :radio-type-choice
         :value :ranking}]
       [:label.form-check-label
        {:for :radio-ranking-choice} (labels :schnaq.poll.create/ranking-label)]]
      [inputs/checkbox
       [:<>
        (labels :schnaq.poll.create.hide-results/label)
        [common/info-icon-with-tooltip (labels :schnaq.poll.create.hide-results/info)]]
       :hide-results?]]

     [:div.text-center.pt-2
      [:button.btn.btn-primary.w-75
       {:type "submit"
        :on-click #(matomo/track-event "Active User" "Action" "Create Poll")}
       (labels :schnaq.poll.create/submit-button)]]]))

(rf/reg-event-fx
 :schnaq.poll/create
 (fn [{:keys [db]} [_ poll]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         params (assoc poll
                       :share-hash share-hash
                       :edit-hash (get-in db [:schnaqs :admin-access share-hash]))]
     {:fx [(http/xhrio-request db :post "/poll"
                               [:schnaq.poll.create/success]
                               params)]})))

(rf/reg-event-fx
 :schnaq.poll/cast-vote
 (fn [{:keys [db]} [_ form-elements poll]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         single-choice? (= :poll.type/single-choice (:poll/type poll))
         poll-id (:db/id poll)
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
                             %))
         updated-poll (update poll :poll/options #(mapv poll-update-fn %))]
     {:db (-> db
              (assoc-in [:schnaq :polls poll-id] updated-poll)
              (assoc-in [:polls :past-votes poll-id] chosen-option))
      :fx [(http/xhrio-request db :put (gstring/format "/poll/%s/vote" poll-id)
                               [:schnaq.poll.cast-vote/success]
                               {:share-hash share-hash
                                :option-id chosen-option}
                               [:schnaq.poll.cast-vote/failure poll-id])]})))

(rf/reg-event-fx
 :schnaq.ranking/cast-vote
 (fn [{:keys [db]} [_ poll rankings]]
   (let [poll-id (:db/id poll)
         share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         poll-update-fn #(let [weighted-options
                               (->> (range (count (:poll/options poll)) 0 -1)
                                    (interleave rankings)
                                    (apply hash-map))]
                           (if (contains? (set rankings) (:db/id %))
                             (update % :option/votes + (weighted-options (:db/id %)))
                             %))
         updated-poll (update poll :poll/options #(mapv poll-update-fn %))]
     {:db (-> db
              (assoc-in [:schnaq :polls poll-id] updated-poll)
              (assoc-in [:polls :past-votes poll-id] rankings))
      :fx [(http/xhrio-request db :put (gstring/format "/poll/%s/vote" poll-id)
                               [:schnaq.poll.cast-vote/success]
                               {:share-hash share-hash
                                :option-id rankings}
                               [:schnaq.poll.cast-vote/failure poll-id])]})))

(rf/reg-event-fx
 :schnaq.poll.cast-vote/success
 (fn [{:keys [db]} _]
   {:fx [[:localstorage/assoc [:poll/cast-votes (get-in db [:polls :past-votes])]]]}))

(rf/reg-event-db
 :schnaq.poll.cast-vote/failure
 (fn [db [_ poll-id]]
   (update-in db [:polls :past-votes] dissoc poll-id)))

(rf/reg-event-fx
 :schnaq.poll.create/success
 (fn [{:keys [db]} [_ {:keys [new-poll]}]]
   {:db (-> db
            (assoc-in [:schnaq :polls (:db/id new-poll)] new-poll)
            (tools/new-activation-focus (:db/id new-poll)))
    :fx [[:dispatch [:polls.create/reset-option-count]]]}))

(rf/reg-sub
 :schnaq/poll
 (fn [db [_ poll-id]]
   (get-in db [:schnaq :polls poll-id])))

(rf/reg-sub
 :schnaq/polls
 ;; Returns all polls of the selected schnaq.
 (fn [db _]
   (vals (get-in db [:schnaq :polls] {}))))

(rf/reg-event-fx
 :schnaq.poll/hide-results
 (fn [{:keys [db]} [_ poll-id hide-results?]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:db (assoc-in db [:schnaq :polls poll-id :poll/hide-results?] hide-results?)
      :fx [(http/xhrio-request db :put "/poll/hide-results"
                               [:schnaq.poll.hide-results/success hide-results?]
                               {:share-hash share-hash
                                :edit-hash (get-in db [:schnaqs :admin-access share-hash])
                                :poll-id poll-id
                                :hide-results? hide-results?})]})))

(rf/reg-event-fx
 :schnaq.poll.hide-results/success
 (fn [_ [_ hide-results?]]
   {:fx [[:dispatch
          [:notification/add
           #:notification{:title (labels :schnaq.poll.hide-results.notification/title)
                          :body (labels (if hide-results?
                                          :schnaq.poll.hide-results.notification/body-hide
                                          :schnaq.poll.hide-results.notification/body-show))
                          :context :success}]]]}))

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
   (when-let [poll-id (:db/id poll)]
     (-> db
         (assoc-in [:schnaq :polls poll-id] poll)
         (assoc-in [:present :poll] poll-id)))))

(rf/reg-sub
 :schnaq/vote-cast
 ;; Show whether and what a user has already voted for a certain poll
 (fn [db [_ poll-id]]
   (get-in db [:polls :past-votes poll-id])))

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
     (assoc-in db [:schnaq :polls] (shared-tools/normalize :db/id polls)))))

(rf/reg-event-db
 :schnaq.polls/load-past-votes
 ;; Load past votes from localstorage
 (fn [db _]
   (assoc-in db [:polls :past-votes] (:poll/cast-votes local-storage))))

(rf/reg-event-fx
 :poll/delete
 (fn [{:keys [db]} [_ poll-id]]
   {:db (update-in db [:schnaq :polls] dissoc poll-id)
    :fx [(http/xhrio-request
          db :delete "/poll/delete"
          [:no-op]
          {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])
           :edit-hash (get-in db [:schnaq :selected :discussion/edit-hash])
           :poll-id poll-id})]}))

(rf/reg-sub
 :schnaq.ranking/selected-options
 (fn [db [_ poll-id]]
   (get-in db [:schnaq :current :rankings :selected-options poll-id])))

(rf/reg-event-db
 :schnaq.ranking/add-selected-options!
 (fn [db [_ poll-id index option]]
   (assoc-in db [:schnaq :current :rankings :selected-options poll-id index] option)))

(rf/reg-event-db
 :schnaq.ranking/delete-vote
 (fn [db [_ poll-id index]]
   (update-in db [:schnaq :current :rankings :selected-options poll-id] dissoc index)))
