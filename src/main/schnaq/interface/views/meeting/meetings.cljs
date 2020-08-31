(ns schnaq.interface.views.meeting.meetings
  (:require [ajax.core :as ajax]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :as data]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.base :as base]))

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
  (let [with-agendas? @(rf/subscribe [:meeting.creation/with-agendas?])]
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
       [:br]

       [:div.custom-control.custom-switch
        [:input#agenda-switch.custom-control-input {:type "checkbox"
                                                    :on-click #(rf/dispatch [:meeting.creation/toggle-agendas])}]
        [:label.custom-control-label {:for "agenda-switch"}
         (if with-agendas?
           (data/labels :meeting.creation/with-agendas)
           (data/labels :meeting.creation/without-agendas))]]
       [:br]
       ;; submit
       [:button.button-secondary.mt-5.mb-1 {:type "submit"}
        (data/labels :meeting.step2/button)]]]]))

;; #### Events ####

(rf/reg-event-fx
  :meeting-added
  (fn [{:keys [db]} [_ {:keys [new-meeting]}]]
    {:db (-> db
             (assoc-in [:meeting :last-added] new-meeting)
             (update :meetings conj new-meeting))
     :dispatch-n [[:navigation/navigate :routes.agenda/add
                   {:share-hash (:meeting/share-hash new-meeting)}]
                  [:meeting/select-current new-meeting]]}))

(rf/reg-event-db
  :meeting/select-current
  (fn [db [_ meeting]]
    (assoc-in db [:meeting :selected] meeting)))

(rf/reg-sub
  :meeting/selected
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
                    :on-success [:meeting/select-current]
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
      {:dispatch [:navigation/navigate :routes/invalid-link]})))

(rf/reg-event-db
  :meeting.creation/toggle-agendas
  (fn [db _]
    (update-in db [:meeting :creation :with-agendas?] not)))

(rf/reg-sub
  :meeting.creation/with-agendas?
  (fn [db _]
    (get-in db [:meeting :creation :with-agendas?] false)))
