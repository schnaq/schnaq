(ns schnaq.interface.views.agenda.edit
  (:require [ajax.core :as ajax]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.agenda.agenda :as agenda]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.views.modals.modal :as modal]
            [schnaq.interface.views.text-editor.view :as editor]))

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
           [:i {:class (str "far " (fa :check))
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
                          :on-failure [:ajax-failure]}]]})))

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
                          :on-failure [:ajax-failure]}]]})))

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
                          :on-failure [:ajax-failure]}]]})))

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
                          :on-failure [:ajax-failure]}]]})))

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

(defn- badge-wrapper
  "Wrap all the badges!"
  [& badges]
  (when (= :routes.meeting/edit @(rf/subscribe [:navigation/current-view]))
    [:div.my-0.p-2.display-6.text-left
     badges]))


;; -----------------------------------------------------------------------------

(defn- edit-header []
  [base/header
   (labels :agenda/edit-title)
   (labels :agenda/edit-subtitle)])

(defn- submit-edit-button []
  [:button.btn.button-primary (labels :agenda/edit-button)])

(defn edit-agenda-title-attributes [db-id agenda]
  {:type "text"
   :name "title"
   :auto-complete "off"
   :required true
   :placeholder (labels :agenda/point)
   :value (:agenda/title agenda)
   :id (str "title-" db-id)
   :on-change #(rf/dispatch [:agenda/update-edit-form :agenda/title db-id (oget % [:target :value])])})

(defn- agenda-view [agenda]
  (let [db-id (:db/id agenda)
        delete-agenda-fn #(rf/dispatch [:agenda/delete (:db/id agenda)])
        description-update-fn #(rf/dispatch [:agenda/update-edit-form :agenda/description db-id %])]
    [:<>
     [agenda/agenda-form
      delete-agenda-fn
      agenda
      description-update-fn
      (edit-agenda-title-attributes db-id agenda)
      [badge-wrapper
       (with-meta
         [update-suggestions-badge agenda :suggestions/agenda-updates :agenda.suggestion]
         {:key (str "suggestion-badge-" (:db/id agenda))})
       (with-meta
         [deletion-badge agenda]
         {:key (str "deletion-badge-" (:db/id agenda))})]]]))

(defn- editable-meeting-info [selected-meeting]
  (let [meeting-updates @(rf/subscribe [:agenda.edit/meeting-description-update])]
    [:div.agenda-meeting-container.shadow-straight.text-left.p-3
     ;; title form
     [:input#meeting-title.form-control.form-title.form-border-bottom.mb-2
      {:value (:meeting/title selected-meeting)
       :type "text"
       :name "meeting-title"
       :auto-complete "off"
       :required true
       :placeholder (labels :meeting-form-title)
       :id (str "meeting-title-" (:db/id selected-meeting))
       :on-change
       #(rf/dispatch
          [:meeting/update-meeting-attribute :meeting/title (oget % [:target :value])])}]
     ;; description form
     [editor/view
      (:meeting/description selected-meeting)
      #(rf/dispatch
         [:meeting/update-meeting-attribute :meeting/description %])
      meeting-updates]
     [badge-wrapper
      (with-meta
        [update-suggestions-badge selected-meeting :suggestions/meeting :meeting.suggestion]
        {:key (str "suggestion-badge-" (:db/id selected-meeting))})
      (with-meta
        [addition-badge]
        {:key (str "addition-badge-" (:db/id selected-meeting))})
      (with-meta
        [feedback-badge]
        {:key (str "feedback-badge-" (:db/id selected-meeting))})]]))

(defn- editable-meeting-template
  "Can be used to present an editable meeting in different views. Customize the heading
  and on-submit-function to your liking."
  ([heading on-submit-fn]
   [editable-meeting-template heading on-submit-fn [:span ""]])
  ([heading on-submit-fn extras]
   (let [edit-information @(rf/subscribe [:agenda/current-edit-info])
         edit-meeting (:meeting edit-information)
         meeting-agendas (:agendas edit-information)
         current-route @(rf/subscribe [:navigation/current-view])]
     [:<>
      [base/meeting-header edit-meeting]
      heading
      [:div.container.text-center.pb-5
       [:form {:on-submit on-submit-fn}
        ;; meeting title and description
        [editable-meeting-info edit-meeting]
        (for [agenda meeting-agendas]
          [:div {:key (str (:db/id agenda) "-" current-route)}
           [agenda-view agenda]])
        [:div.agenda-line]
        [agenda/add-agenda-button (count meeting-agendas) :agenda/add-edit-form]
        extras
        [submit-edit-button]]]])))

(defn- edit-view []
  [editable-meeting-template
   [edit-header]
   (fn [e]
     (js-wrap/prevent-default e)
     (rf/dispatch [:meeting/submit-changes]))])

(defn agenda-edit-view []
  [edit-view])

(defn- suggestion-feedback-input
  []
  [:<>
   [:div.text-left.pb-5
    [:label.text-left.form-title.text-gray-700 {:for "free-feedback"}
     [:p.mb-0 (labels :suggestion.feedback/label)]]
    [:textarea.form-control.form-round.shadow-straight
     {:rows 4
      :required false
      :id "free-feedback"}]]])

(defn- suggestion-view []
  [editable-meeting-template
   [base/header
    (labels :meetings.suggestions/header)
    (labels :meetings.suggestions/subheader)]
   (fn [e]
     (js-wrap/prevent-default e)
     (rf/dispatch [:suggestions.feedback/submit (oget e [:target :elements "free-feedback" :value])])
     (rf/dispatch [:suggestions/submit]))
   [suggestion-feedback-input]])

(defn agenda-suggestion-view []
  [suggestion-view])

(rf/reg-event-fx
  :suggestions.feedback/submit
  (fn [{:keys [db]} [_ feedback-text]]
    (let [nickname (get-in db [:user :name] "Anonymous")
          share-hash (get-in db [:current-route :path-params :share-hash])]
      (when-not (gstring/isEmptyString feedback-text)
        {:fx [[:http-xhrio {:method :post
                            :uri (str (:rest-backend config) "/meeting/feedback")
                            :params {:share-hash share-hash
                                     :feedback feedback-text
                                     :nickname nickname}
                            :format (ajax/transit-request-format)
                            :response-format (ajax/transit-response-format)
                            :on-success [:no-op]
                            :on-failure [:ajax-failure]}]]}))))

;; load agendas events

(rf/reg-event-fx
  :agenda/load-for-edit
  (fn [_ [_ share-hash]]
    (agenda/load-agenda-fn share-hash :agenda/load-for-edit-success)))

(rf/reg-event-db
  :agenda/load-for-edit-success
  (fn [db [_ agendas]]
    (assoc db :edit-meeting {:agendas (sort-by :agenda/rank agendas)
                             :meeting (get-in db [:meeting :selected])
                             :delete-agendas #{}}
              :edit-meeting-updates {})))

(rf/reg-sub
  :agenda/current-edit-info
  (fn [db _]
    (:edit-meeting db)))

(rf/reg-sub
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
  :agenda.edit/reset-edit-updates
  (fn [db _]
    (assoc db :edit-meeting-updates {}
              :edit-meeting {}
              :suggestions {})))

;; delete agenda events

(rf/reg-event-db
  :agenda/delete
  (fn [db [_ agenda-id]]
    (let [delete-fn (fn [agendas] (remove #(= agenda-id (:db/id %)) agendas))]
      (cond-> db
              true (update-in [:edit-meeting :agendas] delete-fn)
              ;; We do not need to remove temporary agendas in the backend
              (int? agenda-id) (update-in [:edit-meeting :delete-agendas] conj agenda-id)))))

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
          update-fn (fn [coll] (map #(if (has-id? %)
                                       ;; update attribute of agenda
                                       (assoc % attribute new-val)
                                       ;; do not update agenda
                                       %) coll))]
      (update-in db [:edit-meeting :agendas] update-fn))))

;; update title

(rf/reg-event-db
  :meeting/update-meeting-attribute
  (fn [db [_ attribute new-val]]
    (update-in db [:edit-meeting :meeting] assoc attribute new-val)))

;; submit changes

(rf/reg-event-fx
  :meeting/submit-changes
  (fn [{:keys [db]} _]
    (let [edit-meeting (:edit-meeting db)
          edit-hash (get-in db [:current-route :path-params :edit-hash])
          finalized-changes (assoc-in edit-meeting [:meeting :meeting/edit-hash] edit-hash)
          nickname (-> db :user :name)]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/meeting/update")
                          :params (assoc finalized-changes :nickname nickname)
                          :format (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success [:meeting/on-success-submit-changes-event finalized-changes]
                          :on-failure [:ajax-failure]}]]})))

(rf/reg-event-fx
  :meeting/on-success-submit-changes-event
  (fn [_ [_ {:keys [meeting]} _response]]
    {:fx [[:dispatch [:meeting/select-current meeting]]
          [:dispatch [:navigation/navigate :routes.meeting/show {:share-hash (:meeting/share-hash meeting)}]]]}))

(rf/reg-event-fx
  :meeting/on-success-submit-suggestions-event
  (fn [{:keys [db]} [_ _]]
    (let [share-hash (get-in db [:current-route :path-params :share-hash])]
      {:fx [[:dispatch [:notification/add
                        #:notification{:title (labels :suggestions.notification/title)
                                       :body (labels :suggestions.notification/body)
                                       :context :success}]]
            [:dispatch [:navigation/navigate :routes.meeting/show {:share-hash share-hash}]]]})))

(rf/reg-event-fx
  :suggestions/submit
  (fn [{:keys [db]} _]
    (let [edit-meeting (:edit-meeting db)
          nickname (-> db :user :name)]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/meeting/suggestions")
                          :params {:meeting (:meeting edit-meeting)
                                   :agendas (:agendas edit-meeting)
                                   :delete-agendas (:delete-agendas edit-meeting)
                                   :nickname nickname}
                          :format (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success [:meeting/on-success-submit-suggestions-event]
                          :on-failure [:ajax-failure]}]]})))

(rf/reg-event-db
  :suggestions/for-meeting-success
  (fn [db [_ {:keys [agenda.suggestion.type/update agenda.suggestion.type/new
                     agenda.suggestion.type/delete meeting.suggestions/all
                     meeting.feedback/feedback]}]]
    (let [group-by-agenda-id #(get-in % [:agenda.suggestion/agenda :db/id])
          meeting-id (:db/id (:meeting.suggestion/meeting (first all)))]
      (-> db
          (assoc-in [:suggestions :feedback] feedback)
          (assoc-in [:suggestions :meetings meeting-id] all)
          (assoc-in [:suggestions :agendas :updates] (group-by group-by-agenda-id update))
          (assoc-in [:suggestions :agendas :delete] (group-by group-by-agenda-id delete))
          (assoc-in [:suggestions :agendas :new] new)))))

(rf/reg-event-fx
  :suggestions/get-updates
  (fn [_ [_ share-hash edit-hash]]
    {:fx [[:http-xhrio {:method :get
                        :uri (gstring/format "%s/meeting/suggestions/%s/%s"
                                             (:rest-backend config) share-hash edit-hash)
                        :format (ajax/transit-request-format)
                        :response-format (ajax/transit-response-format)
                        :on-success [:suggestions/for-meeting-success]
                        :on-failure [:ajax-failure]}]]}))

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

