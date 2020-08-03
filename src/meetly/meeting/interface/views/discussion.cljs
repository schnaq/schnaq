(ns meetly.meeting.interface.views.discussion
  (:require [re-frame.core :as rf]
            [meetly.meeting.interface.config :refer [config]]
            [meetly.meeting.interface.text.display-data :refer [labels fa]]
            [ajax.core :as ajax]
            [oops.core :refer [oget]]
            [vimsical.re-frame.cofx.inject :as inject]
            [cljs.pprint :as pp]
            [meetly.meeting.interface.views.base :as base]))

;; #### Helpers ####

(defn- deduce-step
  "Deduces the current discussion-loop step by the available options."
  [options]
  (cond
    (some #{:starting-support/new} options) :starting-conclusions/select
    (some #{:undercut/new} options) :select-or-react
    :else :default))

(defn- index-of
  "Returns the index of the first occurrence of `elem` in `coll` if its present and
  nil if not."
  [coll elem]
  (let [maybe-index (.indexOf coll elem)]
    (if (= maybe-index -1)
      nil
      maybe-index)))

(defn- args-for-reaction
  "Returns the args for a certain reaction."
  [all-steps all-args reaction]
  (nth all-args (index-of all-steps reaction)))

(defn- arg-type->attitude
  "Returns an attitude deduced from an argument-type."
  [arg-type]
  (cond
    (#{:argument.type/attack :argument.type/undercut} arg-type) "disagree"
    (#{:argument.type/support} arg-type) "agree"))

(defn- submit-new-starting-premise
  "Takes arguments and a form input and calls the next step in the discussion."
  [current-args form]
  (let [new-text (oget form [:premise-text :value])
        choice (oget form [:premise-choice :value])
        [reaction key-name] (if (= choice "against-radio")
                              [:starting-rebut/new :new/rebut-premise]
                              [:starting-support/new :new/support-premise])]
    (rf/dispatch [:continue-discussion reaction (assoc current-args key-name new-text)])))

(defn- submit-new-premise
  "Submits a newly created premise as an undercut, rebut or support."
  [[support-args rebut-args undercut-args] form]
  (let [new-text (oget form [:premise-text :value])
        choice (oget form [:premise-choice :value])]
    (case choice
      "against-radio" (rf/dispatch [:continue-discussion :rebut/new (assoc rebut-args :new/rebut new-text)])
      "for-radio" (rf/dispatch [:continue-discussion :support/new (assoc support-args :new/support new-text)])
      "undercut-radio" (rf/dispatch [:continue-discussion :undercut/new (assoc undercut-args :new/undercut new-text)]))))

;; discussion header

(defn- discussion-header [current-meeting]
  ;; meeting header
  (base/discussion-header
    (:meeting/title current-meeting)
    (:meeting/description current-meeting)
    (fn []
      (rf/dispatch [:navigate :routes/meetings.show
                    {:share-hash (:meeting/share-hash current-meeting)}])
      (rf/dispatch [:select-current-meeting current-meeting]))))

;; #### Views ####

(defn- statement-bubble
  "A single bubble of a statement to be used ubiquitously."
  ([statement]
   (statement-bubble statement (arg-type->attitude (:meta/argument.type statement))))
  ([{:keys [statement/content] :as statement} attitude]
   [:div.card.statement {:class (str "statement-" (name attitude))}
    (when (= :argument.type/undercut (:meta/argument.type statement))
      [:p.small.text-muted (labels :discussion/undercut-bubble-intro)])
    [:small.text-right (-> statement :statement/author :author/nickname)]
    [:p content]]))

(defn- history-view
  "Displays the statements it took to get to where the user is."
  []
  (let [history @(rf/subscribe [:discussion-history])
        indexed-history (map-indexed #(vector (- (count history) %1 1) %2) history)]
    [:div#discussion-history
     (for [[count [statement attitude]] indexed-history]
       [:div {:key (:db/id statement)
              :on-click #(rf/dispatch [:discussion.history/time-travel count])}
        [statement-bubble statement attitude]])]))

(defn- single-statement-view
  "Displays a single starting conclusion-statement inside a discussion."
  [statement discussion-id]
  (let [meeting @(rf/subscribe [:selected-meeting])]
    [:div.card.statement-single.shadow-custom {:on-click (fn [_e]
                                                           (rf/dispatch [:continue-discussion :starting-conclusions/select statement])
                                                           (rf/dispatch [:navigate :routes/meetings.discussion.continue
                                                                         {:id discussion-id
                                                                          :share-hash (:meeting/share-hash meeting)}]))}
     [:small.text-right (-> statement :statement/author :author/nickname)]
     [:p (:statement/content statement)]]))

(defn- input-starting-argument-form
  "A form, which allows the input of a complete argument.
  (Premise and Conclusion as statements)"
  []
  [:form
   {:on-submit (fn [e] (.preventDefault e)
                 (rf/dispatch [:continue-discussion :starting-argument/new
                               (oget e [:target :elements])]))}
   [:input.form-control.discussion-text-input.mb-1
    {:type "text" :name "conclusion-text"
     :auto-complete "off"
     :placeholder (labels :discussion/add-argument-conclusion-placeholder)}]
   [:br]
   [:input.form-control.discussion-text-input.mb-1
    {:type "text" :name "premise-text"
     :auto-complete "off"
     :placeholder (labels :discussion/add-argument-premise-placeholder)}]
   [:div.text-center.button-spacing-top
    [:button.btn.button-secondary {:type "submit"} (labels :discussion/create-argument-action)]]])


(defn- agenda-header-back-arrow [on-click-back-function]
  (let [agenda @(rf/subscribe [:chosen-agenda])]
    [:div.discussion-view-top-rounded
     [:div.row
      ;; back arrow
      [:div.col-lg-1.back-arrow
       (when on-click-back-function
         [:i.arrow-icon {:class (str "m-auto fas " (fa :arrow-left))
                         :on-click on-click-back-function}])]
      ;; title
      [:div.col-lg-11
       [:div
        [:h2 (:agenda/title agenda)]
        [:p (:agenda/description agenda)]]]]]))

(defn- conclusions-list []
  (let [agenda @(rf/subscribe [:chosen-agenda])
        conclusions @(rf/subscribe [:starting-conclusions])]
    [:div#conclusions-list.container
     (for [conclusion conclusions]
       [:div {:key (:statement/content conclusion)}
        [single-statement-view conclusion (-> agenda :agenda/discussion-id :db/id)]])]))

(defn- input-field []
  (let [allow-new-argument? @(rf/subscribe [:allow-new-argument?])]
    [:div.discussion-view-bottom-rounded
     (when allow-new-argument?
       [:div
        [:h5 (labels :discussion/create-argument-heading)]
        [:br]
        [input-starting-argument-form]]
       )]))

(defn- input-footer [content]
  [:div.discussion-view-bottom-rounded
   content])


(defn discussion-start-view
  "The first step after starting a discussion."
  []
  (let [current-meeting @(rf/subscribe [:selected-meeting])]
    [:div
     [discussion-header current-meeting]
     [:br]
     [:div.container.discussion-view-rounded.shadow-custom
      [agenda-header-back-arrow]
      [history-view]
      [conclusions-list]
      [input-field]]]))


(defn- radio-button
  "Radio Button helper function. This function creates a radio button."
  [id name value label checked?]
  [:div.custom-control.custom-radio
   [:input.custom-control-input.custom-radio-button
    {:type "radio"
     :id id
     :name name
     :value value
     :default-checked checked?}]
   [:label.custom-control-label.custom-radio-button-label.clickable
    {:for id} (labels label)]])


(defn- input-form
  "Text input for adding a statement"
  []
  [:div
   [:input.form-control.discussion-text-input.mb-1
    {:type "text" :name "premise-text"
     :auto-complete "off"
     :placeholder (labels :discussion/premise-placeholder)}]
   ;; add button
   [:div.text-center.button-spacing-top
    [:button.btn.button-secondary {:type "submit"} (labels :discussion/create-starting-premise-action)]]])

(defn- add-starting-premises-form
  "Either support or attack a starting-conclusion with the users own premise."
  []
  (let [all-steps @(rf/subscribe [:discussion-steps])
        all-args @(rf/subscribe [:discussion-step-args])
        new-statement-args (args-for-reaction all-steps all-args :starting-support/new)]
    [:form
     {:on-submit (fn [e] (.preventDefault e)
                   (submit-new-starting-premise new-statement-args (oget e [:target :elements])))}
     ;; radio support
     [radio-button "for-radio-starting" "premise-choice" "for-radio" :discussion/add-premise-supporting true]
     ;; radio attack
     [radio-button "against-radio-starting" "premise-choice" "against-radio" :discussion/add-premise-against false]
     ;; spacing
     [:br]
     ;; input form
     [input-form]]))

(defn- add-premise-form
  "Either support or attack or undercut with the users own premise."
  []
  (let [all-steps @(rf/subscribe [:discussion-steps])
        all-args @(rf/subscribe [:discussion-step-args])
        support-args (args-for-reaction all-steps all-args :support/new)
        rebut-args (args-for-reaction all-steps all-args :rebut/new)
        undercut-args (args-for-reaction all-steps all-args :undercut/new)]
    [:form
     {:on-submit (fn [e] (.preventDefault e)
                   (submit-new-premise [support-args rebut-args undercut-args] (oget e [:target :elements])))}
     ;; support
     [radio-button "for-radio" "premise-choice" "for-radio" :discussion/add-premise-supporting true]
     ;; attack
     [radio-button "against-radio" "premise-choice" "against-radio" :discussion/add-premise-against false]
     ;; undercut
     [radio-button "undercut-radio" "premise-choice" "undercut-radio" :discussion/add-undercut false]
     ;; spacing
     [:br]
     ;; input form
     [input-form]]))


;; carousel

(defn- premises-carousel [premises]

  [:div#carouselExampleIndicators.carousel.slide {:data-ride "carousel"}
   ;; indicator
   [:ol.carousel-indicators.carousel-indicator-custom
    ;; range of number of premises and set the first element as selected
    (map
      (fn [i]
        (let [params {:key (str "indicator-" i) :data-target "#carouselExampleIndicators" :data-slide-to (str i)}]
          (if (= i 0)
            [:li.active params]
            [:li params])))
      (range (count premises)))]
   ;; content
   [:div.carousel-inner
    ;; set first indexed element as selected
    (map-indexed
      (fn [index premise]
        (let [params {:key (:db/id premise)}
              content [:div.premise-carousel-item
                       {:on-click #(rf/dispatch [:continue-discussion :premises/select premise])}
                       [statement-bubble premise]]]
          (if (= index 0)
            [:div.carousel-item.active params content]
            [:div.carousel-item params content])))
      premises)]
   ;; interface elements
   [:a.carousel-control-prev {:href "#carouselExampleIndicators" :role "button" :data-slide "prev"}
    [:span.carousel-control-prev-icon {:aria-hidden "true"}]
    [:span.sr-only "Previous"]]
   [:a.carousel-control-next {:href "#carouselExampleIndicators" :role "button" :data-slide "next"}
    [:span.carousel-control-next-icon {:aria-hidden "true"}]
    [:span.sr-only "Next"]]])


(defn- other-premises-view [premises]
  [:div#other-premises.container.others-say-container.inner-shadow-custom
   (when (not-empty premises)
     [premises-carousel premises])])

(defn- interaction-view
  "A view where the user interacts with statements"
  [allow-new? premises input]
  [:div
   [other-premises-view premises]
   (when allow-new?
     [input-footer input])])

(defn- select-or-react-view
  "A view where the user either reacts to a premise or selects another reaction."
  []
  (let [allow-new? @(rf/subscribe [:allow-rebut-support?])
        premises @(rf/subscribe [:premises-and-undercuts-to-select])]
    (interaction-view allow-new? premises [add-premise-form])))

(defn- starting-premises-view
  "Show the premises after starting-conclusions. This view is different from usual premises,
  since we can't allow undercuts."
  []
  (let [allow-new? @(rf/subscribe [:allow-rebut-support?])
        premises @(rf/subscribe [:premises-to-select])]
    (interaction-view allow-new? premises [add-starting-premises-form])))

(defn discussion-loop-view
  "The view that is shown when the discussion goes on after the bootstrap.
  This view dispatches to the correct discussion-steps sub-views."
  []

  (let [steps @(rf/subscribe [:discussion-steps])
        current-meeting @(rf/subscribe [:selected-meeting])]
    [:div
     [discussion-header current-meeting]
     [:br]
     [:div.container.discussion-view-rounded.shadow-custom
      ;; discussion header
      [agenda-header-back-arrow #(rf/dispatch [:discussion.history/time-travel])]
      [history-view]
      [conclusions-list]
      ;; disussion loop
      [:div#discussion-loop
       (case (deduce-step steps)
         :starting-conclusions/select [starting-premises-view]
         :select-or-react [select-or-react-view]
         :default [:p ""])]]]))

;; #### Events ####

(rf/reg-event-db
  :discussion.history/push
  (fn [db [_ steps statement attitude]]
    (let [newest-entry (-> db :history :full-context :statement peek first)]
      (if (and statement (not= newest-entry statement))
        (update-in db [:history :full-context] conj {:statement [statement attitude]
                                                     :options steps})
        db))))

(rf/reg-event-db
  :discussion.history/clear
  (fn [db _]
    (assoc-in db [:history :full-context] [])))

(rf/reg-event-fx
  :discussion.history/time-travel
  (fn [{:keys [db]} [_ times]]
    ;; Only continue when default value (nil - go back one step) is set or we go back more than 0 steps
    (when (or (nil? times) (< 0 times))
      (let [steps-back (or times 1)
            before-time-travel (get-in db [:history :full-context])
            keep-n (- (count before-time-travel) steps-back)
            after-time-travel (vec (take keep-n before-time-travel))
            discussion-id (get-in db [:agenda :chosen :agenda/discussion-id :db/id])
            share-hash (get-in db [:meeting :selected :meeting/share-hash])]
        (if (>= 0 keep-n)
          {:dispatch-n [[:discussion.history/clear]
                        [:navigate :routes/meetings.discussion.start {:id discussion-id
                                                                      :share-hash share-hash}]]}
          {:db (assoc-in db [:history :full-context] after-time-travel)
           :dispatch [:set-current-discussion-steps (:options (nth before-time-travel keep-n))]})))))

(rf/reg-event-fx
  :start-discussion
  (fn [{:keys [db]} [_ try-counter]]
    (let [discussion-id (get-in db [:agenda :chosen :agenda/discussion-id :db/id])
          meeting-hash (get-in db [:meeting :selected :meeting/share-hash])
          username (get-in db [:user :name] "Anonymous")
          try-counter (or try-counter 0)]
      (when (< try-counter 10)
        (if (and discussion-id meeting-hash)
          {:http-xhrio {:method :get
                        :uri (str (:rest-backend config) "/start-discussion/" discussion-id)
                        :format (ajax/transit-request-format)
                        :url-params {:username username
                                     :meeting-hash meeting-hash}
                        :response-format (ajax/transit-response-format)
                        :on-success [:set-current-discussion-steps]
                        :on-failure [:ajax-failure]}}
          {:dispatch-later [{:ms 50 :dispatch [:start-discussion (inc try-counter)]}]})))))

(rf/reg-event-db
  :set-current-discussion-steps
  (fn [db [_ response]]
    (pp/pprint response)
    (-> db
        (assoc-in [:discussion :options :all] response)
        (assoc-in [:discussion :options :steps] (map first response))
        (assoc-in [:discussion :options :args] (map second response)))))

;; This and the following events serve as the multimethod-equivalent in the frontend
;; for stepping through the discussion.
(rf/reg-event-fx
  :continue-discussion
  (fn [_ [_ reaction args]]
    (println "Continue Discussion: " reaction)
    (println args)
    {:dispatch [reaction args]}))

(rf/reg-event-fx
  :starting-argument/new
  (fn [{:keys [db]} [reaction form]]
    (let [discussion-id (-> db :agenda :chosen :agenda/discussion-id :db/id)
          share-hash (get-in db [:meeting :selected :meeting/share-hash])
          conclusion-text (oget form [:conclusion-text :value])
          premise-text (oget form [:premise-text :value])
          reaction-args
          (args-for-reaction (-> db :discussion :options :steps)
                             (-> db :discussion :options :args) :starting-argument/new)
          updated-args
          (-> reaction-args
              (assoc :new/starting-argument-conclusion conclusion-text)
              (assoc :new/starting-argument-premises premise-text))]
      {:dispatch-n [[:continue-discussion-http-call [reaction updated-args]]
                    [:navigate :routes/meetings.discussion.start {:id discussion-id
                                                                  :share-hash share-hash}]]})))

(rf/reg-event-fx
  :starting-conclusions/select
  [(rf/inject-cofx ::inject/sub [:discussion-steps])
   (rf/inject-cofx ::inject/sub [:discussion-step-args])]
  (fn [{:keys [discussion-steps discussion-step-args db]} [reaction conclusion]]
    (let [old-args (args-for-reaction discussion-steps discussion-step-args reaction)
          new-args (assoc old-args :statement/selected conclusion)
          options (get-in db [:discussion :options :all])]
      {:dispatch-n [[:discussion.history/push options conclusion :neutral]
                    [:continue-discussion-http-call [reaction new-args]]]})))

(rf/reg-event-fx
  :premises/select
  [(rf/inject-cofx ::inject/sub [:discussion-steps])
   (rf/inject-cofx ::inject/sub [:discussion-step-args])]
  (fn [{:keys [discussion-steps discussion-step-args db]} [reaction premise]]
    (let [old-args (args-for-reaction discussion-steps discussion-step-args reaction)
          new-args (assoc old-args :statement/selected premise)
          attitude (arg-type->attitude (:meta/argument.type premise))
          options (get-in db [:discussion :options :all])]
      {:dispatch-n [[:discussion.history/push options premise attitude]
                    [:continue-discussion-http-call [reaction new-args]]]})))

(rf/reg-event-fx
  :starting-rebut/new
  (fn [_cofx [reaction args]]
    {:dispatch [:continue-discussion-http-call [reaction args]]}))

(rf/reg-event-fx
  :starting-support/new
  (fn [_cofx [reaction args]]
    {:dispatch [:continue-discussion-http-call [reaction args]]}))

(rf/reg-event-fx
  :rebut/new
  (fn [_cofx [reaction args]]
    {:dispatch [:continue-discussion-http-call [reaction args]]}))

(rf/reg-event-fx
  :support/new
  (fn [_cofx [reaction args]]
    {:dispatch [:continue-discussion-http-call [reaction args]]}))

(rf/reg-event-fx
  :undercut/new
  (fn [_cofx [reaction args]]
    {:dispatch [:continue-discussion-http-call [reaction args]]}))

(rf/reg-event-fx
  :continue-discussion-http-call
  (fn [{:keys [db]} [_ payload]]
    (let [meeting-hash (get-in db [:meeting :selected :meeting/share-hash])
          discussion-id (get-in db [:agenda :chosen :agenda/discussion-id :db/id])]
      {:http-xhrio {:method :post
                    :uri (str (:rest-backend config) "/continue-discussion")
                    :format (ajax/transit-request-format)
                    :params {:payload payload
                             :meeting-hash meeting-hash
                             :discussion-id discussion-id}
                    :response-format (ajax/transit-response-format)
                    :on-success [:set-current-discussion-steps]
                    :on-failure [:ajax-failure]}})))

;; #### Subs ####

(rf/reg-sub
  :discussion-options
  (fn [db _]
    (get-in db [:discussion :options :all])))

(rf/reg-sub
  :discussion-steps
  (fn [db _]
    (get-in db [:discussion :options :steps])))

(rf/reg-sub
  :discussion-step-args
  (fn [db _]
    (get-in db [:discussion :options :args])))

(rf/reg-sub
  :starting-conclusions
  :<- [:discussion-steps]
  :<- [:discussion-step-args]
  (fn [[steps args] _]
    (when (some #(= % :starting-conclusions/select) steps)
      (->>
        (index-of steps :starting-conclusions/select)
        (nth args)
        :present/conclusions))))

(rf/reg-sub
  :premises-to-select
  :<- [:discussion-steps]
  :<- [:discussion-step-args]
  (fn [[steps args] _]
    (when (some #{:premises/select} steps)
      (->>
        (index-of steps :premises/select)
        (nth args)
        :present/premises))))

(rf/reg-sub
  :premises-and-undercuts-to-select
  :<- [:discussion-steps]
  :<- [:discussion-step-args]
  (fn [[steps args] _]
    (when (some #{:premises/select} steps)
      (let [present-args (nth args (index-of steps :premises/select))]
        (concat (:present/premises present-args)
                (:present/undercuts present-args))))))

(rf/reg-sub
  :allow-new-argument?
  :<- [:discussion-steps]
  (fn [steps]
    (some #(= % :starting-argument/new) steps)))

(rf/reg-sub
  :allow-rebut-support?
  :<- [:discussion-steps]
  (fn [steps _]
    (some #{:starting-support/new :support/new} steps)))

(rf/reg-sub
  :discussion-history/full
  (fn [db _]
    (get-in db [:history :full-context])))

(rf/reg-sub
  :discussion-history
  :<- [:discussion-history/full]
  (fn [history _]
    (map :statement history)))