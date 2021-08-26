(ns schnaq.interface.views.discussion.labels
  (:require ["react-tippy" :refer [Tooltip]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.text.display-data :refer [fa]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.modal :as modal]))

(defn build-label
  "Takes a label and builds the neccesary html."
  [label set?]
  (let [[badge-color icon-class]
        (case label
          ":comment" ["badge-primary" "fa-comment"]
          ":arrow-right" ["badge-purple" "fa-arrow-right"]
          ":calendar-alt" ["badge-info" "fa-calendar-alt"]
          ":check" ["badge-success" "fa-check"]
          ":ghost" ["badge-dark" "fa-ghost"]
          ":question" ["badge-warning" "fa-question"]
          ":times" ["badge-danger" "fa-times"])
        extra-class (if set? (str badge-color " label-set") badge-color)]
    [:span.badge.badge-pill.px-4.label
     {:class extra-class}
     [:i {:class (str "m-auto fas " icon-class)}]]))

(defn- anonymous-labels-modal
  "Explain to anonymous users that they need to log in to set and remove labels."
  []
  (modal/anonymous-modal :discussion.anonymous-labels.modal/title
                         :discussion.anonymous-labels.modal/explain
                         :discussion.anonymous-labels.modal/cta))

(defn build-labels
  [statement]
  ;; TODO add on-click method
  [:<>
   (for [label shared-config/allowed-labels]
     [:span.mr-3
      {:key (str "label-" (:db/id statement) "-" label)
       :on-click #(js/alert (str "Clicked on" label))}
      [build-label label ((set (:statement/labels statement)) label)]])])

(defn edit-labels-button
  "Give the registered user the ability to add or remove labels to a statement."
  ;; TODO labelize
  [statement]
  (let [authenticated? @(rf/subscribe [:user/authenticated?])]
    (if authenticated?
      [:> Tooltip
       {:animation "scale"
        :arrow true
        :html (r/as-element [build-labels statement])
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