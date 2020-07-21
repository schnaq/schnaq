(ns meetly.meeting.interface.views.meetings
  (:require [re-frame.core :as rf]
            [oops.core :refer [oget]]
            [ajax.core :as ajax]
            [meetly.meeting.interface.views.agenda :as agenda-views]
            [meetly.meeting.interface.text.display-data :as data]
            [meetly.meeting.interface.views.base :as base]
            [meetly.meeting.interface.config :refer [config]]))

;; #### Helpers ####

(defn- new-meeting-helper
  "Creates a new meeting with the form from `create-meeting-form`."
  [form-elements]
  (rf/dispatch
    [:new-meeting
     {:title (oget form-elements [:title :value])
      :description (oget form-elements [:description :value])
      :end-date (.getTime (js/Date. (oget form-elements [:end-date :value])))
      :share-hash (str (random-uuid))
      :start-date (.now js/Date)}]))

;; #### header ####

(defn- header []
  (base/header
    (data/labels :meeting-create-header)
    (data/labels :meeting-create-subheader)))

;; date picker

(defn- date-picker []
  [:div
   [:label (data/labels :meeting-form-deadline)]
   [:div.row
    [:div.col-sm-3
     [:input#end-date.form-control.form-round {:type "datetime-local"
                                               :name "end-date"}]]]])

;; #### Views ####

(defn create-meeting-form-view
  "A view with a form that creates a meeting properly."
  []
  [:div.create-meeting-form
   [header]
   [:div.container.px-5.py-3
    ;; form
    [:form {:on-submit (fn [e] (.preventDefault e)
                         (new-meeting-helper (oget e [:target :elements]))
                         (rf/dispatch [:navigate :routes/meetings.agenda]))}
     ;; title
     [:label {:for "title"} (data/labels :meeting-form-title)] [:br]
     [:input#title.form-control.form-round.form-title
      {:type "text" :placeholder (data/labels :meeting-form-title-placeholder)}]
     [:br] [:br]

     ;; description
     [:label {:for "description"} (data/labels :meeting-form-desc)] [:br]
     [:textarea#description.form-control.form-round
      {:rows "6" :placeholder (data/labels :meeting-form-desc-placeholder)}]
     [:br] [:br]

     ;; date
     [date-picker]
     ;; submit
     [:input.button-secondary {:type "submit" :value "Step 2: Add Agenda"}]]]])

(defn single-meeting-view
  "Show a single meeting and all its Agendas."
  []
  (let [current-meeting @(rf/subscribe [:selected-meeting])]
    [:div
     [:h2 (:title current-meeting)]
     [:p (:description current-meeting)]
     [:hr]
     [agenda-views/agenda-in-meeting-view]]))

(defn- meetings-list-view
  "Shows a list of all meetings."
  []
  [:div.meetings-list
   [:h3 "Meetings"]
   (let [meetings @(rf/subscribe [:meetings])]
     (for [meeting meetings]
       [:div {:key (random-uuid)}
        [:p (:title meeting) " - " (:description meeting)]
        [:p "Start: "
         ;; TODO use joda.time in final application
         (str (js/Date. (js/Number. (:start-date meeting)))) " - End Date: "
         (str (js/Date. (js/Number. (:end-date meeting))))]
        [:p "ID: " (:id meeting)]
        [:button.btn.btn-success
         {:on-click (fn []
                      (rf/dispatch [:navigate :routes/meetings.show
                                    {:share-hash (:share-hash meeting)}])
                      (rf/dispatch [:select-current-meeting meeting]))}
         "Go to Meetly: " (:share-hash meeting)]
        [:hr]]))])


(defn meeting-view
  "Shows the page for an overview of all meetings"
  []
  [:div.container
   [base/username-bar-view]
   [meetings-list-view]])

;; #### Events ####

(rf/reg-event-db
  :meeting-added
  (fn [db [_ meeting response]]
    (assoc db :meeting/added
              (assoc meeting :id (:id-created response)))))

(rf/reg-event-db
  :select-current-meeting
  (fn [db [_ meeting]]
    (assoc-in db [:meeting :selected] meeting)))

(rf/reg-event-fx
  :load-meeting-by-share-hash
  (fn [{:keys [db]} [_ hash]]
    (when-not (get-in db [:meeting :selected])
      {:http-xhrio {:method :get
                    :uri (str (:rest-backend config) "/meeting/by-hash/" hash)
                    :format (ajax/json-request-format)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [:select-current-meeting]
                    :on-failure [:ajax-failure]}})))

(rf/reg-event-fx
  :new-meeting
  (fn [{:keys [db]} [_ meeting]]
    {:db (update db :meetings conj meeting)
     :http-xhrio {:method :post
                  :uri (str (:rest-backend config) "/meeting/add")
                  :params {:meeting meeting}
                  :format (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:meeting-added meeting]
                  :on-failure [:ajax-failure]}}))

;; #### Subs ####

(rf/reg-sub
  :meetings
  (fn [db _]
    (:meetings db)))

(rf/reg-sub
  :meeting/last-added
  (fn [db _]
    (:meeting/added db)))

(rf/reg-sub
  :selected-meeting
  (fn [db _]
    (get-in db [:meeting :selected])))