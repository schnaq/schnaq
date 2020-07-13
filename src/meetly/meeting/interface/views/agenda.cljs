(ns meetly.meeting.interface.views.agenda
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]))

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
   (for [agenda-num (range @(rf/subscribe [:agenda/number-of-forms]))]
     (add-form agenda-num))
   [add-agenda-button]
   [:br]
   [submit-agenda-button]])