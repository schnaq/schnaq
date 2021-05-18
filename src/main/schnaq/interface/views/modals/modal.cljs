(ns schnaq.interface.views.modals.modal
  (:require [ghostwheel.core :refer [>defn]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]))

(defn modal-panel
  [{:keys [child show? large?]}]
  [:div.modal-wrapper
   [:div {:class "modal-backdrop"
          :on-click (fn [event]
                      (rf/dispatch [:modal {:show? (not show?)
                                            :child nil}])
                      (js-wrap/prevent-default event)
                      (js-wrap/stop-propagation event))}]
   (let [classes "modal-child modal-dialog modal-dialog-scrollable"]
     [:div {:class (if large? (str classes " modal-lg") classes)}
      child])])

(defn modal-view
  "Include modal in view."
  []
  (let [modal @(rf/subscribe [:modal])]
    (when (:show? modal)
      [modal-panel modal])))

(defn close-modal []
  (rf/dispatch [:modal {:show? false :child nil}]))

(>defn modal-template
  "Generic modal template."
  [header body]
  [string? vector? :ret vector?]
  [:div.modal-content
   [:div.modal-header
    [:h5.modal-title header]
    [:button.close {:type "button" :data-dismiss "modal" :aria-label "Close" :on-click close-modal}
     [:span {:aria-hidden "true"}
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
   [:div.px-2.pb-3
    [:input#name-input.form-control.form-round-05.px-2.py-1
     {:type "text"
      :name "name-input"
      :required true
      :autoFocus true
      :placeholder username}]]
   [:div.modal-footer
    [:input.btn.btn-primary
     {:type "submit"
      :value (labels :user.button/set-name)}]]])

(defn enter-name-modal []
  (modal-template
    (labels :user.set-name.modal/header)
    [:<>
     [:p (labels :user.set-name.modal/primer)]
     [modal-name-input (labels :user.button/set-name-placeholder)]]))


;; -----------------------------------------------------------------------------

(rf/reg-sub
  :modal
  (fn [db] (:modal db)))

(rf/reg-event-db
  :modal
  (fn [db [_ data]]
    (assoc db :modal data)))