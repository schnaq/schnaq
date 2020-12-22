(ns schnaq.interface.views.discussion.cards.card-elements
  (:require [goog.dom :as gdom]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [schnaq.interface.text.display-data :refer [labels img-path fa]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.brainstorm.tools :as btools]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.discussion.cards.conclusion-card :as cards]
            [schnaq.interface.views.discussion.logic :as logic]
            [schnaq.interface.views.discussion.view-elements :as view-elements]
            [schnaq.interface.views.meeting.admin-buttons :as admin-buttons]))

(defn history-view
  "Histroy view displayed in the left column in the desktop view."
  ([{:meeting/keys [share-hash] :as current-meeting}]
   ;; home button
   [:div {:on-click (fn []
                      (rf/dispatch [:navigation/navigate :routes.meeting/show
                                    {:share-hash share-hash}])
                      (rf/dispatch [:meeting/select-current current-meeting]))}
    [:div.card-history-home.clickable.mt-md-4.i
     {:class (str "fas " (fa :home))}]])
  ([current-meeting history]
   (let [indexed-history (map-indexed #(vector (- (count history) %1 1) %2) history)]
     [:<>
      ;; home button
      [history-view current-meeting]
      ;; history
      (for [[index statement] indexed-history]
        [:div {:key (str "history-" (:db/id statement))
               :on-click #(rf/dispatch [:discussion.history/time-travel index])}

         [:div.card-history.clickable.mt-md-4
          {:class (str "statement-card-" (name (logic/arg-type->attitude (:meta/argument-type statement))))}
          [common/avatar (-> statement :statement/author :author/nickname) 30]]])])))

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

(defn- title-and-input-element
  "Element containing Title and textarea input"
  [title input]
  [:<>
   [:h2.align-self-center.my-4 title]
   [:div.line-divider.my-4]
   input])

(defn- topic-bubble-desktop
  [meeting title input]
  (let [agenda @(rf/subscribe [:chosen-agenda])
        share-hash (:meeting/share-hash meeting)]
    [:div.row
     ;; graph
     [:div.col-2.graph-icon
      [graph-button agenda share-hash]]
     ;; title
     [:div.col-10
      [title-and-input-element title input]]]))

(defn- topic-bubble-mobile
  [meeting title input]
  (let [agenda @(rf/subscribe [:chosen-agenda])
        share-hash (:meeting/share-hash meeting)]
    [:<>
     [:div.row
      ;; graph
      [:div.col-12.graph-icon
       [graph-button agenda share-hash]]]
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
  "Dicsussion view for mobile devices
  No history but fullscreen topic bubble and conclusions"
  [current-meeting title input conclusions]
  [:<>
   [topic-view current-meeting conclusions
    [topic-bubble-mobile current-meeting title input]]])

(defn discussion-view-desktop
  "Discussion View for desktop devices.
  Displays a history on the left and a topic with conclusion in its center"
  [current-meeting title input conclusions history]
  [:div.container-fluid
   [:div.row.px-0.mx-0
    [:div.col-1.py-4
     [history-view current-meeting history]]
    [:div.col-10.py-4.px-0
     [topic-view current-meeting conclusions
      [topic-bubble-desktop current-meeting title input]]]]])


(defn info-content-conclusion
  "Badges and up/down-votes to be displayed in the right of the topic bubble."
  [statement edit-hash]
  [:div.ml-5
   (cards/up-down-vote-breaking statement)
   [view-elements/extra-discussion-info-badges statement edit-hash]])
