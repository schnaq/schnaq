(ns meetly.meeting.interface.views.agenda
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [ajax.core :as ajax]))

(defn new-agenda-local
  "This function formats the agenda-form input and saves it locally to the db until
  the discussion is created fully. `field` can be `title` or `description`."
  [field content suffix]
  (case field
    :title (rf/dispatch [:agenda/update-title content suffix])
    :description (rf/dispatch [:agenda/update-description content suffix])))

(defn add-form [numbered-suffix]
  [:div.add-agenda-div {:key numbered-suffix}
   [:form {:id (str "agenda-" numbered-suffix)}
    [:label {:for "title"} "Agenda-point: "]
    [:input {:type "text" :name "title" :placeholder (str "TOP " numbered-suffix)
             :id (str "title-" numbered-suffix)
             :on-key-up
             #(new-agenda-local :title (oget % [:target :value]) numbered-suffix)}]
    [:br]
    [:label {:for "description"} "Additional Information: "]
    [:textarea {:name "description" :placeholder "Important to know!"
                :id (str "description-" numbered-suffix)
                :on-key-up
                #(new-agenda-local :description (oget % [:target :value]) numbered-suffix)}]
    [:br]]
   [:br]])

(defn add-agenda-button []
  [:input {:type "button" :value "+ More Agenda +"
           :on-click #(rf/dispatch [:increase-agenda-forms])}])

(defn submit-agenda-button []
  [:input {:type "button" :value "Start Meetly"
           :on-click #(rf/dispatch [:send-agendas])}])

(defn agenda-view []
  [:div
   [:h1 "Add Agenda!"]
   [:h2 "For Meeting: " (:title @(rf/subscribe [:meeting/last-added]))]
   (for [agenda-num (range @(rf/subscribe [:agenda/number-of-forms]))]
     (add-form agenda-num))
   [add-agenda-button]
   [:br]
   [submit-agenda-button]])

;; #### Events ####

(rf/reg-event-fx
  :send-agendas
  (fn [{:keys [db]} _]
    {:http-xhrio {:method :post
                  :uri "http://localhost:3000/agendas/add"
                  :params {:agendas (get-in db [:agenda :all] [])
                           :meeting-id (-> db :meeting/added :id)}
                  :format (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:reset-temporary-agenda]
                  :on-failure [:ajax-failure]}}))

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
  :reset-temporary-agenda
  (fn [db _]
    (assoc db :agenda {:number-of-forms 1 :all {}})))

;; #### Subs ####

(rf/reg-sub
  :agenda/number-of-forms
  (fn [db _]
    (-> db :agenda :number-of-forms)))