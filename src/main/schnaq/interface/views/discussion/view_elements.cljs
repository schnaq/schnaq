(ns schnaq.interface.views.discussion.view-elements
  (:require [ajax.core :as ajax]
            [ghostwheel.core :refer [>defn-]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels fa img-path]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.markdown-parser :as markdown-parser]
            [schnaq.interface.views.brainstorm.tools :as btools]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.discussion.logic :as logic]))

(defn- up-down-vote
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

(defn agenda-header-with-back-arrow [meeting on-click-back-function]
  (let [agenda @(rf/subscribe [:chosen-agenda])
        {:keys [meeting/share-hash]} meeting
        current-view (-> @(rf/subscribe [:navigation/current-route]) :data :name)]
    (common/set-website-title! (:agenda/title agenda))
    [:div.discussion-primary-background
     [:div.row
      ;; back arrow
      [:div.col-1.back-arrow.text-center
       (when-not (and (btools/is-brainstorm? meeting)
                      (= :routes.discussion/start current-view))
         (when on-click-back-function
           [:p.m-auto {:on-click on-click-back-function}
            [:i.arrow-icon {:class (str "m-auto fas " (fa :arrow-left))}]]))]
      ;; title
      [:div.col-8.col-lg-10.d-flex
       [:h2.clickable-no-hover.align-self-center
        {:on-click #(rf/dispatch [:navigation/navigate :routes.discussion/start
                                  {:share-hash share-hash
                                   :id (:db/id (:agenda/discussion agenda))}])}
        (:agenda/title agenda)]
       [markdown-parser/markdown-to-html (:agenda/description agenda)]]
      [:div.col-3.col-lg-1.graph-icon
       [:img.graph-icon-img.clickable-no-hover
        {:src (img-path :icon-graph) :alt (labels :graph.button/text)
         :title (labels :graph.button/text)
         :on-click #(rf/dispatch
                      [:navigation/navigate :routes/graph-view
                       {:id (-> agenda :agenda/discussion :db/id)
                        :share-hash share-hash}])}]]]]))

(defn input-footer
  ([content]
   [input-footer true content])
  ([allow-new? content]
   (when allow-new?
     [:div.discussion-primary-background
      content])))

(defn- input-starting-statement-form
  "A form, which allows the input of a starting-statement."
  []
  [:form
   {:on-submit (fn [e] (js-wrap/prevent-default e)
                 (rf/dispatch [:discussion.add.statement/starting
                               (oget e [:target :elements])]))}
   [:textarea.form-control.discussion-text-input
    {:name "statement-text" :wrap "soft" :rows 2
     :auto-complete "off"
     :required true
     :placeholder (labels :discussion/add-argument-conclusion-placeholder)}]
   [:div.text-center.button-spacing-top
    [:button.button-secondary {:type "submit"} (labels :discussion/create-argument-action)]]])

(rf/reg-event-fx
  :discussion.add.statement/starting
  (fn [{:keys [db]} [_ form]]
    (let [{:keys [id share-hash]} (get-in db [:current-route :parameters :path])
          nickname (get-in db [:user :name] "Anonymous")
          statement-text (oget form [:statement-text :value])]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/discussion/statements/starting/add")
                          :format (ajax/transit-request-format)
                          :params {:statement statement-text
                                   :nickname nickname
                                   :share-hash share-hash
                                   :discussion-id id}
                          :response-format (ajax/transit-response-format)
                          :on-success [:discussion.add.statement/starting-success form]
                          :on-failure [:ajax-failure]}]]})))

(rf/reg-event-fx
  :discussion.add.statement/starting-success
  (fn [_ [_ form new-starting-statements]]
    {:fx [[:dispatch [:notification/add
                      #:notification{:title (labels :discussion.notification/new-content-title)
                                     :body (labels :discussion.notification/new-content-body)
                                     :context :success}]]
          [:dispatch [:discussion.query.conclusions/set-starting new-starting-statements]]
          [:form/clear form]]}))

(defn input-field []
  [:div.discussion-primary-background
   [:div.mb-2 [:h5 (labels :discussion/create-argument-heading)]]
   [input-starting-statement-form]])

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
    [:p.mt-3
     [:span.badge.badge-pill.badge-transparent.mr-2
      [:i {:class (str "m-auto fas " (fa :comment))}] " "
      (-> statement :meta/sub-discussion-info :sub-statements)]
     [:span.badge.badge-pill.badge-transparent.badge-clickable
      {:id popover-id
       :data-toggle "popover"
       :data-trigger "focus"
       :tabIndex 20
       :on-click (fn [e] (js-wrap/stop-propagation e)
                   (js-wrap/popover (str "#" popover-id) "show"))
       :title (labels :discussion.badges/user-overview)
       :data-html true
       :data-content (build-author-list (get-in statement [:meta/sub-discussion-info :authors]))}
      [:i {:class (str "m-auto fas " (fa :user/group))}] " "
      (-> statement :meta/sub-discussion-info :authors count)]]))

(defn statement-bubble
  "A single bubble of a statement to be used ubiquitously."
  ([statement]
   [statement-bubble statement (logic/arg-type->attitude (:meta/argument-type statement))])
  ([{:keys [statement/content] :as statement} attitude]
   [:div.statement-outer
    [:div.row
     ;; bubble content
     [:div.col-12.col-md-11
      [:div.row.statement {:class (str "statement-" (name attitude))}
       (when (= :argument.type/undercut (:meta/argument-type statement))
         [:div.col-12
          [:p.small (labels :discussion/undercut-bubble-intro)]])
       ;; content
       [:div.col-10.statement-content
        [:p.my-0 content]
        [extra-discussion-info-badges statement]]
       [:div.col-2
        ;; avatar
        [:span
         [common/avatar (-> statement :statement/author :author/nickname) 50]]]]]
     ;; up-down-votes
     [:div.col-12.col-md-1.px-0
      [:div.up-down-vote
       [up-down-vote statement]]]]]))

(rf/reg-event-fx
  :discussion.query.statement/by-id
  (fn [{:keys [db]} _]
    (let [{:keys [id share-hash statement-id]} (get-in db [:current-route :parameters :path])]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/discussion/statement/info")
                          :format (ajax/transit-request-format)
                          :params {:statement-id statement-id
                                   :share-hash share-hash
                                   :discussion-id id}
                          :response-format (ajax/transit-response-format)
                          :on-success [:discussion.query.statement/by-id-success]
                          :on-failure [:ajax-failure]}]]})))

(rf/reg-event-fx
  :discussion.query.statement/by-id-success
  (fn [{:keys [db]} [_ {:keys [conclusion premises undercuts]}]]
    {:db (->
           (assoc-in db [:discussion :conclusions :selected] conclusion)
           (assoc-in [:discussion :premises :current] (concat premises undercuts)))
     :fx [[:dispatch [:discussion.history/push conclusion]]]}))

(rf/reg-event-fx
  :discussion.statement/select
  (fn [{:keys [db]} [_ statement]]
    (let [{:keys [id share-hash]} (get-in db [:current-route :parameters :path])]
      {:fx [[:dispatch [:discussion.select/conclusion statement]]
            [:dispatch [:navigation/navigate :routes.discussion.select/statement
                        {:id id :share-hash share-hash :statement-id (:db/id statement)}]]]})))

(rf/reg-event-fx
  :discussion.select/conclusion
  (fn [{:keys [db]} [_ conclusion]]
    (let [{:keys [id share-hash]} (get-in db [:current-route :parameters :path])]
      {:db (assoc-in db [:discussion :conclusions :selected] conclusion)
       :fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/discussion/statements/for-conclusion")
                          :format (ajax/transit-request-format)
                          :params {:selected-statement conclusion
                                   :share-hash share-hash
                                   :discussion-id id}
                          :response-format (ajax/transit-response-format)
                          :on-success [:discussion.premises/set-current]
                          :on-failure [:ajax-failure]}]]})))

(rf/reg-event-db
  :discussion.premises/set-current
  (fn [db [_ {:keys [premises undercuts]}]]
    (assoc-in db [:discussion :premises :current] (concat premises undercuts))))

(rf/reg-sub
  :discussion.premises/current
  (fn [db _]
    (get-in db [:discussion :premises :current] [])))

(rf/reg-sub
  :discussion.conclusions/selected
  (fn [db _]
    (get-in db [:discussion :conclusions :selected])))

(defn conclusions-list
  "Displays a list of conclusions."
  [conclusions]
  (let [path-params (:path-params @(rf/subscribe [:navigation/current-route]))]
    [:div.conclusions-list.mobile-container
     (for [conclusion conclusions]
       [:div {:key (:db/id conclusion)
              :on-click (fn [_e]
                          (rf/dispatch [:discussion.select/conclusion conclusion])
                          (rf/dispatch [:discussion.history/push conclusion])
                          (rf/dispatch [:navigation/navigate :routes.discussion.select/statement
                                        (assoc path-params :statement-id (:db/id conclusion))]))}
        [statement-bubble conclusion :neutral]])]))

(defn history-view
  "Displays the statements it took to get to where the user is."
  []
  (let [history @(rf/subscribe [:discussion-history])
        indexed-history (map-indexed #(vector (- (count history) %1 1) %2) history)]
    [:div.discussion-history.mobile-container
     (for [[index statement] indexed-history]
       [:div {:key (str "history-" (:db/id statement))
              :on-click #(rf/dispatch [:discussion.history/time-travel index])}
        [statement-bubble statement]])]))

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
