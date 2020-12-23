(ns schnaq.interface.views.discussion.card-elements
  (:require [ajax.core :as ajax]
            [goog.dom :as gdom]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels img-path fa]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.brainstorm.tools :as btools]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.discussion.conclusion-card :as cards]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.discussion.logic :as logic]
            [schnaq.interface.views.meeting.admin-buttons :as admin-buttons]))

(defn history-view
  "History view displayed in the left column in the desktop view."
  ([{:meeting/keys [share-hash] :as current-meeting}]
   ;; home button
   [:div.d-inline-block.d-md-block.pr-2.pr-md-0
    [:span
     {:on-click (fn []
                  (rf/dispatch [:navigation/navigate :routes.meeting/show
                                {:share-hash share-hash}])
                  (rf/dispatch [:meeting/select-current current-meeting]))}
     [:i {:class (str "card-history-home clickable mt-md-4 fas " (fa :home))}]]])
  ([current-meeting history]
   (let [indexed-history (map-indexed #(vector (- (count history) %1 1) %2) history)]
     [:<>
      ;; home button
      [history-view current-meeting]
      ;; history
      (for [[index statement] indexed-history]
        [:div.d-inline-block.d-md-block.pr-2.pr-md-0.text-dark
         {:key (str "history-" (:db/id statement))}
         (let [attitude (name (logic/arg-type->attitude (:meta/argument-type statement)))]
           [:div.card-history.clickable.mt-md-4
            {:class (str "statement-card-" attitude " mobile-attitude-" attitude)
             :on-click #(rf/dispatch [:discussion.history/time-travel index])}
            [common/avatar (-> statement :statement/author :author/nickname) 42]])])])))

(defn- graph-button
  "Rounded square button to navigate to the graph view"
  [agenda share-hash]
  [:img.graph-icon-img.clickable-no-hover
   {:src (img-path :icon-graph) :alt (labels :graph.button/text)
    :title (labels :graph.button/text)
    :on-click #(rf/dispatch
                 [:navigation/navigate :routes/graph-view
                  {:id (-> agenda :agenda/discussion :db/id)
                   :share-hash share-hash}])}])

(defn settings-element
  "Element containing settings buttons"
  [{:meeting/keys [share-hash title] :as meeting} edit-hash]
  [:div.float-right
   (when (and edit-hash (btools/is-brainstorm? meeting))
     [admin-buttons/admin-center share-hash edit-hash])
   [admin-buttons/txt-export share-hash title]])

(defn tooltip-div
  [tooltip-location tooltip content]
  (reagent/create-class
    {:component-did-mount
     (fn [comp] (js-wrap/tooltip (rdom/dom-node comp)))
     :component-will-unmount
     (fn [comp]
       (js-wrap/tooltip (rdom/dom-node comp) "disable")
       (js-wrap/tooltip (rdom/dom-node comp) "dispose"))
     :reagent-render
     (fn [] [:div.h-100
             {:data-toggle "tooltip"
              :data-placement tooltip-location
              :title tooltip} content])}))

(defn radio-button
  "Radio Button helper function. This function creates a radio button."
  [id radio-name value label hint color-class checked?]
  [:<>
   [:input {:id id :type "radio" :name radio-name :value value :default-checked checked?}]
   [:label.mx-1.my-1 {:class color-class :for id}
    [tooltip-div "bottom" hint label]]])

(defn radio-buttons [textarea-id]
  [:div.radio-toolbar
   [:div.d-flex.flex-row.px-3
    ;; support
    [radio-button
     (str textarea-id "for-radio") "premise-choice" "for-radio"
     [:i {:class (str "m-auto fas " (fa :plus))}]
     (labels :discussion/add-premise-supporting) "hover-primary" false]
    (when-not toolbelt/production?
      ;; neutral
      [radio-button
       (str textarea-id "neutral-radio") "premise-choice" "for-radio"
       [:i {:class (str "m-auto fas " (fa :comment))}]
       (labels :discussion/add-premise-supporting) "hover-white" true])
    ;; attack
    [radio-button
     (str textarea-id "against-radio") "premise-choice" "against-radio"
     [:i {:class (str "m-auto fas " (fa :minus))}]
     (labels :discussion/add-premise-against) "hover-secondary" false]]])

(defn- input-form
  "A basic input form with optional radio buttons"
  [textarea-id textarea-name submit-fn radio-buttons]
  [:form.my-2
   {:on-submit submit-fn}
   [:div.discussion-input-container.w-100
    [:div.d-flex.flex-row
     [:textarea.form-control.discussion-text-input-area.w-100
      {:id textarea-id
       :name textarea-name :wrap "soft" :rows 1
       :auto-complete "off"
       :onInput (fn [_event]
                  ;; first reset input then set height +1px in order to prevent scrolling
                  (let [input (gdom/getElement textarea-id)]
                    (toolbelt/height-to-scrollheight! input)))
       :required true
       :data-dynamic-height true
       :placeholder (labels :discussion/add-argument-conclusion-placeholder)}]
     ;; reaction type
     radio-buttons
     ;; submit icon button
     [:button.primary-icon-button
      {:type "submit"
       :title (labels :discussion/create-argument-action)}
      [:i {:class (str "m-auto fas " (fa :plane))}]]]]])

(defn input-conclusion-form
  "A form, which allows the input of a conclusions"
  [textarea-id]
  (let [submit-fn (fn [e]
                    (js-wrap/prevent-default e)
                    (logic/submit-new-premise (oget e [:target :elements])))]
    [input-form textarea-id "premise-text" submit-fn [radio-buttons textarea-id]]))

(defn input-starting-statement-form
  "A form, which allows the input of a starting-statement."
  [textarea-id]
  (let [submit-fn (fn [e] (js-wrap/prevent-default e)
                    (rf/dispatch [:discussion.add.statement/starting
                                  (oget e [:target :elements])]))]
    [input-form textarea-id "statement-text" submit-fn nil]))

(rf/reg-event-fx
  :discussion.add.statement/starting
  (fn [{:keys [db]} [_ form]]
    (let [{:keys [id share-hash]} (get-in db [:current-route :parameters :path])
          nickname (get-in db [:user :name] "Anonymous")
          statement-text (oget form [:statement-text :value])]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/discussion/statements/starting/add")
                          :format (ajax/transit-request-format)
                          :params {:statement statement-text
                                   :nickname nickname
                                   :share-hash share-hash
                                   :discussion-id id}
                          :response-format (ajax/transit-response-format)
                          :on-success [:discussion.add.statement/starting-success form]
                          :on-failure [:ajax.error/as-notification]}]]})))

(rf/reg-event-fx
  :discussion.add.statement/starting-success
  (fn [_ [_ form new-starting-statements]]
    {:fx [[:dispatch [:notification/add
                      #:notification{:title (labels :discussion.notification/new-content-title)
                                     :body (labels :discussion.notification/new-content-body)
                                     :context :success}]]
          [:dispatch [:discussion.query.conclusions/set-starting new-starting-statements]]
          [:form/clear form]]}))

(rf/reg-event-fx
  :discussion.query.conclusions/starting
  (fn [{:keys [db]} _]
    (let [share-hash (get-in db [:current-route :parameters :path :share-hash])]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/discussion/conclusions/starting")
                          :format (ajax/transit-request-format)
                          :params {:share-hash share-hash}
                          :response-format (ajax/transit-response-format)
                          :on-success [:discussion.query.conclusions/set-starting]
                          :on-failure [:ajax.error/to-console]}]]})))

(rf/reg-event-fx
  :discussion.query.conclusions/set-starting
  (fn [{:keys [db]} [_ {:keys [starting-conclusions]}]]
    {:db (assoc-in db [:discussion :conclusions :starting] starting-conclusions)
     :fx [[:dispatch [:votes.local/reset]]]}))

(rf/reg-event-db
  :votes.local/reset
  (fn [db _]
    (assoc db :votes {:up {}
                      :down {}})))

(defn- title-and-input-element
  "Element containing Title and textarea input"
  [title input]
  [:<>
   [toolbelt/desktop-mobile-switch
    [:h2.align-self-center.my-4 title]
    [:h2.align-self-center.my-4.display-6 title]]
   [:div.line-divider.my-4]
   input])

(defn- topic-bubble-desktop
  [meeting title input info-content]
  (let [agenda @(rf/subscribe [:chosen-agenda])
        share-hash (:meeting/share-hash meeting)]
    [:div.row
     ;; graph
     [:div.col-2.graph-icon
      [graph-button agenda share-hash]]
     ;; title
     [:div.col-8
      [title-and-input-element title input]]
     ;; up-down votes and statistics
     [:div.col-2.p-0
      [:div.float-right info-content]]]))

(defn- topic-bubble-mobile
  [meeting title input info-content]
  (let [agenda @(rf/subscribe [:chosen-agenda])
        share-hash (:meeting/share-hash meeting)]
    [:<>
     [:div.d-flex
      ;; graph
      [:div.graph-icon.mr-auto
       [graph-button agenda share-hash]]
      ;; settings
      [:div.p-0
       info-content]]
     ;; title
     [title-and-input-element title input]]))

(defn- topic-bubble [content]
  (let [agenda @(rf/subscribe [:chosen-agenda])]
    (common/set-website-title! (:agenda/title agenda))
    [:div.topic-view-rounded.shadow-straight-light.mt-md-4
     [:div.discussion-light-background content]]))

(defn- topic-view [current-meeting conclusions topic-content]
  [:<>
   [topic-bubble topic-content]
   [cards/conclusion-cards-list conclusions (:meeting/share-hash current-meeting)]])

(defn discussion-view-mobile
  "Discussion view for mobile devices
  No history but fullscreen topic bubble and conclusions"
  [current-meeting title input info-content conclusions]
  [:<>
   [topic-view current-meeting conclusions
    [topic-bubble-mobile current-meeting title input info-content]]])

(defn discussion-view-desktop
  "Discussion View for desktop devices.
  Displays a history on the left and a topic with conclusion in its center"
  [current-meeting title input info-content conclusions history]
  [:div.container-fluid
   [:div.row.px-0.mx-0
    [:div.col-1.py-4
     [history-view current-meeting history]]
    [:div.col-10.py-4.px-0
     [topic-view current-meeting conclusions
      [topic-bubble-desktop current-meeting title input info-content]]]]])

(defn info-content-conclusion
  "Badges and up/down-votes to be displayed in the right of the topic bubble."
  [statement edit-hash]
  [:<>
   [cards/up-down-vote-breaking statement]
   [badges/extra-discussion-info-badges statement edit-hash]])
