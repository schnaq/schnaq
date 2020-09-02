(ns schnaq.interface.views.discussion.discussion
  (:require [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [ajax.core :as ajax]
            [oops.core :refer [oget]]
            [vimsical.re-frame.cofx.inject :as inject]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.discussion.logic :as logic]
            [schnaq.interface.views.discussion.view-elements :as view]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.base :as base]))

(defn- add-starting-premises-form
  "Either support or attack a starting-conclusion with the users own premise."
  []
  (let [all-steps @(rf/subscribe [:discussion-steps])
        all-args @(rf/subscribe [:discussion-step-args])
        new-statement-args (logic/args-for-reaction all-steps all-args :starting-support/new)]
    [:form
     {:on-submit (fn [e] (js-wrap/prevent-default e)
                   (logic/submit-new-starting-premise new-statement-args (oget e [:target :elements])))}
     ;; radio support
     [view/radio-button "for-radio-starting" "premise-choice" "for-radio" :discussion/add-premise-supporting true]
     ;; radio attack
     [view/radio-button "against-radio-starting" "premise-choice" "against-radio" :discussion/add-premise-against false]
     ;; input form
     [view/input-form]]))

(defn- add-premise-form
  "Either support or attack or undercut with the users own premise."
  []
  (let [all-steps @(rf/subscribe [:discussion-steps])
        all-args @(rf/subscribe [:discussion-step-args])
        support-args (logic/args-for-reaction all-steps all-args :support/new)
        rebut-args (logic/args-for-reaction all-steps all-args :rebut/new)
        undercut-args (logic/args-for-reaction all-steps all-args :undercut/new)]
    [:form
     {:on-submit (fn [e] (js-wrap/prevent-default e)
                   (logic/submit-new-premise [support-args rebut-args undercut-args] (oget e [:target :elements])))}
     ;; support
     [view/radio-button "for-radio" "premise-choice" "for-radio" :discussion/add-premise-supporting true]
     ;; attack
     [view/radio-button "against-radio" "premise-choice" "against-radio" :discussion/add-premise-against false]
     ;; undercut
     [view/radio-button "undercut-radio" "premise-choice" "undercut-radio" :discussion/add-undercut false]
     ;; input form
     [view/input-form]]))

(defn- other-premises-view [premises]
  [:div.container.px-0
   [:div#other-premises.others-say-container.inner-shadow-custom
    (when (not-empty premises)
      [view/premises-carousel premises])]])

(defn- interaction-view
  "A view where the user interacts with statements"
  [allow-new? premises input]
  [:div
   [other-premises-view premises]
   (when allow-new?
     [view/input-footer input])])

(defn- select-or-react-view
  "A view where the user either reacts to a premise or selects another reaction."
  []
  (let [allow-new? @(rf/subscribe [:allow-rebut-support?])
        premises @(rf/subscribe [:premises-and-undercuts-to-select])]
    [interaction-view allow-new? premises [add-premise-form]]))

(defn- starting-premises-view
  "Show the premises after starting-conclusions. This view is different from usual premises,
  since we can't allow undercuts."
  []
  (let [allow-new? @(rf/subscribe [:allow-rebut-support?])
        premises @(rf/subscribe [:premises-to-select])]
    [interaction-view allow-new? premises [add-starting-premises-form]]))

(defn- discussion-base-page
  "Base discussion view containing a nav header, meeting title and content container."
  [meeting content]
  [:div
   [base/nav-header]
   [view/discussion-header-no-subtitle meeting]
   [:div.container.discussion-base
    [:div.discussion-view-rounded.shadow-custom
     content]]])

(defn discussion-start-view
  "The first step after starting a discussion."
  []
  (let [current-meeting @(rf/subscribe [:meeting/selected])]
    [discussion-base-page current-meeting
     [:div
      [:div.discussion-view-rounded.shadow-custom
       [view/agenda-header-back-arrow]
       [view/history-view]
       [view/conclusions-list]
       [view/input-field]]]]))

(defn discussion-start-view-entrypoint []
  [discussion-start-view])

(defn discussion-loop-view
  "The view that is shown when the discussion goes on after the bootstrap.
  This view dispatches to the correct discussion-steps sub-views."
  []
  (let [steps @(rf/subscribe [:discussion-steps])
        current-meeting @(rf/subscribe [:meeting/selected])]
    [discussion-base-page current-meeting
     [:div
      ;; discussion header
      [view/agenda-header-back-arrow #(rf/dispatch [:discussion.history/time-travel])]
      [view/history-view]
      [view/conclusions-list]
      ;; disussion loop
      [:div#discussion-loop
       (case (logic/deduce-step steps)
         :starting-conclusions/select [starting-premises-view]
         :select-or-react [select-or-react-view]
         :default [:p ""])]]]))

(defn discussion-loop-view-entrypoint []
  [discussion-loop-view])

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
            {:keys [id share-hash]} (get-in db [:current-route :parameters :path])]
        (if (>= 0 keep-n)
          {:dispatch-n [[:discussion.history/clear]
                        [:navigation/navigate :routes.discussion/start {:id id
                                                                        :share-hash share-hash}]]}
          {:db (assoc-in db [:history :full-context] after-time-travel)
           :dispatch [:set-current-discussion-steps (:options (nth before-time-travel keep-n))]})))))

(rf/reg-event-fx
  :start-discussion
  (fn [{:keys [db]} _]
    (let [{:keys [id share-hash]} (get-in db [:current-route :parameters :path])
          username (get-in db [:user :name] "Anonymous")]
      {:http-xhrio {:method :get
                    :uri (str (:rest-backend config) "/start-discussion/" id)
                    :format (ajax/transit-request-format)
                    :url-params {:username username
                                 :meeting-hash share-hash}
                    :response-format (ajax/transit-response-format)
                    :on-success [:set-current-discussion-steps]
                    :on-failure [:ajax-failure]}})))

(rf/reg-event-fx
  :set-current-discussion-steps
  (fn [{:keys [db]} [_ response]]
    {:db (-> db
             (assoc-in [:discussion :options :all] response)
             (assoc-in [:discussion :options :steps] (map first response))
             (assoc-in [:discussion :options :args] (map second response)))
     :dispatch [:notification/new-content]}))

;; This and the following events serve as the multimethod-equivalent in the frontend
;; for stepping through the discussion.
(rf/reg-event-fx
  :continue-discussion
  (fn [_ [_ reaction args]]
    {:dispatch [reaction args]}))

(rf/reg-event-fx
  :starting-argument/new
  (fn [{:keys [db]} [reaction form]]
    (let [{:keys [id share-hash]} (get-in db [:current-route :parameters :path])
          conclusion-text (oget form [:conclusion-text :value])
          premise-text (oget form [:premise-text :value])
          reaction-args
          (logic/args-for-reaction (-> db :discussion :options :steps)
                                   (-> db :discussion :options :args) :starting-argument/new)
          updated-args
          (-> reaction-args
              (assoc :new/starting-argument-conclusion conclusion-text)
              (assoc :new/starting-argument-premises premise-text))]
      {:dispatch-n [[:added/new-content]
                    [:continue-discussion-http-call [reaction updated-args]]
                    [:navigation/navigate :routes.discussion/start {:id id
                                                                    :share-hash share-hash}]]
       :form/clear form})))

(rf/reg-event-fx
  :starting-conclusions/select
  [(rf/inject-cofx ::inject/sub [:discussion-steps])
   (rf/inject-cofx ::inject/sub [:discussion-step-args])]
  (fn [{:keys [discussion-steps discussion-step-args db]} [reaction conclusion]]
    (let [old-args (logic/args-for-reaction discussion-steps discussion-step-args reaction)
          new-args (assoc old-args :statement/selected conclusion)
          options (get-in db [:discussion :options :all])]
      {:dispatch-n [[:discussion.history/push options conclusion :neutral]
                    [:continue-discussion-http-call [reaction new-args]]]})))

(rf/reg-event-fx
  :premises/select
  [(rf/inject-cofx ::inject/sub [:discussion-steps])
   (rf/inject-cofx ::inject/sub [:discussion-step-args])]
  (fn [{:keys [discussion-steps discussion-step-args db]} [reaction premise]]
    (let [old-args (logic/args-for-reaction discussion-steps discussion-step-args reaction)
          new-args (assoc old-args :statement/selected premise)
          attitude (logic/arg-type->attitude (:meta/argument.type premise))
          options (get-in db [:discussion :options :all])]
      {:dispatch-n [[:discussion.history/push options premise attitude]
                    [:continue-discussion-http-call [reaction new-args]]]})))

(rf/reg-event-db
  :added/new-content
  (fn [db _]
    (assoc db :added/new-content? true)))

(rf/reg-event-fx
  :notification/new-content
  (fn [{:keys [db]} _]
    (when (:added/new-content? db)
      {:db (assoc db :added/new-content? false)
       :dispatch [:notification/add
                  #:notification{:title (labels :discussion.notification/new-content-title)
                                 :body (labels :discussion.notification/new-content-body)
                                 :context :success}]})))

(rf/reg-event-fx
  :starting-rebut/new
  (fn [_cofx [reaction args]]
    {:dispatch-n [[:added/new-content]
                  [:continue-discussion-http-call [reaction args]]]}))

(rf/reg-event-fx
  :starting-support/new
  (fn [_cofx [reaction args]]
    {:dispatch-n [[:added/new-content]
                  [:continue-discussion-http-call [reaction args]]]}))

(rf/reg-event-fx
  :rebut/new
  (fn [_cofx [reaction args]]
    {:dispatch-n [[:added/new-content]
                  [:continue-discussion-http-call [reaction args]]]}))

(rf/reg-event-fx
  :support/new
  (fn [_cofx [reaction args]]
    {:dispatch-n [[:added/new-content]
                  [:continue-discussion-http-call [reaction args]]]}))

(rf/reg-event-fx
  :undercut/new
  (fn [_cofx [reaction args]]
    {:dispatch-n [[:added/new-content]
                  [:continue-discussion-http-call [reaction args]]]}))

(rf/reg-event-fx
  :continue-discussion-http-call
  (fn [{:keys [db]} [_ payload]]
    (let [{:keys [id share-hash]} (get-in db [:current-route :parameters :path])]
      {:http-xhrio {:method :post
                    :uri (str (:rest-backend config) "/continue-discussion")
                    :format (ajax/transit-request-format)
                    :params {:payload payload
                             :meeting-hash share-hash
                             :discussion-id id}
                    :response-format (ajax/transit-response-format)
                    :on-success [:set-current-discussion-steps]
                    :on-failure [:ajax-failure]}})))

(rf/reg-event-fx
  :handle-reload-on-discussion-loop
  (fn [{:keys [db]} [_ agenda-id share-hash]]
    (when (empty? (get-in db [:discussion :options :steps]))
      {:dispatch [:navigation/navigate :routes.discussion/start {:id agenda-id
                                                                 :share-hash share-hash}]})))

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
        (logic/index-of steps :starting-conclusions/select)
        (nth args)
        :present/conclusions))))

(rf/reg-sub
  :premises-to-select
  :<- [:discussion-steps]
  :<- [:discussion-step-args]
  (fn [[steps args] _]
    (when (some #{:premises/select} steps)
      (->>
        (logic/index-of steps :premises/select)
        (nth args)
        :present/premises))))

(rf/reg-sub
  :premises-and-undercuts-to-select
  :<- [:discussion-steps]
  :<- [:discussion-step-args]
  (fn [[steps args] _]
    (when (some #{:premises/select} steps)
      (let [present-args (nth args (logic/index-of steps :premises/select))]
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

(rf/reg-sub
  :local-votes
  (fn [db _]
    (get db :votes)))