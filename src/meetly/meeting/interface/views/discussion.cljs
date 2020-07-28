(ns meetly.meeting.interface.views.discussion
  (:require [re-frame.core :as rf]
            [meetly.meeting.interface.config :refer [config]]
            [meetly.meeting.interface.text.display-data :refer [labels]]
            [ajax.core :as ajax]
            [oops.core :refer [oget]]
            [vimsical.re-frame.cofx.inject :as inject]
            [cljs.pprint :as pp]))

;; #### Helpers ####

(defn- deduce-step
  "Deduces the current discussion-loop step by the available options."
  [options]
  (cond
    (some #{:reaction/support} options) :reactions/present
    (some #{:starting-support/new} options) :starting-conclusions/select
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

;; #### Views ####

(defn- statement-bubble
  "A single bubble of a statement to be used ubiquitously."
  ;; TODO finish this
  [statement]
  [:div {:key (:db/id statement)
         :class (str "statement-" (name :todo))}
   [:p (:statement/content statement)]
   [:p.small.text-muted "By: " (-> statement :statement/author :author/nickname)]])

(defn- history-view
  "Displays the statements it took to get to where the user is."
  []
  (let [history @(rf/subscribe [:discussion-history])]
    [:div.discussion-history
     (for [[statement attitude] history]
       [:div {:key (:db/id statement)
              :class (str "statement-" (name attitude))}
        [:p (:statement/content statement)]
        [:p.small.text-muted "By: " (-> statement :statement/author :author/nickname)]])
     [:hr]]))

(defn- discussion-base
  "The base template of the discussion"
  [content]
  [:div.container
   [history-view]
   content])

(defn- single-statement-view
  "Displays a single starting conclusion-statement inside a discussion."
  [statement discussion-id]
  [:div.card {:style {:background-color "#6aadb8"
                      :width "600px"}
              :on-click (fn [_e]
                          (rf/dispatch [:continue-discussion :starting-conclusions/select statement])
                          (rf/dispatch [:navigate :routes/meetings.discussion.continue
                                        {:id discussion-id}]))}
   [:p (:statement/content statement)]
   [:small "Written by: " (-> statement :statement/author :author/nickname)]])

(defn- input-starting-argument-form
  "A form, which allows the input of a complete argument.
  (Premise and Conclusion as statements)"
  []
  [:form
   {:on-submit (fn [e] (.preventDefault e)
                 (rf/dispatch [:continue-discussion :starting-argument/new
                               (oget e [:target :elements])]))}
   [:input.form-control.mb-1
    {:type "text" :name "conclusion-text"
     :placeholder (labels :discussion/add-argument-conclusion-placeholder)}]
   [:input.form-control.mb-1
    {:type "text" :name "premise-text"
     :placeholder (labels :discussion/add-argument-premise-placeholder)}]
   [:button.btn.btn-primary {:type "submit"} (labels :discussion/create-argument-action)]])

(defn- all-positions-view
  "Shows a nice header and all starting-conclusions."
  []
  (let [agenda @(rf/subscribe [:chosen-agenda])
        conclusions @(rf/subscribe [:starting-conclusions])]
    [:div.row.discussion-head
     [:div.col-12
      [:h2 (:agenda/title agenda)]
      [:p (:agenda/description agenda)]
      [:hr]
      (for [conclusion conclusions]
        [:div {:key (:statement/content conclusion)}
         [single-statement-view conclusion (-> agenda :agenda/discussion-id :db/id)]])]]))

(defn discussion-start-view
  "The first step after starting a discussion."
  []
  [discussion-base
   (let [allow-new-argument? @(rf/subscribe [:allow-new-argument?])]
     [:div#discussion-start
      [all-positions-view]
      [:hr]
      (when allow-new-argument?
        [:h3 (labels :discussion/create-argument-heading)]
        [input-starting-argument-form])])])

(defn- reaction-subview
  "Displays a single reaction, based on the input step"
  [step _args]
  (case step
    :reaction/support
    [:p "Ich unterstütze diese Aussage."]
    :reaction/undercut
    [:p "Die Aussage hängt nicht mit dem vorherigen Fakt zusammen."]
    :reaction/rebut
    [:p "Die Konklusion (zweitletzte in History) ist murks."]
    :reaction/undermine
    [:p "Ich habe etwas gegen die letzte Aussage."]
    :reaction/defend
    [:p "Die Konklusion ist super, die will ich weiter unterstützen!"]))

(defn- choose-reaction-view
  "User chooses a reaction regarding some argument."
  []
  (let [options @(rf/subscribe [:discussion-options])]
    [discussion-base
     [:div#reaction-view
      [:p (labels :discussion/reason-nudge)]
      (for [[step args] options]
        [:div {:key step}
         [reaction-subview step args]])]]))

(defn- starting-premises-view
  "Show the premises after starting-conclusions. This view is different from usual premises,
  since we can't allow undercuts."
  []
  (let [allow-new? @(rf/subscribe [:allow-rebut-support?])
        premises @(rf/subscribe [:premises-to-select])]
    [:div
     (for [premise premises]
       [:div.premise
        {:key (:db/id premise)}
        [:p (:statement/content premise)]])
     (when allow-new?
       [:p "Hier wäre ein neues Argument. WENN ICH EINS HÄTTE"])]))

(defn discussion-loop-view
  "The view that is shown when the discussion goes on after the bootstrap.
  This view dispatches to the correct discussion-steps sub-views."
  []
  (let [steps @(rf/subscribe [:discussion-steps])]
    [discussion-base
     [:div#discussion-loop
      (case (deduce-step steps)
        :reactions/present [choose-reaction-view]
        :starting-conclusions/select [starting-premises-view]
        :default [:p ""])]]))

;; #### Events ####

(rf/reg-event-db
  :discussion.history/push
  (fn [db [_ statement attitude]]
    (let [newest-entry (-> db :history :statements peek first)]
      (if (and statement (not= newest-entry statement))
        (update-in db [:history :statements] conj [statement attitude])
        db))))

(rf/reg-event-db
  :discussion.history/clear
  (fn [db _]
    (assoc-in db [:history :statements] [])))

(rf/reg-event-fx
  :start-discussion
  (fn [{:keys [db]} [_ try-counter]]
    (let [discussion-id (-> db :agenda :chosen :agenda/discussion-id :db/id)
          username (get-in db [:user :name] "Anonymous")
          try-counter (or try-counter 0)]
      (when (< try-counter 10)
        (if discussion-id
          {:http-xhrio {:method :get
                        :uri (str (:rest-backend config) "/start-discussion/" discussion-id)
                        :format (ajax/transit-request-format)
                        :url-params {:username username}
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
    {:dispatch [reaction args]}))

(rf/reg-event-fx
  :starting-argument/new
  (fn [{:keys [db]} [reaction form]]
    (let [discussion-id (-> db :agenda :chosen :agenda/discussion-id :db/id)
          conclusion-text (oget form [:conclusion-text :value])
          premise-text (oget form [:premise-text :value])
          reaction-args
          (args-for-reaction (-> db :discussion :options :steps)
                             (-> db :discussion :options :args) :starting-argument/new)
          updated-args
          (-> reaction-args
              (assoc :new/starting-argument-conclusion conclusion-text)
              (assoc :new/starting-argument-premises [premise-text]))]
      {:dispatch-n [[:continue-discussion-http-call [reaction updated-args]]
                    [:navigate :routes/meetings.discussion.start {:id discussion-id}]]})))

(rf/reg-event-fx
  :starting-conclusions/select
  [(rf/inject-cofx ::inject/sub [:discussion-steps])
   (rf/inject-cofx ::inject/sub [:discussion-step-args])]
  (fn [{:keys [discussion-steps discussion-step-args]} [reaction conclusion]]
    (let [old-args (args-for-reaction discussion-steps discussion-step-args reaction)
          new-args (assoc old-args :conclusion/chosen conclusion)]
      {:dispatch-n [[:discussion.history/push conclusion :neutral]
                    [:continue-discussion-http-call [reaction new-args]]]})))

(rf/reg-event-fx
  :continue-discussion-http-call
  (fn [_ [_ payload]]
    {:http-xhrio {:method :post
                  :uri (str (:rest-backend config) "/continue-discussion")
                  :format (ajax/transit-request-format)
                  :params payload
                  :response-format (ajax/transit-response-format)
                  :on-success [:set-current-discussion-steps]
                  :on-failure [:ajax-failure]}}))

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
        :present/premises
        flatten                                             ;;TODO remove this after refactor in dialog.core
        ))))

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
  :discussion-history
  (fn [db _]
    (get-in db [:history :statements])))