(ns schnaq.interface.views.meeting.after-create
  (:require ["jquery" :as jquery]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels img-path fa]]
            [schnaq.interface.utils.clipboard :as clipboard]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [ghostwheel.core :refer [>defn-]]
            [reagent.core :as reagent]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.utils.js-wrapper :as js-wrap]))

(>defn- get-share-link
  [current-route]
  [map? :ret string?]
  (let [share-hash (-> current-route :path-params :share-hash)
        path (reitfe/href :routes.meeting/show {:share-hash share-hash})
        location (oget js/window :location)]
    (gstring/format "%s//%s%s" (oget location :protocol) (oget location :host) path)))

(>defn- get-edit-link
  [current-route]
  [map? :ret string?]
  (let [share-hash (-> current-route :path-params :share-hash)
        admin-hash (-> current-route :path-params :admin-hash)
        path (reitfe/href :routes.meeting/edit {:share-hash share-hash
                                                :admin-hash admin-hash})
        location (oget js/window :location)]
    (gstring/format "%s//%s%s" (oget location :protocol) (oget location :host) path)))

(defn- copy-link-form
  "A form that displays the link the user can copy. Form is read-only."
  [create-link-fn id-extra]
  (reagent/create-class
    {:component-did-mount
     (fn [_]
       (.tooltip (jquery (str "#meeting-link-form-" id-extra))))
     :reagent-render
     (fn []
       (let [display-content (create-link-fn @(rf/subscribe [:navigation/current-route]))
             meeting-link-id (str "meeting-link" id-extra)]
         [:div.pb-4
          [:form.form.create-meeting-form.form-inline.row
           {:id (str "meeting-link-form-" id-extra)
            :on-click (fn [e]
                        (js-wrap/prevent-default e)
                        (clipboard/copy-to-clipboard! display-content)
                        (rf/dispatch
                          [:notification/add
                           #:notification{:title (labels :meeting/link-copied-heading)
                                          :body (labels :meeting/link-copied-success)
                                          :context :info}]))
            :data-toggle "tooltip"
            :data-placement "right"
            :title (labels :meeting/copy-link-tooltip)}
           [:input.form-control.form-round.col-11.copy-link-form.clickable-no-hover
            {:id meeting-link-id
             :type "text"
             :value display-content
             :readOnly true}]
           [:label.col-1.clickable-no-hover {:for meeting-link-id}
            [:div {:class (str "m-auto far fa-lg " (fa :copy))}]]]]))}))

(defn img-text
  "Create one icon in a grid"
  [path-to-img heading]

  [:div.d-flex.flex-row.p-1
   [:div [:img {:src path-to-img}]]
   [:span [:h5 heading]]])

(defn- educate-element []
  [:div.row.mb-3
   [:div.col-11
    [:div.row
     [:div.col-lg-6.share-link-icons
      (img-text (img-path :elephant-share)
                (labels :meeting/educate-on-link-text))]
     [:div.col-lg-6.share-link-icons
      (img-text (img-path :elephant-talk)
                (labels :meetings/educate-on-link-text-subtitle))]]]])

(defn- educate-admin-element [share-hash admin-hash]
  [:div.row.mb-3
   [:div.col-11
    [:div.row
     ;; edit
     [:div.col-lg-6
      [:div.share-link-icons
       (img-text (img-path :elephant-erase)
                 (labels :meeting/educate-on-edit))]
      [:button.btn.button-secondary.btn-lg.float-left.mt-2.span-container
       {:role "button"
        :on-click #(rf/dispatch [:navigation/navigate
                                 :routes.meeting/edit
                                 {:share-hash share-hash :admin-hash admin-hash}])}
       (labels :meetings/edit-schnaq-button)]]
     ;; admin hash
     [:div.col-lg-6.share-link-icons
      (img-text (img-path :elephant-admin)
                (labels :meeting/educate-on-admin))
      [:div.py-3
       [copy-link-form get-edit-link "edit-hash"]]]]]])

(defn after-meeting-creation-view
  "This view is presented to the user after they have created a new meeting. They should
  see the share-link and should be able to copy it easily."
  []
  (let [{:keys [share-hash admin-hash]} (:path-params @(rf/subscribe [:navigation/current-route]))]
    [:div
     [base/nav-header]
     [base/header
      (labels :meeting/created-success-heading)
      (labels :meeting/created-success-subheading)]
     [:div.container.px-5.py-3.text-center
      ;; list agendas
      [educate-element]
      [copy-link-form get-share-link "share-hash"]
      [educate-admin-element share-hash admin-hash]
      ;; stop image and hint to copy the link
      [:div.single-image [:img {:src (img-path :elephant-stop)}]]
      [:h4.mb-4 (labels :meetings/continue-with-schnaq-after-creation)]
      ;; go to meeting button
      [:button.btn.button-primary.btn-lg.center-block
       {:role "button"
        :on-click #(rf/dispatch [:navigation/navigate :routes.meeting/show {:share-hash share-hash}])}
       (labels :meetings/continue-to-schnaq-button)]]]))