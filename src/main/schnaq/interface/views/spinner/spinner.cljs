(ns schnaq.interface.views.spinner.spinner
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]))

(def ^:private role-id "loading-status")

(defn view
  "Place a spinner on top of a view.
    Activate spinner with (rf/dispatch [:spinner/active! true])
    Deactivate spinner with (rf/dispatch [:spinner/active! false])"
  []
  (reagent/create-class
    {:display-name "Spinner View"
     :reagent-render
     (fn [_this]
       (let [spinner-is-loading? @(rf/subscribe [:spinner/active?])]
         (when spinner-is-loading?
           [:div.spinner-styling
            [:div.spinner-border.text-primary {:role role-id}
             [:span.sr-only "Loading..."]]])))
     :component-will-unmount
     (fn [_this]
       (rf/dispatch [:spinner/active! false]))}))


;; subs

(rf/reg-sub
  :spinner/active?
  (fn [db _]
    (get-in db [:spinner :active?] false)))

(rf/reg-event-db
  :spinner/active!
  (fn [db [_ toggle]]
    (assoc-in db [:spinner :active?] toggle)))