(ns schnaq.interface.views.meeting.meetings
  (:require [ajax.core :as ajax]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.meeting.create :as create-view]
            [schnaq.interface.views.pages :as pages]))

;; #### Events ####

(rf/reg-event-fx
  :meeting.creation/added-continue-with-agendas
  (fn [{:keys [db]} [_ {:keys [new-meeting]}]]
    (let [share-hash (:meeting/share-hash new-meeting)
          edit-hash (:meeting/edit-hash new-meeting)]
      {:db (-> db
               (assoc-in [:meeting :last-added] new-meeting)
               (update :meetings conj new-meeting))
       :fx [[:dispatch [:navigation/navigate :routes.meeting/admin-center
                        {:share-hash share-hash
                         :edit-hash edit-hash}]]
            [:dispatch [:meeting/select-current new-meeting]]
            [:dispatch [:notification/add
                        #:notification{:title (labels :meeting/created-success-heading)
                                       :body (labels :meeting/created-success-subheading)
                                       :context :success}]]
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
                        :on-failure [:ajax.error/as-notification]}]]}))

(rf/reg-event-fx
  :meeting.creation/new
  (fn [{:keys [db]} [_ {:meeting/keys [title description] :as new-meeting}]]
    (let [nickname (get-in db [:user :name] "Anonymous")
          agendas (get-in db [:agenda :creating :all] [])
          stub-agendas [{:title title
                         :description description
                         :agenda/rank 1}]
          agendas-to-send (if (zero? (count agendas)) stub-agendas (vals agendas))]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/meeting/add")
                          :params {:nickname nickname
                                   :meeting new-meeting
                                   :agendas agendas-to-send}
                          :format (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success [:meeting.creation/added-continue-with-agendas]
                          :on-failure [:ajax.error/as-notification]}]]})))

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
                        :on-failure [:ajax.error/as-notification]}]]}))

(rf/reg-event-fx
  ;; Response tells whether the user is allowed to see the view. (Actions are still checked by
  ;; the backend every time)
  :meeting/check-admin-credentials-success
  (fn [_ [_ {:keys [valid-credentials?]}]]
    (when-not valid-credentials?
      {:fx [[:dispatch [:navigation/navigate :routes/forbidden-page]]]})))

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
    {:fx [[:dispatch [:ajax.error/as-notification response]]
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
