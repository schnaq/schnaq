(ns schnaq.interface.views.discussion.labels
  (:require ["react-tippy" :refer [Tooltip]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.text.display-data :refer [fa]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.modal :as modal]))

(defn build-label
  "Takes a label and builds the neccesary html."
  [label]
  ;; TODO add hover animation and pointer
  (case label
    ":comment"
    [:span.badge.badge-pill.badge-primary.px-4 [:i {:class "m-auto fas fa-comment"}]]
    ":arrow-right"
    [:span.badge.badge-pill.badge-purple.px-4 [:i {:class "m-auto fas fa-arrow-right"}]]
    ":calendar-alt"
    [:span.badge.badge-pill.badge-info.px-4 [:i {:class "m-auto fas fa-calendar-alt"}]]
    ":check"
    [:span.badge.badge-pill.badge-success.px-4 [:i {:class "m-auto fas fa-check"}]]
    ":ghost"
    [:span.badge.badge-pill.badge-dark.px-4 [:i {:class "m-auto fas fa-ghost"}]]
    ":question"
    [:span.badge.badge-pill.badge-warning.px-4 [:i {:class "m-auto fas fa-question"}]]
    ":times"
    [:span.badge.badge-pill.badge-danger.px-4 [:i {:class "m-auto fas fa-times"}]]))

(defn- anonymous-labels-modal
  "Explain to anonymous users that they need to log in to set and remove labels."
  []
  (modal/anonymous-modal :discussion.anonymous-labels.modal/title
                         :discussion.anonymous-labels.modal/explain
                         :discussion.anonymous-labels.modal/cta))

(defn test-component
  [statement]
  [:div
   (for [label shared-config/allowed-labels]
     [:span.mr-3
      {:key (str "label-" (:db/id statement) "-" label)
       :on-click #(js/alert (str "Clicked on" label))}
      [build-label label]])])

(defn edit-labels-button
  "Give the registered user the ability to add or remove labels to a statement."
  ;; TODO labelize
  [statement]
  (let [authenticated? @(rf/subscribe [:user/authenticated?])]
    (if authenticated?
      [:> Tooltip
       {:animation "scale"
        :arrow true
        :html (r/as-element [test-component statement])
        :interactive true
        :offset 5
        :position "bottom"
        :size "big"
        :theme "light"
        :trigger "click"}
       [:div.pr-2.clickable
        [:i {:class (fa :tag)}]]]
      [:button.dropdown-item
       {:tabIndex 30
        :on-click (fn [e]
                    (js-wrap/stop-propagation e)
                    #(rf/dispatch [:modal {:show? true
                                           :child [anonymous-labels-modal]}]))
        :title "Labels"}
       [:i {:class (str "m-auto " (fa :tag))}] " Labels"])))