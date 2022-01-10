(ns schnaq.interface.views.modal
  (:require [com.fulcrologic.guardrails.core :refer [>defn]]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.translations :refer [labels]]
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
  [:div.modal-content.px-4
   [:div.modal-header
    [:h5.modal-title header]
    [:button.close {:type "button" :data-dismiss "modal" :aria-label "Close" :on-click close-modal}
     [:span {:aria-hidden "true"}
      [icon :cross]]]]
   [:div.modal-body body]])

;; -----------------------------------------------------------------------------
;; Enter Name Modal

(defn anonymous-modal
  "Basic modal which is presented to anonymous users trying to alter statements."
  [header-label shield-label info-label]
  [modal-template
   (labels header-label)
   [:<>
    [:p [icon :shield "m-auto" {:size "lg"}] " " (labels shield-label)]
    [:p (labels :discussion.anonymous-edit.modal/persuade)]
    [:button.btn.btn-primary.mx-auto.d-block
     {:on-click #(rf/dispatch [:keycloak/login])}
     (labels info-label)]]])

;; -----------------------------------------------------------------------------

(rf/reg-sub
 :modal
 (fn [db] (:modal db)))

(rf/reg-event-db
 :modal
 (fn [db [_ data]]
   (assoc db :modal data)))
