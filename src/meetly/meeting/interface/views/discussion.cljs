(ns meetly.meeting.interface.views.discussion
  (:require [re-frame.core :as rf]
            [meetly.meeting.interface.config :refer [config]]
            [ajax.core :as ajax]))


;; #### Views ####

(defn- single-statement-view
  "Displays a single statement inside a discussion."
  [statement]
  [:div.card {:style {:background-color "#6aadb8"
                      :width "600px"}}
   [:p (:statement/content statement)]
   [:small "Written by: " (-> statement :statement/author :author/nickname)]])

(defn all-positions-view
  "Shows a nice header and all positions."
  []
  (let [agenda @(rf/subscribe [:chosen-agenda])
        conclusions @(rf/subscribe [:starting-conclusions])]
    [:div
     [:div.row.discussion-head
      [:div.col-12
       [:h2 (:title agenda)]
       [:p (:description agenda)]
       [:hr]
       (for [conclusion conclusions]
         [:div {:key (random-uuid)}
          [single-statement-view conclusion]])]]]))

;; #### Events ####

(rf/reg-event-fx
  :load-starting-conclusions
  (fn [_ [_ discussion-id]]
    {:http-xhrio {:method :get
                  :uri (str (:rest-backend config) "/agenda/starting-conclusions/" discussion-id)
                  :format (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:display-starting-conclusions]
                  :on-failure [:ajax-failure]}}))

(rf/reg-event-db
  :display-starting-conclusions
  (fn [db [_ response]]
    (assoc-in db [:discussion :starting-conclusion] (:conclusions response))))

;; #### Subs ####

(rf/reg-sub
  :starting-conclusions
  (fn [db _]
    (get-in db [:discussion :starting-conclusion] [])))