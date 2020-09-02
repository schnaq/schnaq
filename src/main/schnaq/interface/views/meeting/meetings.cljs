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
    [:meeting.creation/new
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
  :meeting.creation/added-continue-with-agendas
  (fn [{:keys [db]} [_ {:keys [new-meeting]}]]
    (let [share-hash (:meeting/share-hash new-meeting)
          edit-hash (:meeting/edit-hash new-meeting)]
      {:db (-> db
               (assoc-in [:meeting :last-added] new-meeting)
               (update :meetings conj new-meeting))
       :fx [[:dispatch [:navigation/navigate :routes.agenda/add
                        {:share-hash share-hash}]]
            [:dispatch [:meeting/select-current new-meeting]]
            [:localstorage/write [:meeting.last-added/share-hash share-hash]]
            [:localstorage/write [:meeting.last-added/edit-hash edit-hash]]]})))

(rf/reg-event-fx
  :meeting.creation/create-stub-agenda
  (fn [{:keys [db]} [_ {:meeting/keys [title description]}]]
    {:db (assoc-in db [:agenda :all] {0 {:title title
                                         :description description}})
     :fx [[:dispatch [:send-agendas]]]}))

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
  :meeting.creation/new
  (fn [_ [_ meeting]]
    {:fx [[:dispatch [:meeting.creation/new-meeting-http-call meeting
                      :meeting.creation/added-continue-with-agendas]]]}))

(rf/reg-event-fx
  :meeting.creation/new-meeting-http-call
  (fn [{:keys [db]} [_ meeting on-success-event]]
    (let [nickname (get-in db [:user :name] "Anonymous")]
      {:http-xhrio {:method :post
                    :uri (str (:rest-backend config) "/meeting/add")
                    :params {:nickname nickname
                             :meeting meeting}
                    :format (ajax/transit-request-format)
                    :response-format (ajax/transit-response-format)
                    :on-success [on-success-event]
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
  :meeting/save-as-last-added
  (fn [db [_ {:keys [meeting]}]]
    (assoc-in db [:meeting :last-added] meeting)))

(rf/reg-sub
  :meeting/last-added
  (fn [db _]
    (get-in db [:meeting :last-added])))

(rf/reg-event-fx
  :meeting/error-remove-hashes
  (fn [_ [_ response]]
    {:fx [[:dispatch [:ajax-failure response]]
          [:localstorage/remove [:meeting.last-added/edit-hash]]
          [:localstorage/remove [:meeting.last-added/share-hash]]]}))

(rf/reg-event-fx
  :meeting/load-by-hash-as-admin
  (fn [_ [_ share-hash edit-hash]]
    {:http-xhrio {:method :post
                  :uri (str (:rest-backend config) "/meeting/by-hash-as-admin")
                  :params {:share-hash share-hash
                           :edit-hash edit-hash}
                  :format (ajax/transit-request-format)
                  :response-format (ajax/transit-response-format)
                  :on-success [:meeting/save-as-last-added]
                  :on-failure [:meeting/error-remove-hashes]}}))