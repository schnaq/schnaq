(ns meetly.interface.views.discussion.view-elements
  (:require ["jquery" :as jquery]
            [ajax.core :as ajax]
            [ghostwheel.core :refer [>defn-]]
            [meetly.interface.config :refer [config]]
            [meetly.interface.text.display-data :refer [labels fa]]
            [meetly.interface.utils.js-wrapper :as js-wrap]
            [meetly.interface.views.base :as base]
            [meetly.interface.views.common :as common]
            [meetly.interface.views.discussion.logic :as logic]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]))

(defn up-down-vote
  "Add panel for up and down votes."
  [statement]
  (let [votes @(rf/subscribe [:local-votes])]
    [:div
     [:div.up-vote.text-center
      ;; Prevent activating the time travel or deep dive
      {:on-click (fn [e]
                   (js-wrap/stop-propagation e)
                   (rf/dispatch [:toggle-upvote statement]))}
      [:h6 [:i.pr-1 {:class (str "m-auto fas fa-lg " (fa :arrow-up))}]
       (logic/calculate-votes statement :upvotes votes)]]

     [:div.down-vote.text-center
      {:on-click (fn [e]
                   (js-wrap/stop-propagation e)
                   (rf/dispatch [:toggle-downvote statement]))}
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
      [:div.col-11
       [:div
        [:h2.link-pointer {:on-click #(rf/dispatch [:navigation/navigate :routes.discussion/start
                                                    {:share-hash share-hash
                                                     :id (:db/id (:agenda/discussion agenda))}])}
         (:agenda/title agenda)]
        [:p (:agenda/description agenda)]]]]]))

(defn input-footer [content]
  [:div.discussion-view-bottom-rounded
   content])

;; text input

(defn- input-starting-argument-form
  "A form, which allows the input of a complete argument.
  (Premise and Conclusion as statements)"
  []
  [:form
   {:on-submit (fn [e] (js-wrap/prevent-default e)
                 (rf/dispatch [:continue-discussion :starting-argument/new
                               (oget e [:target :elements])]))}
   [:input.form-control.discussion-text-input.mb-5
    {:type "text" :name "conclusion-text"
     :auto-complete "off"
     :placeholder (labels :discussion/add-argument-conclusion-placeholder)}]
   [:input.form-control.discussion-text-input.mb-1
    {:type "text" :name "premise-text"
     :auto-complete "off"
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
   [:input.form-control.discussion-text-input.mb-1
    {:type "text" :name "premise-text"
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
  (reagent/create-class
    {:component-did-mount
     (fn [_]
       (.popover (jquery "[data-toggle=\"popover\"]")))
     :reagent-render
     (fn []
       [:p
        [:span.badge.badge-pill.badge-transparent.mr-2
         [:i {:class (str "m-auto fas " (fa :comment))}] " "
         (-> statement :meta/sub-discussion-info :sub-statements)]
        [:span.badge.badge-pill.badge-transparent.badge-clickable
         {:data-toggle "popover"
          :data-trigger "focus"
          :tabIndex 20
          :on-click #(js-wrap/stop-propagation %)
          :title (labels :discussion.badges/user-overview)
          :data-html true
          :data-content (build-author-list (get-in statement [:meta/sub-discussion-info :authors]))}
         [:i {:class (str "m-auto fas " (fa :users))}] " "
         (-> statement :meta/sub-discussion-info :authors count)]])}))

;; bubble

(defn- statement-bubble
  "A single bubble of a statement to be used ubiquitously."
  ([statement]
   (statement-bubble statement (logic/arg-type->attitude (:meta/argument.type statement))))
  ([{:keys [statement/content] :as statement} attitude]
   [:div.statement-outer
    [:div.row
     ;; bubble content
     [:div.col-11.px-0
      [:div.statement {:class (str "statement-" (name attitude))}
       (when (= :argument.type/undercut (:meta/argument.type statement))
         [:p.small (labels :discussion/undercut-bubble-intro)])
       ;; information
       [:div
        ;; avatar
        [:small.text-right.float-right (common/avatar (-> statement :statement/author :author/nickname) 50)]]
       ;; content
       [:div.statement-content
        [:p content]]
       [:div.row.px-3
        [:div.col
         [extra-discussion-info-badges statement]]]]]
     ;; up-down-votes
     [:div.col-1.px-0
      [:div.up-down-vote
       (up-down-vote statement)]]]]))

;; carousel

(defn premises-carousel [premises]
  [:div#carouselExampleIndicators.carousel.slide {:data-ride "carousel"}
   ;; indicator
   [:ol.carousel-indicators.carousel-indicator-custom
    ;; range of number of premises and set the first element as selected
    (map
      (fn [i]
        (let [params {:key (str "indicator-" (:db/id (nth premises i))) :data-target "#carouselExampleIndicators" :data-slide-to (str i)}]
          (if (zero? i)
            [:li.active params]
            [:li params])))
      (range (count premises)))]
   ;; content
   [:div.carousel-inner
    ;; set first indexed element as selected
    (map-indexed
      (fn [index premise]
        (let [params {:key (:db/id premise)}
              content [:div.premise-carousel-item
                       {:on-click #(rf/dispatch [:continue-discussion :premises/select premise])}
                       [statement-bubble premise]]]
          (if (zero? index)
            [:div.carousel-item.active params content]
            [:div.carousel-item params content])))
      premises)]
   ;; interface elements
   [:a.carousel-control-prev {:href "#carouselExampleIndicators" :role "button" :data-slide "prev"}
    [:span.carousel-control-prev-icon {:aria-hidden "true"}]
    [:span.sr-only "Previous"]]
   [:a.carousel-control-next {:href "#carouselExampleIndicators" :role "button" :data-slide "next"}
    [:span.carousel-control-next-icon {:aria-hidden "true"}]
    [:span.sr-only "Next"]]])


(defn conclusions-list []
  (let [path-params (:path-params @(rf/subscribe [:navigation/current-route]))
        conclusions @(rf/subscribe [:starting-conclusions])]
    [:div.container
     [:div#conclusions-list.px-3
      (for [conclusion conclusions]
        [:div {:key (:db/id conclusion)
               :on-click (fn [_e]
                           (rf/dispatch [:continue-discussion :starting-conclusions/select conclusion])
                           (rf/dispatch [:navigation/navigate :routes.discussion/continue
                                         {:id (:id path-params)
                                          :share-hash (:share-hash path-params)}]))}
         [statement-bubble conclusion :neutral]])]]))


(defn history-view
  "Displays the statements it took to get to where the user is."
  []
  (let [history @(rf/subscribe [:discussion-history])
        indexed-history (map-indexed #(vector (- (count history) %1 1) %2) history)]
    [:div#discussion-history.container.px-4
     (for [[count [statement attitude]] indexed-history]
       [:div {:key (:db/id statement)
              :on-click #(rf/dispatch [:discussion.history/time-travel count])}
        [statement-bubble statement attitude]])]))


(rf/reg-event-fx
  :toggle-upvote
  (fn [{:keys [db]} [_ {:keys [db/id] :as statement}]]
    {:http-xhrio {:method :post
                  :uri (str (:rest-backend config) "/votes/up/toggle")
                  :format (ajax/transit-request-format)
                  :params {:statement-id id
                           :nickname (get-in db [:user :name] "Anonymous")
                           :meeting-hash (-> db :meeting :selected :meeting/share-hash)}
                  :response-format (ajax/transit-response-format)
                  :on-success [:upvote-success statement]
                  :on-failure [:ajax-failure]}}))

(rf/reg-event-fx
  :toggle-downvote
  (fn [{:keys [db]} [_ {:keys [db/id] :as statement}]]
    {:http-xhrio {:method :post
                  :uri (str (:rest-backend config) "/votes/down/toggle")
                  :format (ajax/transit-request-format)
                  :params {:statement-id id
                           :nickname (get-in db [:user :name] "Anonymous")
                           :meeting-hash (-> db :meeting :selected :meeting/share-hash)}
                  :response-format (ajax/transit-response-format)
                  :on-success [:downvote-success statement]
                  :on-failure [:ajax-failure]}}))

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