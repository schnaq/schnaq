(ns schnaq.interface.views.discussion.card-view
  (:require [oops.core :refer [oget oset!]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.text.display-data :refer [labels img-path fa]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.markdown-parser :as markdown-parser]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.brainstorm.tools :as btools]
            [schnaq.interface.views.discussion.conclusion-card :as cards]
            [schnaq.interface.views.meeting.admin-buttons :as admin-buttons]
            [schnaq.interface.views.navbar :as navbar]))


(defn- card-meeting-header
  "Overview header for a meeting with a name input"
  []
  [:nav.navbar.navbar-expand-lg.py-3.navbar-dark.context-header.shadow-straight-light
   ;; schnaq logo
   [:a.navbar-brand.mr-auto {:href (reitfe/href :routes/startpage)}
    [:img.d-inline-block.align-middle.mr-2
     {:src (img-path :logo-white) :width "150" :alt "schnaq logo"}]]
   ;; name input
   [:div.float-right
    [navbar/username-bar-view-light]]])

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

(defn- settings-element
  "Element containing settings buttons"
  [meeting title share-hash edit-hash]
  [:div.float-right
   (when (and edit-hash (btools/is-brainstorm? meeting))
     [admin-buttons/admin-center share-hash edit-hash])
   [admin-buttons/txt-export share-hash title]])

(defn- input-starting-statement-form
  "A form, which allows the input of a starting-statement."
  []
  (let [input-id "textinput-statement"]
    [:form.my-2
     {:on-submit (fn [e] (js-wrap/prevent-default e)
                   (rf/dispatch [:discussion.add.statement/starting
                                 (oget e [:target :elements])]))}
     [:div.discussion-input-container.w-100
      [:div.d-flex.flex-row
       [:textarea.form-control.discussion-text-input-area.w-100
        {:id input-id
         :name "statement-text" :wrap "soft" :rows 1
         :auto-complete "off"
         :onInput (fn [_element]
                    ;; first reset input then set height +1px in order to prevent scrolling
                    (let [input (.getElementById js/document input-id)]
                      (oset! input [:style :height] "0.5rem")
                      (oset! input [:style :height] (str (+ 1 (oget input [:scrollHeight])) "px"))))
         :required true
         :placeholder (labels :discussion/add-argument-conclusion-placeholder)}]
       ;; submit icon button
       [:button.primary-icon-button
        {:type "submit"
         :title (labels :discussion/create-argument-action)}
        [:i {:class (str "m-auto fas " (fa :plane))}]]]]]))

(defn- title-and-input-element
  "Element containing Title and textarea input"
  [agenda]
  [:<>
   [:h2.align-self-center.my-4
    (:agenda/title agenda)]
   [markdown-parser/markdown-to-html (:agenda/description agenda)]
   [:div.line-divider.my-4]
   [input-starting-statement-form]])

(defn- topic-bubble-mobile
  [meeting title agenda share-hash edit-hash]
  [:<>
   [:div.row
    ;; graph
    [:div.col-6.graph-icon
     [graph-button agenda share-hash]]
    ;; settings
    [:div.col-6.p-0
     [settings-element meeting title share-hash edit-hash]]]
   ;; title
   [title-and-input-element agenda]])

(defn- topic-bubble-desktop
  [meeting title agenda share-hash edit-hash]
  [:div.row
   ;; graph
   [:div.col-2.graph-icon
    [graph-button agenda share-hash]]
   ;; title
   [:div.col-8
    [title-and-input-element agenda]]
   ;; settings
   [:div.col-2.p-0
    [settings-element meeting title share-hash edit-hash]]])

(defn- topic-bubble [{:meeting/keys [share-hash title] :as meeting}]
  (let [agenda @(rf/subscribe [:chosen-agenda])
        admin-access-map @(rf/subscribe [:meetings/load-admin-access])
        edit-hash (get admin-access-map share-hash)]
    (common/set-website-title! (:agenda/title agenda))
    [:div.topic-view-rounded.shadow-straight-light.mt-md-4
     [:div.discussion-light-background
      [toolbelt/desktop-mobile-switch
       [topic-bubble-desktop meeting title agenda share-hash edit-hash]
       [topic-bubble-mobile meeting title agenda share-hash edit-hash]]]]))

(defn- topic-view [current-meeting]
  [:<>
   [topic-bubble
    current-meeting
    (fn []
      (rf/dispatch [:navigation/navigate :routes.meeting/show
                    {:share-hash (:meeting/share-hash current-meeting)}])
      (rf/dispatch [:meeting/select-current current-meeting]))]
   [cards/conclusion-cards-list @(rf/subscribe [:discussion.conclusions/starting])
    (:meeting/share-hash current-meeting)]])

(defn- discussion-start-view-desktop
  [current-meeting]
  [:container-fluid
   [:div.row.px-0.mx-0
    [:div.col-1]
    [:div.col-10.py-4.px-0
     [topic-view current-meeting]]]])

(defn- discussion-start-view-mobile
  [current-meeting]
  [:div.container-fluid [topic-view current-meeting]])

(defn- discussion-start-view
  "The first step after starting a discussion."
  []
  (let [current-meeting @(rf/subscribe [:meeting/selected])]
    [:<>
     [card-meeting-header current-meeting]
     [:container-fluid
      [toolbelt/desktop-mobile-switch
       [discussion-start-view-desktop current-meeting]
       [discussion-start-view-mobile current-meeting]]]]))

(defn discussion-start-view-entrypoint []
  [discussion-start-view])