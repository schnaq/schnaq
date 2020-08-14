(ns meetly.interface.views.agenda.agenda
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [ajax.core :as ajax]
            [goog.string :as gstring]
            [meetly.interface.text.display-data :as data]
            [meetly.interface.views.base :as base]
            [meetly.interface.config :refer [config]]
            [meetly.interface.utils.js-wrapper :as js-wrap]))

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
    [:input.form-control.agenda-form-title.form-title.agenda-row-title
     {:type "text"
      :name "title"
      :auto-complete "off"
      :required true
      :placeholder (str (data/labels :agenda/point) (inc numbered-suffix))
      :id (str "title-" numbered-suffix)
      :on-key-up
      #(new-agenda-local :title (oget % [:target :value]) numbered-suffix)}]
    ;; description
    [:textarea.form-control.agenda-form-round
     {:name "description"
      :placeholder (str (data/labels :agenda/desc-for) (inc numbered-suffix))
      :id (str "description-" numbered-suffix)
      :on-key-up #(new-agenda-local
                    :description (oget % [:target :value]) numbered-suffix)}]]])

(defn- add-agenda-button []
  [:input.btn.agenda-add-button {:type "button"
                                 :value "+"
                                 :on-click #(rf/dispatch [:increase-agenda-forms])}])

(defn- submit-agenda-button []
  [:input.btn.button-primary {:type "submit"
                              :value (data/labels :meeting-create-header)}])

;; #### header ####

(defn- header []
  (base/header
    (data/labels :agenda/header)
    (data/labels :agenda/subheader)))


(defn agenda-view
  "Shows the view for adding one or more agendas."
  []
  [:div#create-agenda
   [base/nav-header]
   [header]
   [:div.container.px-5.py-3.text-center
    [:div.agenda-meeting-container.p-3
     [:h2 (:meeting/title @(rf/subscribe [:meeting/last-added]))]
     [:br]
     [:h4 (:meeting/description @(rf/subscribe [:meeting/last-added]))]]
    [:div.container
     [:div.agenda-container
      [:form {:id "agendas-add-form"
              :on-submit (fn [e]
                           (js-wrap/prevent-default e)
                           (rf/dispatch [:send-agendas]))}
       (for [agenda-num (range @(rf/subscribe [:agenda/number-of-forms]))]
         [:div {:key agenda-num}
          [new-agenda-form agenda-num]])
       [:div.agenda-line]
       [add-agenda-button]
       [:br]
       [:br]
       [:br]
       [submit-agenda-button]]]]]])

;; #### Events ####

(rf/reg-event-fx
  :send-agendas
  (fn [{:keys [db]} _]
    (let [meeting-id (get-in db [:meeting/added :db/id])
          meeting-hash (get-in db [:meeting/added :meeting/share-hash])
          edit-hash (get-in db [:meeting/added :meeting/edit-hash])]
      {:http-xhrio {:method :post
                    :uri (str (:rest-backend config) "/agendas/add")
                    :params {:agendas (vals (get-in db [:agenda :all] []))
                             :meeting-id meeting-id
                             :meeting-hash meeting-hash}
                    :format (ajax/transit-request-format)
                    :response-format (ajax/transit-response-format)
                    :on-success [:on-successful-agenda-add meeting-hash edit-hash]
                    :on-failure [:ajax-failure]}})))

(rf/reg-event-fx
  :on-successful-agenda-add
  (fn [_ [_ meeting-hash edit-hash]]
    {:dispatch-n [[:clear-current-agendas]
                  [:reset-temporary-agendas]
                  [:navigate :routes.meeting.created {:share-hash meeting-hash
                                                      :admin-hash edit-hash}]]}))

(defn load-agenda-fn [hash on-success-event]
  {:http-xhrio {:method :get
                :uri (str (:rest-backend config) "/agendas/by-meeting-hash/" hash)
                :format (ajax/transit-request-format)
                :response-format (ajax/transit-response-format)
                :on-success [on-success-event]
                :on-failure [:ajax-failure]}})

(rf/reg-event-fx
  :load-agendas
  (fn [_ [_ hash]]
    (load-agenda-fn hash :set-current-agendas)))

(rf/reg-event-fx
  :load-agenda-information
  (fn [{:keys [db]} [_ share-hash discussion-id]]
    (when-not (-> db :agenda :chosen)
      {:http-xhrio {:method :get
                    :uri (gstring/format "%s/agenda/%s/%s" (:rest-backend config) share-hash discussion-id)
                    :format (ajax/transit-request-format)
                    :response-format (ajax/transit-response-format)
                    :on-success [:set-response-as-agenda]
                    :on-failure [:agenda-not-available]}})))

(rf/reg-event-fx
  :agenda-not-available
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:error :ajax] "Agenda could not be loaded, please refresh the App.")
     :dispatch [:navigate :routes/meetings]}))

(rf/reg-event-db
  :set-current-agendas
  (fn [db [_ response]]
    (assoc-in db [:agendas :current] response)))

(rf/reg-event-db
  :clear-current-agendas
  (fn [db _]
    (assoc-in db [:agendas :current] [])))

(rf/reg-event-db
  :increase-agenda-forms
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
  :reset-temporary-agendas
  (fn [db _]
    (assoc db :agenda {:number-of-forms 1 :all {}})))

(rf/reg-event-db
  :choose-agenda
  (fn [db [_ agenda]]
    (assoc-in db [:agenda :chosen] agenda)))

(rf/reg-event-db
  :set-response-as-agenda
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