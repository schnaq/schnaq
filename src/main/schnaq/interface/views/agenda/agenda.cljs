(ns schnaq.interface.views.agenda.agenda
  (:require [ajax.core :as ajax]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :as data]
            [schnaq.interface.utils.js-wrapper :as js-wrap]))

(defn new-agenda-local
  "This function formats the agenda-form input and saves it locally to the db until
  the discussion is created fully. `field` can be `title` or `description`."
  [field content suffix]
  (case field
    :title (rf/dispatch [:agenda/update-title content suffix])
    :description (rf/dispatch [:agenda/update-description content suffix])))

(defn new-agenda-form
  "A form for creating a new agenda. The new agenda is automatically saved in the
  app-state according to the suffix."
  [numbered-suffix]
  [:div
   [:div.agenda-line]
   [:div.add-agenda-div.agenda-point
    ;; title
    [:div.row.agenda-row-title
     [:div.col-8.col-md-10
      [:input.form-control.agenda-form-title.form-title
       {:type "text"
        :name (str "title-" numbered-suffix)
        :auto-complete "off"
        :required true
        :placeholder (str (data/labels :agenda/point) (inc numbered-suffix))
        :id (str "title-" numbered-suffix)
        :on-key-up
        #(new-agenda-local :title (oget % [:target :value]) numbered-suffix)}]]
     [:div.col-4.col-md-2
      [:div.pt-4.clickable
       {:on-click #(rf/dispatch [:agenda/delete-temporary numbered-suffix])}
       [:i {:class (str "m-auto fas fa-2x " (data/fa :delete-icon))}]]]]
    ;; description
    [:textarea.form-control.agenda-form-round
     {:name (str "title-" numbered-suffix)
      :rows 2
      :placeholder (str (data/labels :agenda/desc-for) (inc numbered-suffix))
      :id (str "description-" numbered-suffix)
      :on-key-up #(new-agenda-local
                    :description (oget % [:target :value]) numbered-suffix)}]]])

(defn add-agenda-button [number-of-forms add-event]
  (let [zero-agendas? (or (nil? number-of-forms) (zero? number-of-forms))]
    [:div.mb-5
     [:button.btn.agenda-add-button
      {:on-click (fn [e]
                   (js-wrap/prevent-default e)
                   (rf/dispatch [add-event]))
       :style {:padding (if zero-agendas? "0.5rem 1rem" "0 1rem")}}
      (if zero-agendas?
        [:span.display-6.my-4 (data/labels :agenda.create/optional-agenda)]
        [:span.display-4 "+"])]]))

(defn- submit-agenda-button []
  [:button.btn.button-primary (data/labels :meeting-create-header)])

(defn load-agenda-fn [hash on-success-event]
  {:fx [[:http-xhrio {:method :get
                      :uri (str (:rest-backend config) "/agendas/by-meeting-hash/" hash)
                      :format (ajax/transit-request-format)
                      :response-format (ajax/transit-response-format)
                      :on-success [on-success-event]
                      :on-failure [:ajax-failure]}]]})

(rf/reg-event-fx
  :agenda/load-and-redirect
  (fn [_ [_ hash]]
    (load-agenda-fn hash :agenda/set-current-maybe-enter)))

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

(rf/reg-event-fx
  :agenda/set-current-maybe-enter
  (fn [{:keys [db]} [_ agendas]]
    (let [selected-meeting (get-in db [:meeting :selected])
          always-to-db (assoc-in db [:agendas :current] agendas)]
      (if (<= (count agendas) 1)
        {:db (assoc-in always-to-db [:agenda :chosen] (first agendas))
         :fx [[:dispatch [:navigation/navigate :routes.discussion/start
                          {:share-hash (:meeting/share-hash selected-meeting)
                           :id (-> agendas first :agenda/discussion :db/id)}]]]}
        {:db always-to-db}))))

(rf/reg-event-db
  :agenda/clear-current
  (fn [db _]
    (assoc-in db [:agendas :current] [])))

(rf/reg-event-db
  :agenda/increase-form-num
  (fn [db _]
    (update-in db [:agenda :number-of-forms] inc)))

(rf/reg-event-db
  :agenda/update-title
  (fn [db [_ content suffix]]
    (assoc-in db [:agenda :all suffix :title] content)))

(rf/reg-event-db
  :agenda/update-description
  (fn [db [_ content suffix]]
    (assoc-in db [:agenda :all suffix :description] content)))

(rf/reg-event-db
  :agenda/reset-temporary-entries
  (fn [db _]
    (assoc db :agenda {})))

(rf/reg-event-db
  :agenda/delete-temporary
  (fn [db [_ suffix]]
    (-> db
        (update-in [:agenda :number-of-forms] dec)
        (update-in [:agenda :all] dissoc suffix))))

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
  :agenda/number-of-forms
  (fn [db _]
    (-> db :agenda :number-of-forms)))

(rf/reg-sub
  :current-agendas
  (fn [db _]
    (get-in db [:agendas :current])))

(rf/reg-sub
  :chosen-agenda
  (fn [db _]
    (get-in db [:agenda :chosen])))