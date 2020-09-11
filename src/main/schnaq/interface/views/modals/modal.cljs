(ns schnaq.interface.views.modals.modal
  (:require [re-frame.core :as rf]
            [schnaq.interface.views.modals.events]
            [schnaq.interface.views.modals.subs]
            [schnaq.interface.text.display-data :refer [labels]]
            [ghostwheel.core :refer [>defn]]
            [oops.core :refer [oget]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]))

(defn modal-panel
  [{:keys [child show? large?]}]
  [:div {:class "modal-wrapper"}
   [:div {:class "modal-backdrop"
          :on-click (fn [event]
                      (rf/dispatch [:modal {:show? (not show?)
                                            :child nil}])
                      (js-wrap/prevent-default event)
                      (js-wrap/stop-propagation event))}]
   (let [classes "modal-child modal-dialog modal-dialog-scrollable"]
     [:div {:class (if large? (str classes " modal-lg") classes)}
      child])])

(defn modal []
  (let [modal (rf/subscribe [:modal])]
    (fn []
      [:div
       (when (:show? @modal)
         [modal-panel @modal])])))

(defn close-modal []
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
                 (js-wrap/prevent-default e)
                 (rf/dispatch [:user/set-display-name (oget e [:target :elements :name-input :value])])
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