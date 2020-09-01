(ns schnaq.interface.views.agenda.agenda
  (:require [ajax.core :as ajax]
            [goog.string :as gstring]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :as data]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.base :as base]
            [re-frame.core :as rf]
            [oops.core :refer [oget]]))

(defn new-agenda-local
  "This function formats the agenda-form input and saves it locally to the db until
  the discussion is created fully. `field` can be `title` or `description`."
  [field content suffix]
  (case field
    :title (rf/dispatch [:agenda/update-title content suffix])
    :description (rf/dispatch [:agenda/update-description content suffix])))

(defn new-agenda-form
  "A form for creating a new agenda. The new agenda is automatically saved in the
  app-state according to the suffix."
  [numbered-suffix]
  [:div
   [:div.agenda-line]
   [:div.add-agenda-div.agenda-point
    ;; title
    [:div.row.agenda-row-title
     [:div.col-10
      [:input.form-control.agenda-form-title.form-title
       {:type "text"
        :name "title"
        :auto-complete "off"
        :required true
        :placeholder (str (data/labels :agenda/point) (inc numbered-suffix))
        :id (str "title-" numbered-suffix)
        :on-key-up
        #(new-agenda-local :title (oget % [:target :value]) numbered-suffix)}]]
     [:div.col-2
      [:div.pt-4.clickable
       {:on-click #(rf/dispatch [:agenda/delete-temporary numbered-suffix])}
       [:i {:class (str "m-auto fas fa-2x " (data/fa :delete-icon))}]]]]
    ;; description
    [:textarea.form-control.agenda-form-round
     {:name "description"
      :placeholder (str (data/labels :agenda/desc-for) (inc numbered-suffix))
      :id (str "description-" numbered-suffix)
      :on-key-up #(new-agenda-local
                    :description (oget % [:target :value]) numbered-suffix)}]]])

(defn- add-agenda-button [number-of-forms]
  [:input.btn.agenda-add-button.mb-5 {:type "button"
                                      :value (if (or (nil? number-of-forms) (zero? number-of-forms))
                                               (data/labels :agenda.create/optional-agenda)
                                               "+")
                                      :on-click #(rf/dispatch [:increase-agenda-forms])}])

(defn- submit-agenda-button []
  [:input.btn.button-primary {:type "submit"
                              :value (data/labels :meeting-create-header)}])

;; #### header ####

(defn- header []
  (base/header
    (data/labels :agenda/header)
    (data/labels :agenda/subheader)))


(defn agenda-view
  "Shows the view for adding one or more agendas."
  []
  (let [number-of-forms @(rf/subscribe [:agenda/number-of-forms])]
    [:div#create-agenda
     [base/nav-header]
     [header]
     [:div.container.px-5.py-3.text-center
      [:div.agenda-meeting-container.p-3
       [:h2.mb-4 (:meeting/title @(rf/subscribe [:meeting/selected]))]
       [:h4 (:meeting/description @(rf/subscribe [:meeting/selected]))]]
      [:div.container
       [:div.agenda-container
        [:form {:id "agendas-add-form"
                :on-submit (fn [e]
                             (js-wrap/prevent-default e)
                             (rf/dispatch [:send-agendas]))}
         (for [agenda-num (range number-of-forms)]
           [:div {:key agenda-num}
            [new-agenda-form agenda-num]])
         [:div.agenda-line]
         [add-agenda-button number-of-forms]
         [submit-agenda-button]]]]]]))

;; #### Events ####

(rf/reg-event-fx
  :send-agendas
  (fn [{:keys [db]} _]
    ;; Use [:meeting :last-added] instead of selected meeting because it contains secret information
    (let [agendas (get-in db [:agenda :all] [])
          {:keys [db/id meeting/share-hash meeting/edit-hash meeting/title meeting/description]}
          (get-in db [:meeting :last-added])
          stub-agenda {0 {:title title
                          :description description}}
          agendas-to-send (if (zero? (count agendas)) stub-agenda agendas)]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/agendas/add")
                          :params {:agendas (vals agendas-to-send)
                                   :meeting-id id
                                   :meeting-hash share-hash}
                          :format (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success [:on-successful-agenda-add share-hash edit-hash]
                          :on-failure [:ajax-failure]}]]})))

(rf/reg-event-fx
  :on-successful-agenda-add
  (fn [_ [_ meeting-hash edit-hash]]
    (let [meeting-hashs {:share-hash meeting-hash
                         :admin-hash edit-hash}]
      {:fx [[:dispatch [:clear-current-agendas]]
            [:dispatch [:reset-temporary-agendas]]
            [:dispatch [:navigation/navigate :routes.meeting/created meeting-hashs]]]})))

(defn load-agenda-fn [hash on-success-event]
  {:http-xhrio {:method :get
                :uri (str (:rest-backend config) "/agendas/by-meeting-hash/" hash)
                :format (ajax/transit-request-format)
                :response-format (ajax/transit-response-format)
                :on-success [on-success-event]
                :on-failure [:ajax-failure]}})

(rf/reg-event-fx
  :agenda/load-and-redirect
  (fn [_ [_ hash]]
    (load-agenda-fn hash :agenda/set-current-maybe-enter)))

(rf/reg-event-fx
  :load-agenda-information
  (fn [{:keys [db]} [_ share-hash discussion-id]]
    (when-not (-> db :agenda :chosen)
      {:http-xhrio {:method :get
                    :uri (gstring/format "%s/agenda/%s/%s" (:rest-backend config) share-hash discussion-id)
                    :format (ajax/transit-request-format)
                    :response-format (ajax/transit-response-format)
                    :on-success [:set-response-as-agenda]
                    :on-failure [:agenda-not-available]}})))

(rf/reg-event-fx
  :agenda-not-available
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:error :ajax] "Agenda could not be loaded, please refresh the App.")
     :dispatch [:navigation/navigate :routes/meetings]}))

(rf/reg-event-fx
  :agenda/set-current-maybe-enter
  (fn [{:keys [db]} [_ agendas]]
    (let [selected-meeting (get-in db [:meeting :selected])]
      {:db (assoc-in db [:agendas :current] agendas)
       :fx [(when (<= (count agendas) 1)
              [:dispatch [:navigation/navigate :routes.discussion/start
                          {:share-hash (:meeting/share-hash selected-meeting)
                           :id (-> agendas first :agenda/discussion :db/id)}]])]})))

(rf/reg-event-db
  :clear-current-agendas
  (fn [db _]
    (assoc-in db [:agendas :current] [])))

(rf/reg-event-db
  :increase-agenda-forms
  (fn [db _]
    (update-in db [:agenda :number-of-forms] inc)))

(rf/reg-event-db
  :agenda/update-title
  (fn [db [_ content suffix]]
    (assoc-in db [:agenda :all suffix :title] content)))

(rf/reg-event-db
  :agenda/update-description
  (fn [db [_ content suffix]]
    (assoc-in db [:agenda :all suffix :description] content)))

(rf/reg-event-db
  :reset-temporary-agendas
  (fn [db _]
    (assoc db :agenda {})))

(rf/reg-event-db
  :agenda/delete-temporary
  (fn [db [_ suffix]]
    (-> db
        (update-in [:agenda :number-of-forms] dec)
        (update-in [:agenda :all] dissoc suffix))))

(rf/reg-event-db
  :choose-agenda
  (fn [db [_ agenda]]
    (assoc-in db [:agenda :chosen] agenda)))

(rf/reg-event-db
  :set-response-as-agenda
  (fn [db [_ response]]
    (assoc-in db [:agenda :chosen] response)))

(rf/reg-event-fx
  :agenda/redirect-on-reload
  (fn [{:keys [db]} _]
    (when-not (get-in db [:meeting :last-added])
      {:dispatch [:navigation/navigate :routes.meeting/create]})))

;; #### Subs ####

(rf/reg-sub
  :agenda/number-of-forms
  (fn [db _]
    (-> db :agenda :number-of-forms)))

(rf/reg-sub
  :current-agendas
  (fn [db _]
    (get-in db [:agendas :current])))

(rf/reg-sub
  :chosen-agenda
  (fn [db _]
    (get-in db [:agenda :chosen])))