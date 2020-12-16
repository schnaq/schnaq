(ns schnaq.interface.views.agenda.edit
  (:require [ajax.core :as ajax]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.views.agenda.agenda :as agenda]
            [schnaq.interface.views.modals.modal :as modal]))

(defn- suggestions-table
  "Show all suggestions."
  [suggestions suggestion-type addition?]
  [:table.table
   [:thead
    [:tr
     [:th {:width "15%"} (labels :suggestions.modal.table/nickname)]
     [:th {:width "20%"} (labels :suggestions.modal.table/suggestion-title)]
     [:th {:width "60%"} (labels :suggestions.modal.table/suggestion-description)]
     [:th {:width "5%"} (labels :suggestions.modal.table/suggestion-accept)]]]
   [:tbody
    (for [suggestion suggestions]
      (let [get-value #(suggestion (keyword (str (name suggestion-type) "/" %)))]
        [:tr {:key (:db/id suggestion)}
         [:td (get-value "ideator")]
         [:td (get-value "title")]
         [:td (get-value "description")]
         [:td.text-center
          [:button.btn.btn-success
           {:on-click #(rf/dispatch [:suggestions.update/accept suggestion suggestion-type addition?])}
           [:i {:class (str "far " (fa :check/square))
                :style {:font-size "150%"}}]]]]))]])

(rf/reg-event-fx
  :suggestions.update/accept
  (fn [_ [_ suggestion suggestion-type addition?]]
    (if (= suggestion-type :agenda.suggestion)
      (if addition?
        {:fx [[:dispatch [:suggestion.new.agenda/accept suggestion]]]}
        {:fx [[:dispatch [:suggestion.update.agenda/accept suggestion]]]})
      {:fx [[:dispatch [:suggestion.update.meeting/accept suggestion]]]})))

(rf/reg-event-fx
  :suggestion.update.agenda/accept
  (fn [{:keys [db]} [_ suggestion]]
    (let [{:keys [share-hash edit-hash]} (-> db :current-route :path-params)
          {:agenda.suggestion/keys [agenda title description rank]} suggestion
          new-agenda {:db/id (:db/id agenda)
                      :agenda/title title
                      :agenda/description description
                      :agenda/rank rank}]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/agenda/update")
                          :params {:agenda new-agenda
                                   :share-hash share-hash
                                   :edit-hash edit-hash}
                          :format (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success [:suggestion.update.agenda/success]
                          :on-failure [:ajax.error/as-notification]}]]})))

(rf/reg-event-fx
  :suggestion.update.agenda/success
  (fn [_ [_ response]]
    {:fx [[:dispatch [:agenda/update-edit-form :agenda/title (:db/id response) (:agenda/title response)]]
          [:dispatch [:agenda/update-edit-form :agenda/description (:db/id response) (:agenda/description response)]]
          [:dispatch [:agenda.edit/set-agenda-description-update (:db/id response) (:agenda/description response)]]
          [:dispatch [:notification/add
                      #:notification{:title (labels :suggestions.update.agenda/success-title)
                                     :body (labels :suggestions.update.agenda/success-body)
                                     :context :success}]]]}))

(rf/reg-event-fx
  :suggestion.new.agenda/accept
  (fn [{:keys [db]} [_ suggestion]]
    (let [{:keys [share-hash edit-hash]} (-> db :current-route :path-params)
          {:agenda.suggestion/keys [title description meeting rank]} suggestion
          new-agenda {:agenda/title title
                      :agenda/description description
                      :agenda/meeting meeting
                      :agenda/rank rank}]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/agenda/new")
                          :params {:agenda new-agenda
                                   :share-hash share-hash
                                   :edit-hash edit-hash}
                          :format (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success [:suggestion.new.agenda/success]
                          :on-failure [:ajax.error/as-notification]}]]})))

(rf/reg-event-fx
  :suggestion.new.agenda/success
  (fn [{:keys [db]} [_ response]]
    (let [new-agendas (conj (get-in db [:edit-meeting :agendas]) response)
          sorted-agendas (sort-by :agenda/rank new-agendas)]
      {:db (assoc-in db [:edit-meeting :agendas] sorted-agendas)
       :fx [[:dispatch [:notification/add
                        #:notification{:title (labels :suggestions.update.agenda/success-title)
                                       :body (labels :suggestions.update.agenda/success-body)
                                       :context :success}]]]})))

(rf/reg-event-fx
  :suggestion.update.meeting/accept
  (fn [{:keys [db]} [_ suggestion]]
    (let [{:keys [share-hash edit-hash]} (-> db :current-route :path-params)
          {:meeting.suggestion/keys [meeting title description]} suggestion
          new-meeting {:db/id (:db/id meeting)
                       :meeting/title title
                       :meeting/description description}]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/meeting/info/update")
                          :params {:meeting new-meeting
                                   :share-hash share-hash
                                   :edit-hash edit-hash
                                   :nickname (get-in db [:user :nickname] "Anonymous")}
                          :format (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success [:suggestion.update.meeting/success]
                          :on-failure [:ajax.error/as-notification]}]]})))

(rf/reg-event-fx
  :suggestion.update.meeting/success
  (fn [_ [_ response]]
    {:fx [[:dispatch [:meeting/update-meeting-attribute :meeting/title (:meeting/title response)]]
          [:dispatch [:meeting/update-meeting-attribute :meeting/description (:meeting/description response)]]
          [:dispatch [:agenda.edit/set-meeting-description-update (:meeting/description response)]]
          [:dispatch [:notification/add
                      #:notification{:title (labels :suggestions.update.agenda/success-title)
                                     :body (labels :suggestions.update.agenda/success-body)
                                     :context :success}]]]}))

(defn- suggestions-modal
  "Open a modal containing the suggested changes."
  ([suggestions suggestion-type modal-title]
   [suggestions-modal suggestions suggestion-type modal-title false])
  ([suggestions suggestion-type modal-title addition?]
   [modal/modal-template
    modal-title
    [:<>
     [:p (labels :suggestions.modal/primer)]
     [suggestions-table suggestions suggestion-type addition?]]]))

(defn- update-suggestions-badge
  ;; todo del
  "Show update-suggestion badge."
  [selected-entity subscription-key suggestions-namespace]
  (when-let [suggestions @(rf/subscribe [subscription-key (:db/id selected-entity)])]
    [:span.badge.badge-pill.mr-2.badge-clickable.clickable
     {:title (labels :suggestions.modal/header)
      :on-click #(rf/dispatch [:modal {:show? true
                                       :large? true
                                       :child [suggestions-modal suggestions suggestions-namespace
                                               (labels :suggestions.modal.update/title)]}])}
     [:i {:class (str "m-auto fas " (fa :edit))}] " "
     (count suggestions)]))

(defn- deletion-modal [suggestions]
  [modal/modal-template
   (labels :suggestions.modal.delete/title)
   [:<>
    [:p (labels :suggestions.modal/primer-delete)]
    [:ul
     (for [suggestion suggestions]
       [:li {:key (:db/id suggestion)}
        (:agenda.suggestion/ideator suggestion)])]
    [:button.btn.btn-danger.btn-large
     {:on-click #(rf/dispatch [:suggestion.agenda/delete (first suggestions)])}
     [:i {:class (str "far " (fa :trash))}] " " (labels :suggestions.modal.delete/button)]]])

(defn- meeting-feedback-modal [feedback]
  [modal/modal-template
   (labels :suggestions.feedback/title)
   [:<>
    [:p (labels :suggestions.feedback/primer)]
    [:table.table
     [:thead
      [:tr
       [:th {:width "30%"} (labels :suggestions.feedback.table/nickname)]
       [:th {:width "70%"} (labels :suggestions.feedback.table/content)]]]
     [:tbody
      (for [single-feedback feedback]
        [:tr {:key (:db/id single-feedback)}
         [:td (:meeting.feedback/ideator single-feedback)]
         [:td (:meeting.feedback/content single-feedback)]])]]]])

(rf/reg-event-fx
  :suggestion.agenda/delete
  (fn [{:keys [db]} [_ suggestion]]
    (let [{:keys [share-hash edit-hash]} (-> db :current-route :path-params)
          {:agenda.suggestion/keys [agenda]} suggestion]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/agenda/delete")
                          :params {:agenda-id (:db/id agenda)
                                   :share-hash share-hash
                                   :edit-hash edit-hash}
                          :format (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success [:suggestion.agenda/delete-success (:db/id agenda)]
                          :on-failure [:ajax.error/as-notification]}]]})))

(rf/reg-event-fx
  :suggestion.agenda/delete-success
  (fn [{:keys [db]} [_ agenda-id _response]]
    {:db (update-in db [:edit-meeting :agendas] (fn [as] (remove #(= (:db/id %) agenda-id) as)))
     :fx [[:dispatch [:notification/add
                      #:notification{:title (labels :suggestions.agenda/delete-title)
                                     :body (labels :suggestions.agenda/delete-body)
                                     :context :success}]]
          [:dispatch [:modal {:show? false :child nil}]]]}))

(defn- deletion-badge
  ;; todo del
  "Badge containing the wishes of users to delete the agenda point."
  [agenda]
  (when-let [delete-suggestions @(rf/subscribe [:suggestions/agenda-delete (:db/id agenda)])]
    [:span.badge.badge-pill.mr-2.badge-clickable.clickable
     {:title (labels :suggestions.modal.delete/title)
      :on-click #(rf/dispatch [:modal {:show? true
                                       :large? true
                                       :child [deletion-modal delete-suggestions]}])}
     [:i {:class (str "m-auto fas " (fa :trash))}] " "
     (count delete-suggestions)]))

(defn- addition-badge
  ;; todo del
  "Badge indicating an addition of an agenda point to the meeting."
  []
  (when-let [suggestions @(rf/subscribe [:suggestions/agenda-new])]
    [:span.badge.badge-pill.mr-2.badge-clickable.clickable
     {:title (labels :suggestions.modal/header)
      :on-click #(rf/dispatch [:modal {:show? true
                                       :large? true
                                       :child [suggestions-modal suggestions :agenda.suggestion
                                               (labels :suggestions.modal.new/title) true]}])}
     [:i {:class (str "m-auto fas " (fa :add))}] " "
     (count suggestions)]))

(defn- feedback-badge
  ;; todo del
  "Badge indicating free-text feedback from users."
  []
  (when-let [feedback @(rf/subscribe [:suggestions/feedback])]
    [:span.badge.badge-pill.mr-2.badge-clickable.clickable
     {:title (labels :suggestions.feedback/header)
      :on-click #(rf/dispatch [:modal {:show? true
                                       :large? true
                                       :child [meeting-feedback-modal feedback]}])}
     [:i {:class (str "m-auto fas " (fa :comment))}] " "
     (count feedback)]))

(rf/reg-event-fx
  :agenda/load-for-edit
  (fn [_ [_ share-hash]]
    (agenda/load-agenda-fn share-hash :agenda/load-for-edit-success)))

(rf/reg-event-db
  :agenda/load-for-edit-success
  (fn [db [_ {:keys [agendas]}]]
    (assoc db :edit-meeting {:agendas (sort-by :agenda/rank agendas)
                             :meeting (get-in db [:meeting :selected])
                             :delete-agendas #{}}
              :edit-meeting-updates {})))

(rf/reg-sub
  ;; todo del
  :agenda.edit/meeting-description-update
  (fn [db _]
    (-> db :edit-meeting-updates :meeting-description)))

(rf/reg-event-db
  :agenda.edit/set-meeting-description-update
  (fn [db [_ value]]
    (assoc-in db [:edit-meeting-updates :meeting-description] value)))

(rf/reg-sub
  :agenda.edit/agenda-description-update
  (fn [db [_ id]]
    (get-in db [:edit-meeting-updates :agenda-descriptions id])))

(rf/reg-event-db
  :agenda.edit/set-agenda-description-update
  (fn [db [_ id value]]
    (assoc-in db [:edit-meeting-updates :agenda-descriptions id] value)))

(rf/reg-event-db
  :agenda.edit/reset-editor-update-flag
  (fn [db _]
    (assoc db :edit-meeting-updates {})))

;; add agenda form event

(rf/reg-event-db
  :agenda/add-edit-form
  (fn [db _]
    (let [all-temp-agendas (get-in db [:edit-meeting :agendas])
          all-ranks (conj (map :agenda/rank all-temp-agendas) 0)
          biggest-rank (apply max all-ranks)]
      (update-in db [:edit-meeting :agendas]
                 #(concat % [{:db/id (str (random-uuid))
                              :agenda/title ""
                              :agenda/description ""
                              :agenda/rank (inc biggest-rank)
                              :agenda/meeting (get-in db [:edit-meeting :meeting :db/id])}])))))

;; update agenda

(rf/reg-event-db
  :agenda/update-edit-form
  (fn [db [_ attribute id new-val]]
    (let [has-id? #(= id (:db/id %))
          update-fn (fn [coll] (sort-by :agenda/rank
                                        (map #(if (has-id? %)
                                                ;; update attribute of agenda
                                                (assoc % attribute new-val)
                                                ;; do not update agenda
                                                %) coll)))]
      (update-in db [:edit-meeting :agendas] update-fn))))

;; update title

(rf/reg-event-db
  :meeting/update-meeting-attribute
  (fn [db [_ attribute new-val]]
    (update-in db [:edit-meeting :meeting] assoc attribute new-val)))

(rf/reg-sub
  :suggestions/meeting
  (fn [db [_ meeting-id]]
    (get-in db [:suggestions :meetings meeting-id])))

(rf/reg-sub
  :suggestions/agenda-updates
  (fn [db [_ agenda-id]]
    (get-in db [:suggestions :agendas :updates agenda-id])))

(rf/reg-sub
  :suggestions/agenda-delete
  (fn [db [_ agenda-id]]
    (get-in db [:suggestions :agendas :delete agenda-id])))

(rf/reg-sub
  :suggestions/agenda-new
  (fn [db _]
    (get-in db [:suggestions :agendas :new])))

(rf/reg-sub
  :suggestions/feedback
  (fn [db _]
    (get-in db [:suggestions :feedback])))

