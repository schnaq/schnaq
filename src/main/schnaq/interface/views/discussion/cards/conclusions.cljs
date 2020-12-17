(ns schnaq.interface.views.discussion.cards.conclusions
  (:require [goog.dom :as gdom]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels img-path fa]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.base :as base]


            [schnaq.interface.views.discussion.cards.conclusion-card :as cards]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.common :as common]

            [schnaq.interface.views.discussion.logic :as logic]))

(defn history-view
  "Histroy view displayed in the left column in the desktop view."
  ([{:meeting/keys [share-hash] :as current-meeting}]
   ;; home button
   [:div {:on-click (fn []
                      (rf/dispatch [:navigation/navigate :routes.meeting/show
                                    {:share-hash share-hash}])
                      (rf/dispatch [:meeting/select-current current-meeting]))}
    [:div.card-history.card-history-home.clickable.mt-md-4.i
     {:class (str "fas " (fa :home))}]])
  ([current-meeting history]
   (let [indexed-history (map-indexed #(vector (- (count history) %1 1) %2) history)]
     [:div
      ;; home button
      [history-view current-meeting]
      ;; history
      (for [[index statement] indexed-history]
        [:div {:key (str "history-" (:db/id statement))
               :on-click #(rf/dispatch [:discussion.history/time-travel index])}

         [:div.card-history.card-history-home.clickable.mt-md-4.i
          {:class (str "fas " (fa :plane))}]])])))


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

(defn- input-starting-statement-form
  "A form, which allows the input of a starting-statement."
  [textarea-id]
  [:form.my-2
   {:on-submit (fn [e] (js-wrap/prevent-default e)
                 (rf/dispatch [:discussion.add.statement/starting (oget e [:target :elements])]))}
   [:div.discussion-input-container.w-100
    [:div.d-flex.flex-row
     [:textarea.form-control.discussion-text-input-area.w-100
      {:id textarea-id
       :name "statement-text" :wrap "soft" :rows 1
       :auto-complete "off"
       :onInput (fn [_event]
                  ;; first reset input then set height +1px in order to prevent scrolling
                  (let [input (gdom/getElement textarea-id)]
                    (toolbelt/height-to-scrollheight! input)))
       :required true
       :data-dynamic-height true
       :placeholder (labels :discussion/add-argument-conclusion-placeholder)}]
     ;; submit icon button
     [:button.primary-icon-button
      {:type "submit"
       :title (labels :discussion/create-argument-action)}
      [:i {:class (str "m-auto fas " (fa :plane))}]]]]])

(defn radio-button
  "Radio Button helper function. This function creates a radio button."
  [id name value label hint color-class checked?]
  [:<>
   [:input {:id id :type "radio" :name name :value value :default-checked checked?}]
   [:label.mx-1.my-1 {:class color-class :for id} label]])

(defn radio-buttons []
  [:div.radio-toolbar
   [:div.d-flex.flex-row.px-3
    ;; support
    [radio-button
     "for-radio" "premise-choice" "for-radio"
     [:i {:class (str "m-auto fas " (fa :plus))}]
     (labels :discussion/add-premise-supporting) "hover-primary" false]
    ;; neutral
    [radio-button
     "neutral-radio" "premise-choice" "for-radio"
     [:i {:class (str "m-auto fas " (fa :plus))}]
     (labels :discussion/add-premise-supporting) "hover-white" true]
    ;; attack
    [radio-button
     "against-radio" "premise-choice" "against-radio"
     [:i {:class (str "m-auto fas " (fa :minus))}]
     (labels :discussion/add-premise-against) "hover-secondary" false]]])

(defn- input-reaction-statement-form
  "A form, which allows the input of a starting-statement."
  [textarea-id]
  [:form.my-2
   {:on-submit (fn [e]
                 (js-wrap/prevent-default e)
                 (logic/submit-new-premise (oget e [:target :elements])))}
   [:div.discussion-input-container.w-100
    [:div.d-flex.flex-row
     [:textarea.form-control.discussion-text-input-area.w-100
      {:id textarea-id
       :name "premise-text" :wrap "soft" :rows 1
       :auto-complete "off"
       :onInput (fn [_event]
                  ;; first reset input then set height +1px in order to prevent scrolling
                  (let [input (gdom/getElement textarea-id)]
                    (toolbelt/height-to-scrollheight! input)))
       :required true
       :data-dynamic-height true
       :placeholder (labels :discussion/add-argument-conclusion-placeholder)}]
     [radio-buttons]
     ;; submit icon button
     [:button.primary-icon-button
      {:type "submit"
       :title (labels :discussion/create-argument-action)}
      [:i {:class (str "m-auto fas " (fa :plane))}]]]]])

(defn- title-and-input-element
  "Element containing Title and textarea input"
  [input-element-id title]
  [:<>
   [:h2.align-self-center.my-4 title]
   [:div.line-divider.my-4]
   [input-reaction-statement-form input-element-id]])

(defn- topic-bubble-desktop
  [{:meeting/keys [share-hash name] :as meeting} title]
  (let [agenda @(rf/subscribe [:chosen-agenda])
        admin-access-map @(rf/subscribe [:meetings/load-admin-access])
        edit-hash (get admin-access-map share-hash)]
    [:div.row
     ;; graph
     [:div.col-2.graph-icon
      [graph-button agenda share-hash]]
     ;; title
     [:div.col-8
      [title-and-input-element "input-statement-id-desktop" title]]
     ;; settings
     [:div.col-2.p-0
      #_[settings-element meeting name share-hash edit-hash]]]))

(defn- topic-bubble [content]
  (let [agenda @(rf/subscribe [:chosen-agenda])]
    (common/set-website-title! (:agenda/title agenda))
    [:div.topic-view-rounded.shadow-straight-light.mt-md-4
     [:div.discussion-light-background content]]))

(defn- topic-view [current-meeting conclusions topic-content]
  [:<>
   [topic-bubble topic-content]
   [cards/conclusion-cards-list conclusions (:meeting/share-hash current-meeting)]])



(defn- discussion-step-view-desktop
  [current-meeting history conclusions title]
  [:div.container-fluid
   [:div.row.px-0.mx-0
    [:div.col-1.py-4
     [history-view current-meeting history]]
    [:div.col-10.py-4.px-0
     [topic-view current-meeting
      conclusions
      [topic-bubble-desktop current-meeting title]]]]])

(defn- discussion-start-view
  "The first step after starting a discussion."
  []
  (let [current-premises @(rf/subscribe [:discussion.premises/current])
        current-meeting @(rf/subscribe [:meeting/selected])
        history @(rf/subscribe [:discussion-history])
        current-conclusion (last history)
        title (:statement/content current-conclusion)]

    [:<>
     [base/meeting-header current-meeting]
     [:div.container-fluid.px-0
      [toolbelt/desktop-mobile-switch
       [discussion-step-view-desktop current-meeting history current-premises title]
       [discussion-step-view-desktop current-meeting history current-premises title]]]]))

(defn selected-conclusion []
  [discussion-start-view])