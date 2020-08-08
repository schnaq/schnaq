(ns meetly.meeting.interface.views.modals.modal
  (:require [re-frame.core :as rf]
            [meetly.meeting.interface.views.modals.events]
            [meetly.meeting.interface.views.modals.subs]
            [meetly.meeting.interface.text.display-data :refer [labels]]
            [ghostwheel.core :refer [>defn]]
            [oops.core :refer [oget]]))

(defn modal-panel
  [{:keys [child show?]}]
  [:div {:class "modal-wrapper"}
   [:div {:class "modal-backdrop"
          :on-click (fn [event]
                      (rf/dispatch [:modal {:show? (not show?)
                                            :child nil}])
                      (.preventDefault event)
                      (.stopPropagation event))}]
   [:div {:class "modal-child modal-dialog"}
    child]])

(defn modal []
  (let [modal (rf/subscribe [:modal])]
    (fn []
      [:div
       (when (:show? @modal)
         [modal-panel @modal])])))

(defn- close-modal []
  (rf/dispatch [:modal {:show? false :child nil}]))

(>defn modal-template
  "Generic modal template."
  [header body]
  [string? vector? :ret vector?]
  [:div.modal-content
   [:div.modal-header
    [:h5.modal-title header]
    [:button.close {:type "button" :data-dismiss "modal" :aria-label "Close"}
     [:span {:aria-hidden "true"
             :on-click close-modal}
      "x"]]]
   [:div.modal-body body]])


;; -----------------------------------------------------------------------------
;; Enter Name Modal

(defn- modal-name-input
  "An input, where the user can set their name. Happens automatically by typing."
  [username]
  [:form.form
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (rf/dispatch [:set-username (oget e [:target :elements :name-input :value])])
                 (rf/dispatch [:hide-name-input])
                 (close-modal))}
   [:div.px-2 [:input#name-input.form-control.form-round-05.px-2.py-1
               {:type "text"
                :name "name-input"
                :required true
                :autoFocus true
                :placeholder username}]]
   [:br]
   [:div.modal-footer
    [:input.btn.btn-primary {:type "submit"
                             :value "Set Name"}]]])

(defn enter-name-modal []
  (modal-template
    (labels :modals/enter-name-header)
    [:div
     [:p (labels :modals/enter-name-primer)]
     [modal-name-input "Ihr Name"]]))