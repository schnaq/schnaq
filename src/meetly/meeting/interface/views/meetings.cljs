(ns meetly.meeting.interface.views.meetings
  (:require [re-frame.core :as rf]
            [oops.core :refer [oget]]
            [ajax.core :as ajax]
            [meetly.meeting.interface.views.agenda :as agenda-views]))

;; #### Helpers ####

(defn- new-meeting-helper
  [form-elements]
  (rf/dispatch
    [:new-meeting
     {:title (oget form-elements [:title :value])
      :description (oget form-elements [:description :value])
      :end-date (.getTime (js/Date. (oget form-elements [:end-date :value])))
      :share-hash (str (random-uuid))
      :start-date (.now js/Date)}]))

;; #### Views ####

(defn create-meeting-form []
  [:div.create-meeting-form
   [:form {:on-submit (fn [e] (.preventDefault e)
                        (new-meeting-helper (oget e [:target :elements]))
                        (rf/dispatch [:navigate :routes/meetings.agenda]))}
    [:label {:for "title"} "Title: "]
    [:input#title {:type "text" :name "title"}] [:br]
    [:label {:for "description"} "Description: "]
    [:textarea#description {:name "description"}] [:br]
    [:label {:for "end-date"} "End Date: "]
    [:input#end-date {:type "datetime-local" :name "end-date"}] [:br]
    [:input {:type "submit" :value "Step 2: Add Agenda"}]]])

(defn single-meeting
  []
  (let [current-meeting @(rf/subscribe [:selected-meeting])]
    [:div
     [:h2 (:title current-meeting)]
     [:p (:description current-meeting)]
     [:hr]
     [agenda-views/agenda-in-meeting]]))

(defn meetings-list []
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
        [:button
         {:on-click (fn []
                      (rf/dispatch [:navigate :routes/meetings.show
                                    {:share-hash (:share-hash meeting)}])
                      (rf/dispatch [:select-current-meeting meeting]))}
         "Go to Meetly: " (:share-hash meeting)]
        [:hr]]))])

(defn meetings-view []
  [:div
   [:h1 "Meetly Central"]
   [:hr]
   [:h2 "Meeting controls"]
   [create-meeting-form]
   [:hr]
   [meetings-list]
   [:hr]])

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
                    :uri (str "http://localhost:3000/meeting/by-hash/" hash)
                    :format (ajax/json-request-format)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [:select-current-meeting]
                    :on-failure [:ajax-failure]}})))

(rf/reg-event-fx
  :new-meeting
  (fn [{:keys [db]} [_ meeting]]
    {:db (update db :meetings conj meeting)
     :http-xhrio {:method :post
                  :uri "http://localhost:3000/meeting/add"
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