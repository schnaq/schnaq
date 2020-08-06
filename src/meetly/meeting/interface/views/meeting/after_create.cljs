(ns meetly.meeting.interface.views.meeting.after-create
  (:require [re-frame.core :as rf]
            [meetly.meeting.interface.text.display-data :refer [labels fa]]
            [meetly.meeting.interface.utils.clipboard :as clipboard]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [reagent.core :as reagent]
            [reitit.frontend.easy :as reitfe]
            [meetly.meeting.interface.views.base :as base]))

(defn- copy-link-form
  "A form that displays the link the user can copy. Form is read-only."
  []
  (reagent/create-class
    {:component-did-mount
     (fn [_]
       (.tooltip (js/$ "#meeting-link-form")))
     :reagent-render
     (fn []
       (let [meeting @(rf/subscribe [:selected-meeting])
             path (reitfe/href :routes/meetings.show {:share-hash (:meeting/share-hash meeting)})
             location (oget js/window :location)
             share-link (gstring/format "%s//%s/%s" (oget location :protocol) (oget location :host) path)
             display-success? @(rf/subscribe [:meeting/link-copied-display])]
         [:div
          [:form#meeting-link-form.form.create-meeting-form.form-inline.row
           {:on-click (fn []
                        (clipboard/copy-to-clipboard! share-link)
                        (rf/dispatch [:meeting/link-copied]))
            :data-toggle "tooltip"
            :data-placement "left"
            :title (labels :meeting/copy-link-tooltip)}
           [:input#meeting-link.form-control.form-round.form.title.col-11 {:type "text"
                                                                           :value share-link
                                                                           :readOnly true}]
           [:label.col-1 {:for "meeting-link"}
            [:h3 {:class (str "m-auto far " (fa :copy))}]]]
          [:br]
          [:div
           (if display-success?
             [:div.alert-success.text-center
              {:style {:width "50%"
                       :margin "auto"}
               :role "alert"}
              [:p (labels :meeting/link-copied-success)]]
             [:div.alert-success
              {:style {:visibility "hidden"}}
              [:p "Platzhalter"]])]]))}))

(defn after-meeting-creation-view
  "This view is presented to the user after they have created a new meeting. They should
  see the share-link and should be able to copy it easily."
  []
  (let [{:keys [meeting/share-hash]} @(rf/subscribe [:selected-meeting])]
    [:div
     [base/nav-header]
     [base/header
      (labels :meeting/created-success-heading)
      (labels :meeting/created-success-subheading)]
     [:div.container.px-5.py-3.text-center
      ;; list agendas
      [:h4.mb-4 (labels :meeting/educate-on-link-text)]
      [copy-link-form]
      [:h4.mb-4 (labels :meetings/continue-with-meetly-after-creation)]
      [:button.btn.btn-primary.btn-lg.center-block
       {:role "button"
        :on-click #(rf/dispatch [:navigate :routes/meetings.show {:share-hash share-hash}])}
       (labels :meetings/continue-to-meetly-button)]]]))

;; Events

(rf/reg-event-fx
  :meeting/link-copied
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:display-triggers :meeting-link-success] true)
     :dispatch-later [{:dispatch [:meeting/hide-link-copied-display]
                       :ms 5000}]}))

(rf/reg-event-db
  :meeting/hide-link-copied-display
  (fn [db _]
    (assoc-in db [:display-triggers :meeting-link-success] false)))

;; Subs

(rf/reg-sub
  :meeting/link-copied-display
  (fn [db _]
    (get-in db [:display-triggers :meeting-link-success])))