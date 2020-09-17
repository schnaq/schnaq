(ns schnaq.interface.views.meeting.meetings
  (:require [ajax.core :as ajax]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :as data]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.agenda.agenda :as agenda]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.views.text-editor.view :as editor]
            ))

;; #### Helpers ####

(defn- new-meeting-helper
  "Creates a new meeting with the form from `create-meeting-form`."
  [title description]
  (rf/dispatch
    [:meeting.creation/new
     {:meeting/title title
      :meeting/description description
      :meeting/end-date (js/Date. (str "2016-05-28T13:37"))
      :meeting/start-date (js/Date.)}]))

;; #### Views ####

(defn- header []
  [base/header
   (data/labels :meeting-create-header)])

(defn- meeting-title-input
  "The input and label for a new meeting-title"
  []
  [:<>
   ;[:label {:for "meeting-title"} (data/labels :meeting-form-title)] [:br]
   [:input#meeting-title.form-control.form-title.form-border-bottom.mb-2
    {:type "text"
     :autoComplete "off"
     :required true
     :placeholder (data/labels :meeting-form-title-placeholder)}]])

(defn- submit-meeting-button []
  [:button.btn.button-primary (data/labels :meeting-create-header)])

(defn- create-meeting-form-view
  "A view with a form that creates a meeting and optional agendas."
  []
  (let [number-of-forms @(rf/subscribe [:agenda/number-of-forms])
        description-storage-key :meeting.create/description]
    [:div#create-meeting-form
     [base/nav-header]
     [header]
     [:div.container.py-3
      [:form
       {:on-submit (fn [e]
                     (let [title (oget e [:target :elements :meeting-title :value])
                           description @(rf/subscribe [:mde/load-content description-storage-key])]
                       (js-wrap/prevent-default e)
                       (new-meeting-helper title description)))}
       [:div.agenda-meeting-container.shadow-straight.text-left.p-3
        [meeting-title-input]
        [editor/view description-storage-key]]
       [:div.agenda-container.text-center
        (for [agenda-num (range number-of-forms)]
          [:div {:key agenda-num}
           [agenda/new-agenda-form agenda-num]])
        [:div.agenda-line]
        [agenda/add-agenda-button number-of-forms :agenda/increase-form-num]
        [submit-meeting-button]]]]]))

(defn create-meeting-view []
  [create-meeting-form-view])

;; #### Events ####

(rf/reg-event-fx
  :meeting.creation/added-continue-with-agendas
  (fn [{:keys [db]} [_ {:keys [new-meeting]}]]
    (let [share-hash (:meeting/share-hash new-meeting)
          edit-hash (:meeting/edit-hash new-meeting)]
      {:db (-> db
               (assoc-in [:meeting :last-added] new-meeting)
               (update :meetings conj new-meeting))
       :fx [[:dispatch [:navigation/navigate :routes.meeting/created
                        {:share-hash share-hash
                         :edit-hash edit-hash}]]
            [:dispatch [:meeting/select-current new-meeting]]
            [:localstorage/write [:meeting.last-added/share-hash share-hash]]
            [:localstorage/write [:meeting.last-added/edit-hash edit-hash]]
            [:dispatch [:agenda/clear-current]]
            [:dispatch [:agenda/reset-temporary-entries]]]})))

(rf/reg-event-fx
  :meeting/select-current
  (fn [{:keys [db]} [_ meeting]]
    {:db (assoc-in db [:meeting :selected] meeting)
     :fx [[:dispatch [:meeting.visited/to-localstorage (:meeting/share-hash meeting)]]]}))

(rf/reg-sub
  :meeting/selected
  (fn [db _]
    (get-in db [:meeting :selected])))

(rf/reg-event-fx
  :meeting/load-by-share-hash
  (fn [_ [_ hash]]
    {:fx [[:http-xhrio {:method :get
                        :uri (str (:rest-backend config) "/meeting/by-hash/" hash)
                        :format (ajax/transit-request-format)
                        :response-format (ajax/transit-response-format)
                        :on-success [:meeting/select-current]
                        :on-failure [:ajax-failure]}]]}))

(rf/reg-event-fx
  :meeting.creation/new
  (fn [{:keys [db]} [_ {:meeting/keys [title description] :as new-meeting}]]
    (let [nickname (get-in db [:user :name] "Anonymous")
          agendas (get-in db [:agenda :all] [])
          stub-agendas [{:title title
                         :description description}]
          agendas-to-send (if (zero? (count agendas)) stub-agendas (vals agendas))]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/meeting/add")
                          :params {:nickname nickname
                                   :meeting new-meeting
                                   :agendas agendas-to-send}
                          :format (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success [:meeting.creation/added-continue-with-agendas]
                          :on-failure [:ajax-failure]}]]})))

(rf/reg-event-fx
  :meeting/check-admin-credentials
  (fn [_ [_ share-hash edit-hash]]
    {:fx [[:http-xhrio {:method :post
                        :uri (str (:rest-backend config) "/credentials/validate")
                        :params {:share-hash share-hash
                                 :edit-hash edit-hash}
                        :format (ajax/transit-request-format)
                        :response-format (ajax/transit-response-format)
                        :on-success [:meeting/check-admin-credentials-success]
                        :on-failure [:ajax-failure]}]]}))

(rf/reg-event-fx
  ;; Response tells whether the user is allowed to see the view. (Actions are still checked by
  ;; the backend every time)
  :meeting/check-admin-credentials-success
  (fn [_ [_ {:keys [valid-credentials?]}]]
    (when-not valid-credentials?
      {:fx [[:dispatch [:navigation/navigate :routes/invalid-link]]]})))

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
    {:fx [[:http-xhrio {:method :post
                        :uri (str (:rest-backend config) "/meeting/by-hash-as-admin")
                        :params {:share-hash share-hash
                                 :edit-hash edit-hash}
                        :format (ajax/transit-request-format)
                        :response-format (ajax/transit-response-format)
                        :on-success [:meeting/save-as-last-added]
                        :on-failure [:meeting/error-remove-hashes]}]]}))
