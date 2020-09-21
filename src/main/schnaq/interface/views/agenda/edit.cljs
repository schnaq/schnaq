(ns schnaq.interface.views.agenda.edit
  (:require [ajax.core :as ajax]
            [ghostwheel.core :refer [>defn-]]
            [goog.string :as gstring]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.agenda.agenda :as agenda]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.views.modals.modal :as modal]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.views.text-editor.view :as editor]))

(defn- suggestions-table
  "Show all suggestions."
  [suggestions suggestion-type]
  [:table.table
   [:thead
    [:tr
     [:th {:width "10%"} (labels :suggestions.modal.table/nickname)]
     [:th {:width "25%"} (labels :suggestions.modal.table/suggestion-title)]
     [:th {:width "65%"} (labels :suggestions.modal.table/suggestion-description)]]]
   [:tbody
    (for [suggestion suggestions]
      (let [get-value #(suggestion (keyword (str (name suggestion-type) "/" %)))]
        [:tr {:key (:db/id suggestion)}
         [:td (get-value "ideator")]
         [:td (get-value "title")]
         [:td (get-value "description")]]))]])

(defn- suggestions-modal
  "Open a modal containing the suggested changes."
  [suggestions suggestion-type modal-title]
  [modal/modal-template
   modal-title
   [:<>
    [:p (labels :suggestions.modal/primer)]
    [suggestions-table suggestions suggestion-type]]])

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
    [:p (labels :suggestions.modal/primer)]
    [:ul
     (for [suggestion suggestions]
       [:li {:key (:db/id suggestion)}
        (:agenda.suggestion/ideator suggestion)])]]])

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
                                               (labels :suggestions.modal.new/title)]}])}
     [:i {:class (str "m-auto fas " (fa :add))}] " "
     (count suggestions)]))

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

(defn- agenda-edit-title
  "The editable title input of an edit-agenda-form."
  [agenda]
  (let [db-id (:db/id agenda)]
    [:input.form-control.form-border-bottom-light.form-title-light
     {:type "text"
      :name "title"
      :auto-complete "off"
      :required true
      :placeholder (labels :agenda/point)
      :value (:agenda/title agenda)
      :id (str "title-" db-id)
      :on-change
      #(rf/dispatch [:agenda/update-edit-form :agenda/title db-id (oget % [:target :value])])}]))

(defn- agenda-edit-description
  "The editable description input of an edit-agenda-form."
  [agenda]
  (let [db-id (:db/id agenda)]
    [editor/view
     #(rf/dispatch [:agenda/update-edit-form :agenda/description db-id %])
     (:agenda/description agenda)]))

(defn- agenda-view [agenda]
  [:<>
   [:div.agenda-line]
   [:div.agenda-point.shadow-straight.pb-3
       ;; title
    [:div.background-secondary.p-3
     [:div.row
      [:div.col-10.col-md-10
       [agenda-edit-title agenda]]
      [:div.col-2.col-md-2
       [:div.pt-4.clickable
        {:on-click #(rf/dispatch [:agenda/delete (:db/id agenda)])}
        [:i {:class (str "m-auto fas fa-2x " (fa :delete-icon))}]]]]]
    ;; description
    [:div.text-left
     [agenda-edit-description agenda]]
    [badge-wrapper
     (with-meta
       [update-suggestions-badge agenda :suggestions/agenda-updates :agenda.suggestion]
       {:key (str "suggestion-badge-" (:db/id agenda))})
     (with-meta
       [deletion-badge agenda]
       {:key (str "deletion-badge-" (:db/id agenda))})]]])

(defn- editable-meeting-info [selected-meeting]
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
    #(rf/dispatch
       [:meeting/update-meeting-attribute :meeting/description %])
    (:meeting/description selected-meeting)]
   [badge-wrapper
    (with-meta
      [update-suggestions-badge selected-meeting :suggestions/meeting :meeting.suggestion]
      {:key (str "suggestion-badge-" (:db/id selected-meeting))})
    (with-meta
      [addition-badge]
      {:key (str "addition-badge-" (:db/id selected-meeting))})]])

(>defn- editable-meeting-template
  "Can be used to present an editable meeting in different views. Customize the heading
  and on-submit-function to your liking."
  [heading on-submit-fn]
  [:re-frame/component fn? :ret :re-frame/component]
  (let [edit-information @(rf/subscribe [:agenda/current-edit-info])
        selected-meeting (:meeting edit-information)
        meeting-agendas (:agendas edit-information)]
    [:<>
     [base/nav-header]
     heading
     [:div.container.text-center.pb-5
      [:form {:on-submit on-submit-fn}
       ;; meeting title and description
       [editable-meeting-info selected-meeting]
       [:div.container
        (for [agenda meeting-agendas]
          [:div {:key (:db/id agenda)}
           [agenda-view agenda]])
        [:div.agenda-line]
        [agenda/add-agenda-button (count meeting-agendas) :agenda/add-edit-form]
        [submit-edit-button]]]]]))

(defn- edit-view []
  [editable-meeting-template
   [edit-header]
   (fn [e]
     (js-wrap/prevent-default e)
     (rf/dispatch [:meeting/submit-changes]))])

(defn agenda-edit-view []
  [edit-view])

(defn- suggestion-view []
  [editable-meeting-template
   [base/header
    (labels :meetings.suggestions/header)
    (labels :meetings.suggestions/subheader)]
   (fn [e]
     (js-wrap/prevent-default e)
     (rf/dispatch [:suggestions/submit]))])

(defn agenda-suggestion-view []
  [suggestion-view])

;; load agendas events

(rf/reg-event-fx
  :agenda/load-for-edit
  (fn [_ [_ share-hash]]
    (agenda/load-agenda-fn share-hash :agenda/load-for-edit-success)))

(rf/reg-event-db
  :agenda/load-for-edit-success
  (fn [db [_ agendas]]
    (assoc db :edit-meeting {:agendas agendas
                             :meeting (get-in db [:meeting :selected])
                             :delete-agendas #{}})))

(rf/reg-sub
  :agenda/current-edit-info
  (fn [db _]
    (:edit-meeting db)))

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
    (update-in db [:edit-meeting :agendas]
               #(concat % [{:db/id (str (random-uuid))
                            :agenda/title ""
                            :agenda/description ""
                            :agenda/meeting (get-in db [:edit-meeting :meeting :db/id])}]))))

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
  (fn [_ [_ _]]
    {:fx [[:dispatch [:notification/add
                      #:notification{:title "Vorschläge eingereicht"
                                     :body "Ihre Vorschläge wurden erfolgreich verschickt!"
                                     :context :success}]]]}))

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
                     agenda.suggestion.type/delete meeting.suggestions/all]}]]
    (let [group-by-agenda-id #(get-in % [:agenda.suggestion/agenda :db/id])]
      (-> db
          (assoc-in [:suggestions :meetings (:db/id (:meeting.suggestion/meeting (first all)))] all)
          (assoc-in [:suggestions :agendas :updates] (group-by group-by-agenda-id update))
          (assoc-in [:suggestions :agendas :delete] (group-by group-by-agenda-id delete))
          (assoc-in [:suggestions :agendas :new] new)))))

(rf/reg-event-fx
  :suggestions/send-updates
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

