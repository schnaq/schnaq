(ns meetly.meeting.interface.views.meeting.meetings
  (:require [re-frame.core :as rf]
            [oops.core :refer [oget]]
            [ajax.core :as ajax]
            [meetly.meeting.interface.text.display-data :as data]
            [meetly.meeting.interface.views.base :as base]
            [meetly.meeting.interface.config :refer [config]]
            [meetly.meeting.interface.utils.js-wrapper :as js-wrap]))

;; #### Helpers ####

(defn- new-meeting-helper
  "Creates a new meeting with the form from `create-meeting-form`."
  [form-elements]
  (rf/dispatch
    [:new-meeting
     {:meeting/title (oget form-elements [:title :value])
      :meeting/description (oget form-elements [:description :value])
      :meeting/end-date (js/Date. (str "2016-05-28T13:37"))
      :meeting/start-date (js/Date.)}]))

;; #### Views ####

(defn- header []
  (base/header
    (data/labels :meeting-create-header)
    (data/labels :meeting-create-subheader)))

(defn create-meeting-form-view
  "A view with a form that creates a meeting properly."
  []
  [:div#create-meeting-form
   [base/nav-header]
   [header]
   [:div.container.px-5.py-3
    ;; form
    [:form {:on-submit (fn [e] (js-wrap/prevent-default e)
                         (new-meeting-helper (oget e [:target :elements])))}
     ;; title
     [:label {:for "title"} (data/labels :meeting-form-title)] [:br]
     [:input#title.form-control.form-round.form-title
      {:type "text"
       :autoComplete "off"
       :required true
       :placeholder (data/labels :meeting-form-title-placeholder)}]
     [:br] [:br]

     ;; description
     [:label {:for "description"} (data/labels :meeting-form-desc)] [:br]
     [:textarea#description.form-control.form-round
      {:rows "6" :placeholder (data/labels :meeting-form-desc-placeholder)}]
     [:br] [:br]

     ;; submit
     [:button.button-secondary.mt-5.mb-1 {:type "submit"}
      (data/labels :meeting.step2/button)]]]])

;; #### Events ####

(rf/reg-event-fx
  :meeting-added
  (fn [{:keys [db]} [_ {:keys [new-meeting]}]]
    {:db (-> db
             (assoc :meeting/added new-meeting)
             (update :meetings conj new-meeting))
     :dispatch-n [[:navigate :routes/meetings.agenda
                   {:share-hash (:meeting/share-hash new-meeting)}]
                  [:select-current-meeting new-meeting]]}))

(rf/reg-event-db
  :select-current-meeting
  (fn [db [_ meeting]]
    (assoc-in db [:meeting :selected] meeting)))

(rf/reg-sub
  :selected-meeting
  (fn [db _]
    (get-in db [:meeting :selected])))

(rf/reg-event-fx
  :load-meeting-by-share-hash
  (fn [{:keys [db]} [_ hash]]
    (when-not (get-in db [:meeting :selected])
      {:http-xhrio {:method :get
                    :uri (str (:rest-backend config) "/meeting/by-hash/" hash)
                    :format (ajax/transit-request-format)
                    :response-format (ajax/transit-response-format)
                    :on-success [:select-current-meeting]
                    :on-failure [:ajax-failure]}})))

(rf/reg-event-fx
  :new-meeting
  (fn [{:keys [db]} [_ meeting]]
    (let [nickname (get-in db [:user :name] "Anonymous")]
      {:http-xhrio {:method :post
                    :uri (str (:rest-backend config) "/meeting/add")
                    :params {:nickname nickname
                             :meeting meeting}
                    :format (ajax/transit-request-format)
                    :response-format (ajax/transit-response-format)
                    :on-success [:meeting-added]
                    :on-failure [:ajax-failure]}})))

(rf/reg-event-fx
  :meeting/check-admin-credentials
  (fn [_ [_ share-hash edit-hash]]
    {:http-xhrio {:method :post
                  :uri (str (:rest-backend config) "/credentials/validate")
                  :params {:share-hash share-hash
                           :edit-hash edit-hash}
                  :format (ajax/transit-request-format)
                  :response-format (ajax/transit-response-format)
                  :on-success [:meeting/check-admin-credentials-success]
                  :on-failure [:ajax-failure]}}))

(rf/reg-event-fx
  ;; Response tells whether the user is allowed to see the view. (Actions are still checked by
  ;; the backend every time)
  :meeting/check-admin-credentials-success
  (fn [_ [_ {:keys [valid-credentials?]}]]
    (when-not valid-credentials?
      {:dispatch [:navigate :routes/invalid-link]})))

;; #### Subs ####

(rf/reg-sub
  :meeting/last-added
  (fn [db _]
    (:meeting/added db)))