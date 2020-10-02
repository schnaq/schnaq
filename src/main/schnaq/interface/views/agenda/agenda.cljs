(ns schnaq.interface.views.agenda.agenda
  (:require [ajax.core :as ajax]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :as data]
            [schnaq.interface.views.text-editor.view :as editor]
            [schnaq.interface.utils.js-wrapper :as js-wrap]))

(defn new-agenda-local
  "This function formats the agenda-form input and saves it locally to the db until
  the discussion is created fully. `field` can be `title` or `description`."
  [field content suffix]
  (case field
    :title (rf/dispatch [:agenda/update-title content suffix])
    :description (rf/dispatch [:agenda/update-description content suffix])))

(defn new-agenda-description-keyword [numbered-suffix]
  (keyword (str "meeting.create/agenda/" numbered-suffix "/description")))

(defn- agenda-title-input
  [attributes]
  [:<>
   [:input.form-control.form-border-bottom-light.form-title-light
    attributes]])

(defn agenda-form
  "A form for creating a new agenda. The new agenda is automatically saved in the
  app-state according to the suffix."
  ([delete-fn description description-submit-fn title-attributes]
   (agenda-form delete-fn description description-submit-fn title-attributes nil))
  ([delete-fn description description-submit-fn title-attributes children]
   (let [min-description-height "150px"]
     [:<>
      [:div.agenda-line]
      [:div.agenda-point.shadow-straight.pb-3
       ;; title
       [:div.background-secondary.p-3
        [:div.row
         [:div.col-10.col-md-10
          [agenda-title-input title-attributes]]
         [:div.col-2.col-md-2
          [:div.pt-4
           {:on-click delete-fn}
           [:i.clickable {:class (str "m-auto fas fa-2x " (data/fa :delete-icon))}]]]]]
       ;; description
       [:div.text-left
        [editor/view description description-submit-fn nil min-description-height]]
       children]])))

(defn add-agenda-button [number-of-forms add-event]
  (let [zero-agendas? (or (nil? number-of-forms) (zero? number-of-forms))]
    [:div.mb-5
     [:div.p-0
      {:on-click (fn [e]
                   (js-wrap/prevent-default e)
                   (rf/dispatch [add-event]))
       :style {:padding (if zero-agendas? "0.5rem 1rem" "0 1rem")}}
      (if zero-agendas?
        [:button.btn.agenda-add-button.font-150
         [:span.m-4 (data/labels :agenda.create/optional-agenda)]]
        [:img.align-middle.clickable {:src (data/img-path :icon-add) :width "50" :alt ""}])]]))

(defn load-agenda-fn [share-hash on-success-event]
  {:fx [[:http-xhrio {:method :get
                      :uri (str (:rest-backend config) "/agendas/by-meeting-hash/" share-hash)
                      :format (ajax/transit-request-format)
                      :response-format (ajax/transit-response-format)
                      :on-success [on-success-event]
                      :on-failure [:ajax-failure]}]]})

(rf/reg-event-fx
  :agenda/load-agendas
  (fn [_ [_ hash]]
    (load-agenda-fn hash :agenda/set-current)))

(rf/reg-event-fx
  :agenda/load-chosen
  (fn [{:keys [db]} [_ share-hash discussion-id]]
    (when-not (-> db :agenda :chosen)
      {:fx [[:http-xhrio {:method :get
                          :uri (gstring/format "%s/agenda/%s/%s" (:rest-backend config) share-hash discussion-id)
                          :format (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success [:agenda/set-response-as-chosen]
                          :on-failure [:agenda.error/not-available]}]]})))

(rf/reg-event-fx
  :agenda.error/not-available
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:error :ajax] "Agenda could not be loaded, please refresh the App.")
     :fx [[:dispatch [:navigation/navigate :routes/meetings]]]}))

(rf/reg-event-db
  :agenda/set-current
  (fn [db [_ agendas]]
    (assoc-in db [:agendas :current] agendas)))

(rf/reg-event-db
  :agenda/clear-current
  (fn [db _]
    (assoc-in db [:agendas :current] [])))

(rf/reg-event-db
  :agenda/increase-form-num
  (fn [db _]
    (assoc-in db [:agenda :creating :all (random-uuid)] {})))

(rf/reg-event-db
  :agenda/update-title
  (fn [db [_ content suffix]]
    (assoc-in db [:agenda :creating :all suffix :title] content)))

(rf/reg-event-db
  :agenda/update-description
  (fn [db [_ content suffix]]
    (assoc-in db [:agenda :creating :all suffix :description] content)))

(rf/reg-event-db
  :agenda/reset-temporary-entries
  (fn [db _]
    (assoc-in db [:agenda :creating :all] [])))

(rf/reg-event-db
  :agenda/delete-temporary
  (fn [db [_ suffix]]
    (update-in db [:agenda :creating :all] dissoc suffix)))

(rf/reg-event-db
  :agenda/choose
  (fn [db [_ agenda]]
    (assoc-in db [:agenda :chosen] agenda)))

(rf/reg-event-db
  :agenda/set-response-as-chosen
  (fn [db [_ response]]
    (assoc-in db [:agenda :chosen] response)))

;; #### Subs ####

(rf/reg-sub
  :agendas.temporary/all
  (fn [db _]
    (get-in db [:agenda :creating :all] [])))

(rf/reg-sub
  :current-agendas
  (fn [db _]
    (get-in db [:agendas :current])))

(rf/reg-sub
  :chosen-agenda
  (fn [db _]
    (get-in db [:agenda :chosen])))