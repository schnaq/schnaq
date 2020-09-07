(ns schnaq.interface.views.discussion.view-elements
  (:require [ajax.core :as ajax]
            [ghostwheel.core :refer [>defn-]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels fa img-path]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.discussion.logic :as logic]))

(defn up-down-vote
  "Add panel for up and down votes."
  [statement]
  (let [votes @(rf/subscribe [:local-votes])]
    [:<>
     [:div.vote.up-vote.text-center
      ;; Prevent activating the time travel or deep dive
      {:on-click (fn [e]
                   (js-wrap/stop-propagation e)
                   (rf/dispatch [:discussion/toggle-upvote statement]))}
      [:h6 [:i.pr-1 {:class (str "m-auto fas fa-lg " (fa :arrow-up))}]
       (logic/calculate-votes statement :upvotes votes)]]

     [:div.vote.down-vote.text-center
      {:on-click (fn [e]
                   (js-wrap/stop-propagation e)
                   (rf/dispatch [:discussion/toggle-downvote statement]))}
      [:h6 [:i.pr-1 {:class (str "m-auto fas fa-lg " (fa :arrow-down))}]
       (logic/calculate-votes statement :downvotes votes)]]]))


;; discussion header

(defn discussion-header [current-meeting]
  ;; meeting header
  [base/discussion-header
   (:meeting/title current-meeting)
   (:meeting/description current-meeting)
   (fn []
     (rf/dispatch [:navigation/navigate :routes.meeting/show
                   {:share-hash (:meeting/share-hash current-meeting)}])
     (rf/dispatch [:meeting/select-current current-meeting]))])

(defn discussion-header-no-subtitle [current-meeting]
  ;; meeting header
  [base/discussion-header
   (:meeting/title current-meeting)
   nil
   (fn []
     (rf/dispatch [:navigation/navigate :routes.meeting/show
                   {:share-hash (:meeting/share-hash current-meeting)}])
     (rf/dispatch [:meeting/select-current current-meeting]))])


;; discussion loop box

(defn agenda-header-back-arrow [on-click-back-function]
  (let [agenda @(rf/subscribe [:chosen-agenda])
        {:keys [meeting/share-hash]} @(rf/subscribe [:meeting/selected])]
    [:div.discussion-view-top-rounded
     [:div.row
      ;; back arrow
      [:div.col-1.back-arrow
       (when on-click-back-function
         [:p {:on-click on-click-back-function}
          [:i.arrow-icon {:class (str "m-auto fas " (fa :arrow-left))}]])]
      ;; title
      [:div.col-7.col-lg-10
       [:div
        [:h2.clickable-no-hover {:on-click #(rf/dispatch [:navigation/navigate :routes.discussion/start
                                                          {:share-hash share-hash
                                                           :id (:db/id (:agenda/discussion agenda))}])}
         (:agenda/title agenda)]
        [:p (:agenda/description agenda)]]]
      [:div.col-3.col-lg-1.graph-icon
       [:img.graph-icon-img.clickable-no-hover
        {:src (img-path :icon-graph) :alt "icon of graph"
         :on-click #(rf/dispatch
                      [:navigation/navigate :routes/graph-view
                       {:id (-> agenda :agenda/discussion :db/id)
                        :share-hash share-hash}])}]]]]))

(defn input-footer [allow-new? content]
  (when allow-new?
    [:div.discussion-view-bottom-rounded
     content]))

;; text input

(defn- input-starting-argument-form
  "A form, which allows the input of a complete argument.
  (Premise and Conclusion as statements)"
  []
  [:form
   {:on-submit (fn [e] (js-wrap/prevent-default e)
                 (rf/dispatch [:discussion/continue :starting-argument/new
                               (oget e [:target :elements])]))}
   [:textarea.form-control.discussion-text-input.mb-5
    {:name "conclusion-text" :wrap "soft" :rows 2
     :auto-complete "off"
     :required true
     :placeholder (labels :discussion/add-argument-conclusion-placeholder)}]
   [:textarea.form-control.discussion-text-input.mb-1
    {:name "premise-text" :wrap "soft" :rows 2
     :auto-complete "off"
     :required true
     :placeholder (labels :discussion/add-argument-premise-placeholder)}]
   [:div.text-center.button-spacing-top
    [:button.button-secondary {:type "submit"} (labels :discussion/create-argument-action)]]])

(defn input-field []
  (let [allow-new-argument? @(rf/subscribe [:allow-new-argument?])]
    [:div.discussion-view-bottom-rounded
     (when allow-new-argument?
       [:div
        [:div.mb-5 [:h5 (labels :discussion/create-argument-heading)]]
        [input-starting-argument-form]])]))


(defn input-form
  "Text input for adding a statement"
  []
  [:div.mt-4
   [:textarea.form-control.discussion-text-input.mb-1
    {:name "premise-text" :wrap "soft" :rows 2
     :auto-complete "off"
     :required true
     :placeholder (labels :discussion/premise-placeholder)}]
   ;; add button
   [:div.text-center.button-spacing-top
    [:button.button-secondary {:type "submit"} (labels :discussion/create-starting-premise-action)]]])

;; selection

(defn radio-button
  "Radio Button helper function. This function creates a radio button."
  [id name value label checked?]
  [:div.custom-control.custom-radio.my-2
   [:input.custom-control-input.custom-radio-button
    {:type "radio"
     :id id
     :name name
     :value value
     :default-checked checked?}]
   [:label.custom-control-label.custom-radio-button-label.clickable
    {:for id} (labels label)]])

(>defn- build-author-list
  "Build a nicely formatted string of a html list containing the authors from a sequence."
  [authors]
  [sequential? :ret string?]
  (str
    "<ul class=\"authors-list\">"
    (apply str (map #(str "<li>" (:author/nickname %) "</li>") authors))
    "</ul>"))

(defn- extra-discussion-info-badges
  "Badges that display additional discussion info."
  [statement]
  (let [popover-id (str "debater-popover-" (:db/id statement))]
    (reagent/create-class
      {:component-did-mount
       (fn [_]
         (js-wrap/popover (str "#" popover-id)))
       :component-will-unmount
       (fn [_]
         (js-wrap/popover (str "#" popover-id) "disable")
         (js-wrap/popover (str "#" popover-id) "dispose"))
       :reagent-render
       (fn []
         [:p.my-0
          [:span.badge.badge-pill.badge-transparent.mr-2
           [:i {:class (str "m-auto fas " (fa :comment))}] " "
           (-> statement :meta/sub-discussion-info :sub-statements)]
          [:span.badge.badge-pill.badge-transparent.badge-clickable
           {:id popover-id
            :data-toggle "popover"
            :data-trigger "focus"
            :tabIndex 20
            :on-click #(js-wrap/stop-propagation %)
            :title (labels :discussion.badges/user-overview)
            :data-html true
            :data-content (build-author-list (get-in statement [:meta/sub-discussion-info :authors]))}
           [:i {:class (str "m-auto fas " (fa :users))}] " "
           (-> statement :meta/sub-discussion-info :authors count)]])})))

;; bubble
(defn statement-bubble
  "A single bubble of a statement to be used ubiquitously."
  ([statement]
   [statement-bubble statement (logic/arg-type->attitude (:meta/argument.type statement))])
  ([{:keys [statement/content] :as statement} attitude]
   [:div.statement-outer
    [:div.row
     ;; bubble content
     [:div.col-12.col-md-11.px-0
      [:div.statement {:class (str "statement-" (name attitude))}
       (when (= :argument.type/undercut (:meta/argument.type statement))
         [:p.small (labels :discussion/undercut-bubble-intro)])
       ;; content
       [:div.statement-content
        [:p.my-0 content]]
       ;; additional Info
       [:div.row.px-3
        [:div.col-5.align-self-end
         [extra-discussion-info-badges statement]]
        [:div.col-7
         ;; avatar
         [:small.text-right.float-right
          (common/avatar (-> statement :statement/author :author/nickname) 50)]]]]]
     ;; up-down-votes
     [:div.col-12.col-md-1.px-0
      [:div.up-down-vote
       [up-down-vote statement]]]]]))

(defn conclusions-list []
  (let [path-params (:path-params @(rf/subscribe [:navigation/current-route]))
        conclusions @(rf/subscribe [:starting-conclusions])]
    [:div
     [:div#conclusions-list.mobile-container
      (for [conclusion conclusions]
        [:div {:key (:db/id conclusion)
               :on-click (fn [_e]
                           (rf/dispatch [:discussion/continue :starting-conclusions/select conclusion])
                           (rf/dispatch [:navigation/navigate :routes.discussion/continue
                                         {:id (:id path-params)
                                          :share-hash (:share-hash path-params)}]))}
         [statement-bubble conclusion :neutral]])]]))

(defn history-view
  "Displays the statements it took to get to where the user is."
  []
  (let [history @(rf/subscribe [:discussion-history])
        indexed-history (map-indexed #(vector (- (count history) %1 1) %2) history)]
    [:div#discussion-history.mobile-container
     (for [[count [statement attitude]] indexed-history]
       [:div {:key (:db/id statement)
              :on-click #(rf/dispatch [:discussion.history/time-travel count])}
        [statement-bubble statement attitude]])]))


(rf/reg-event-fx
  :discussion/toggle-upvote
  (fn [{:keys [db]} [_ {:keys [db/id] :as statement}]]
    {:fx [[:http-xhrio {:method :post
                        :uri (str (:rest-backend config) "/votes/up/toggle")
                        :format (ajax/transit-request-format)
                        :params {:statement-id id
                                 :nickname (get-in db [:user :name] "Anonymous")
                                 :meeting-hash (-> db :meeting :selected :meeting/share-hash)}
                        :response-format (ajax/transit-response-format)
                        :on-success [:upvote-success statement]
                        :on-failure [:ajax-failure]}]]}))

(rf/reg-event-fx
  :discussion/toggle-downvote
  (fn [{:keys [db]} [_ {:keys [db/id] :as statement}]]
    {:fx [[:http-xhrio {:method :post
                        :uri (str (:rest-backend config) "/votes/down/toggle")
                        :format (ajax/transit-request-format)
                        :params {:statement-id id
                                 :nickname (get-in db [:user :name] "Anonymous")
                                 :meeting-hash (-> db :meeting :selected :meeting/share-hash)}
                        :response-format (ajax/transit-response-format)
                        :on-success [:downvote-success statement]
                        :on-failure [:ajax-failure]}]]}))

(rf/reg-event-db
  :upvote-success
  (fn [db [_ {:keys [db/id]} {:keys [operation]}]]
    (case operation
      :added (update-in db [:votes :up id] inc)
      :removed (update-in db [:votes :up id] dec)
      :switched (update-in
                  (update-in db [:votes :up id] inc)
                  [:votes :down id] dec))))


(rf/reg-event-db
  :downvote-success
  (fn [db [_ {:keys [db/id]} {:keys [operation]}]]
    (case operation
      :added (update-in db [:votes :down id] inc)
      :removed (update-in db [:votes :down id] dec)
      :switched (update-in
                  (update-in db [:votes :down id] inc)
                  [:votes :up id] dec))))
