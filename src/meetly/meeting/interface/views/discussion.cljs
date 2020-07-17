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
         [:div {:key (:statement/content conclusion)}
          [single-statement-view conclusion]])]]]))

;; #### Events ####

(rf/reg-event-fx
  :start-discussion
  (fn [{:keys [db]} [_ try-counter]]
    (let [discussion-id (-> db :agenda :chosen :discussion-id)
          username (get-in db [:user :name] "Anonymous")
          try-counter (or try-counter 0)]
      (when (< try-counter 10)
        (if discussion-id
          {:http-xhrio {:method :get
                        :uri (str (:rest-backend config) "/start-discussion/" discussion-id)
                        :format (ajax/json-request-format)
                        :url-params {:username username}
                        :response-format (ajax/json-response-format {:keywords? true})
                        :on-success [:set-current-discussion-steps]
                        :on-failure [:ajax-failure]}}
          {:dispatch-later [{:ms 50 :dispatch [:start-discussion (inc try-counter)]}]})))))

(rf/reg-event-db
  :set-current-discussion-steps
  (fn [db [_ response]]
    (let [options (:discussion-reactions response)]
      (assoc-in db [:discussion :options] options))))

;; #### Subs ####

(rf/reg-sub
  :discussion-options
  (fn [db _]
    (get-in db [:discussion :options])))

(rf/reg-sub
  :starting-conclusions
  (fn [_]
    (rf/subscribe [:discussion-options]))
  (fn [options]
    (when (= "starting-argument/select" (ffirst options))
      (->>
        options
        first second
        :present/arguments
        (filter #(not= "argument.type/undercut" (:argument/type %)))
        (map :argument/conclusion)
        distinct))))