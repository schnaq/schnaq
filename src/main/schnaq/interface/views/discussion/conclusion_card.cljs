(ns schnaq.interface.views.discussion.conclusion-card
  (:require [ghostwheel.core :refer [>defn-]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.config.shared :as shared-config]
            [schnaq.database.specs :as specs]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.components.schnaq :as sc]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.markdown :as md]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.discussion.edit :as edit]
            [schnaq.interface.views.discussion.filters :as filters]
            [schnaq.interface.views.discussion.labels :as labels]
            [schnaq.interface.views.discussion.logic :as logic]
            [schnaq.interface.views.user :as user]))

(defn- call-to-action-schnaq
  "If no contributions are available, add a call to action to engage the users."
  [body]
  [motion/fade-in-and-out
   [:article.call-to-contribute.m-3
    [:div.alert.alert-light.text-light.row.layered-wave-background.p-md-5.rounded-1
     [:div.col-2.py-md-5.d-flex
      [:img.w-75.align-self-center {:src (img-path :schnaqqifant/three-d-head)}]]
     [:div.col-10.py-md-5
      body]]]])

(defn- call-to-discuss
  "If no contributions are available, add a call to action to engage the users."
  []
  [call-to-action-schnaq
   [:<>
    [:h2 (labels :call-to-contribute/body)]
    [:p.mt-5 (labels :how-to/ask-question)
     [:a.text-dark.btn.btn-link {:href (reitfe/href :routes/how-to)}
      (labels :how-to/answer-question)]]]])

(defn- call-to-q&a
  "If no statements are present show the access code in Q&A"
  []
  [call-to-action-schnaq
   [:<>
    [:p.h5 (labels :qanda.call-to-action/display-code)]
    [:p.h1.py-3 [sc/access-code]]
    [:p.h5 (labels :qanda.call-to-action/intro-1) " "
     [:span.text-monospace.mx-2 {:href "https://schnaq.app"
                                 :target :_blank}
      "https://schnaq.app"]
     " "
     (labels :qanda.call-to-action/intro-2)]
    [:p.pt-3 [icon :info "m-auto fas"] " "
     (labels :qanda.call-to-action/help) " " [icon :share "m-auto fas"]]]])

(defn- call-to-action-content
  "Either display cta for discussion or Q&A"
  []
  (if @(rf/subscribe [:schnaq.selected/access-code])
    [call-to-q&a]
    [call-to-discuss]))


;; -----------------------------------------------------------------------------

(defn up-down-vote
  "Add inline panel for up and down votes."
  [statement]
  (let [votes @(rf/subscribe [:local-votes])
        [local-upvote? local-downvote?] @(rf/subscribe [:votes/upvoted-or-downvoted (:db/id statement)])
        ;; Do not use or shortcut, since the value can be false and should be preferably selected over backend value
        upvoted? (if (nil? local-upvote?) (:meta/upvoted? statement) local-upvote?)
        downvoted? (if (nil? local-downvote?) (:meta/downvoted? statement) local-downvote?)]
    [:div.d-flex.flex-row.align-items-center
     [:div.mr-2
      {:class (if upvoted? "badge badge-upvote-selected" "badge badge-upvote")
       :on-click (fn [e]
                   (js-wrap/stop-propagation e)
                   (rf/dispatch [:discussion/toggle-upvote statement]))}
      [icon :arrow-up "vote-arrow m-auto"]]
     [:span.mr-3 (logic/get-up-votes statement votes)]
     [:div.mr-2
      {:class (if downvoted? "badge badge-downvote-selected" "badge badge-downvote")
       :on-click (fn [e]
                   (js-wrap/stop-propagation e)
                   (rf/dispatch [:discussion/toggle-downvote statement]))}
      [icon :arrow-down "vote-arrow m-auto"]]
     [:span (logic/get-down-votes statement votes)]]))

(defn- statement-information-row [statement]
  (let [path-params (:path-params @(rf/subscribe [:navigation/current-route]))]
    [:div.d-flex.flex-wrap.align-items-center
     [:a.badge.mr-3
      {:href (reitfe/href :routes.schnaq.select/statement (assoc path-params :statement-id (:db/id statement)))}
      [:button.btn.btn-sm.btn-dark
       [icon :plus "text-white m-auto fa-xs"]]
      [:span.ml-2.text-dark (labels :statement/reply)]]
     [up-down-vote statement]
     [:div.ml-sm-0.ml-lg-auto
      [badges/extra-discussion-info-badges statement]]]))

(defn- mark-as-answer-button
  "Show a button to mark a statement as an answer."
  [statement]
  (let [history-length (count @(rf/subscribe [:discussion-history]))
        statement-labels (set (:statement/labels statement))
        label ":check"
        checked? (statement-labels label)]
    (when (= 1 history-length)                              ;; history-length == 1 => a reply to a question
      [:section.w-100
       [:button.btn.btn-sm.btn-link.text-dark.pr-0
        {:on-click #(if checked?
                      (rf/dispatch [:statement.labels/remove statement label])
                      (rf/dispatch [:statement.labels/add statement label]))}
        [:small.pr-2 (if checked? (labels :qanda.button.mark/as-unanswered) (labels :qanda.button.mark/as-answer))]
        [labels/build-label (if checked? label ":unchecked")]]])))

(defn- show-active-labels
  "Shows all active labels."
  [statement]
  (let [statement-labels (set (:statement/labels statement))]
    (when (seq statement-labels)
      [:div.mx-1
       (for [label statement-labels]
         [:span.pr-1 {:key (str "show-label-" (:db/id statement) label)}
          [labels/build-label label]])])))

(>defn- card-highlighting
  "Add card-highlighting to a statement card."
  [{:keys [meta/answered? statement/type]}]
  [::specs/statement :ret string?]
  (str "highlight-card-" (if answered? "answered" (name (or type :neutral)))))

(defn statement-card
  "Display a full interactive statement. Takes `additional-content`, e.g. the
  answer of a question."
  ([statement]
   [statement-card statement nil])
  ([statement additional-content]
   (let [q-and-a? @(rf/subscribe [:schnaq.mode/qanda?])]
     [:article.statement-card.my-2
      [:div.d-flex.flex-row
       [:div {:class (card-highlighting statement)}]
       [:div.card-view.card-body.py-2
        (when (:meta/new? statement)
          [:div.bg-primary.p-2.rounded-1.d-inline-block.text-white.small.float-right
           (labels :discussion.badges/new)])
        [:div.pt-2.d-flex
         [:div.mr-auto [user/user-info statement 42 "w-100"]]
         (when q-and-a?
           [:div.ml-auto [mark-as-answer-button statement]])]
        [:div.my-4]
        [:div.text-purple-dark
         [md/as-markdown (:statement/content statement)]]
        [statement-information-row statement]
        (when-not q-and-a?
          [show-active-labels statement])
        additional-content]]])))

(defn reduced-answer
  "A reduced statement-card focusing on the statement."
  [statement]
  (let [path-params (:path-params @(rf/subscribe [:navigation/current-route]))
        statement-labels (set (:statement/labels statement))]
    [:article.statement-card.my-1
     [:div.d-flex.flex-row
      [:div {:class (str "highlight-card-" (name (or (:statement/type statement) :neutral)))}]
      [:div.card-view.card-body
       [:div.d-flex.justify-content-start.pt-2
        [user/user-info statement 25 "w-100"]]
       [:div.my-3]
       [:div.text-purple-dark
        [md/as-markdown (:statement/content statement)]]
       [:div.d-flex.flex-wrap.align-items-center
        [:a.mr-3
         {:href (reitfe/href :routes.schnaq.select/statement (assoc path-params :statement-id (:db/id statement)))}
         [:small.text-muted (labels :statement/reply)]]
        [up-down-vote statement]]
       (when (seq statement-labels)
         [:div.mx-1
          (for [label statement-labels]
            [:span.pr-1 {:key (str "show-label-answer" (:db/id statement) label)}
             [labels/build-label label]])])]]]))

(defn answer-card
  "Display the answer directly inside the statement itself."
  [statement]
  (let [answers (filter #(some #{":check"} (:statement/labels %)) (:statement/answers statement))]
    [statement-card statement
     (when (seq answers)
       [:div.mt-2
        (for [answer answers]
          (with-meta
            [reduced-answer answer]
            {:key (str "answer-" (:db/id answer))}))])]))

(defn- statement-or-edit-wrapper
  "Either show the clickable statement, or its edit-view."
  [statement]
  (let [currently-edited? @(rf/subscribe [:statement.edit/ongoing? (:db/id statement)])]
    (if currently-edited?
      [edit/edit-card-statement statement]
      [answer-card statement])))

(defn- sort-statements
  "Sort statements according to the filter method. If we are in q-and-a-mode,
  then always display own statements first."
  [q-and-a? user statements sort-method local-votes]
  (let [selection-function (if (:authenticated? user)
                             #(= (:id user) (get-in % [:statement/author :db/id]))
                             #(= (get-in user [:names :display]) (get-in % [:statement/author :user/nickname])))
        keyfn (case sort-method
                :newest :statement/created-at
                :popular #(logic/calculate-votes % local-votes))
        own-statements (filter selection-function statements)
        other-statements (sort-by keyfn > (remove selection-function statements))]
    (if q-and-a?
      (concat own-statements other-statements)
      (sort-by keyfn > statements))))

(defn conclusion-cards-list
  "Displays a list of conclusions."
  []
  (let [active-filters @(rf/subscribe [:filters/active])
        sort-method @(rf/subscribe [:discussion.statements/sort-method])
        local-votes @(rf/subscribe [:local-votes])
        user @(rf/subscribe [:user/current])
        q-and-a? @(rf/subscribe [:schnaq.mode/qanda?])
        card-column-class (if shared-config/embedded? "card-columns-embedded" "card-columns-discussion")
        current-premises @(rf/subscribe [:discussion.premises/current])]
    (if (seq current-premises)
      (let [sorted-conclusions (sort-statements q-and-a? user current-premises sort-method local-votes)
            filtered-conclusions (filters/filter-statements sorted-conclusions active-filters (rf/subscribe [:local-votes]))]
        [:div.card-columns.pb-3 {:class card-column-class}
         (for [statement filtered-conclusions]
           (with-meta
             [motion/fade-in-and-out
              [statement-or-edit-wrapper statement]
              0.1]
             {:key (:db/id statement)}))])
      [call-to-action-content])))

(rf/reg-event-fx
  :discussion.select/conclusion
  (fn [{:keys [db]} [_ conclusion]]
    (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
      {:db (assoc-in db [:discussion :conclusion :selected] conclusion)
       :fx [(http/xhrio-request db :get "/discussion/statements/for-conclusion"
                                [:discussion.premises/set-current]
                                {:conclusion-id (:db/id conclusion)
                                 :share-hash share-hash
                                 :display-name (tools/current-display-name db)}
                                [:ajax.error/as-notification])]})))

(rf/reg-event-fx
  :discussion.statements/reload
  (fn [{:keys [db]} _]
    (let [path (get-in db [:current-route :data :name])]
      (case path
        :routes.schnaq.select/statement {:fx [[:dispatch [:discussion.query.statement/by-id]]]}
        :routes.schnaq/start {:fx [[:dispatch [:discussion.query.conclusions/starting]]]}
        {}))))

(rf/reg-event-db
  :discussion.premises/set-current
  (fn [db [_ {:keys [premises]}]]
    (assoc-in db [:discussion :premises :current] premises)))

(rf/reg-sub
  :local-votes
  (fn [db _]
    (get db :votes)))

(rf/reg-event-fx
  :discussion/toggle-upvote
  (fn [{:keys [db]} [_ {:keys [db/id meta/upvoted?] :as statement}]]
    {:db (-> db
             (update-in [:votes :own :up id] #(not (if (nil? %) upvoted? %)))
             (assoc-in [:votes :own :down id] false))
     :fx [(http/xhrio-request db :post "/discussion/statement/vote/up" [:upvote-success statement]
                              {:statement-id id
                               :nickname (tools/current-display-name db)
                               :share-hash (-> db :schnaq :selected :discussion/share-hash)}
                              [:ajax.error/as-notification])]}))

(rf/reg-event-fx
  :discussion/toggle-downvote
  (fn [{:keys [db]} [_ {:keys [db/id meta/downvoted?] :as statement}]]
    {:db (-> db
             (assoc-in [:votes :own :up id] false)
             (update-in [:votes :own :down id] #(not (if (nil? %) downvoted? %))))
     :fx [(http/xhrio-request db :post "/discussion/statement/vote/down" [:downvote-success statement]
                              {:statement-id id
                               :nickname (tools/current-display-name db)
                               :share-hash (-> db :schnaq :selected :discussion/share-hash)}
                              [:ajax.error/as-notification])]}))

(rf/reg-event-db
  :upvote-success
  (fn [db [_ {:keys [db/id]} {:keys [operation]}]]
    (case operation
      :added (update-in db [:votes :up id] inc)
      :removed (update-in db [:votes :up id] dec)
      :switched (-> db
                    (update-in [:votes :up id] inc)
                    (update-in [:votes :down id] dec)))))

(rf/reg-event-db
  :downvote-success
  (fn [db [_ {:keys [db/id]} {:keys [operation]}]]
    (case operation
      :added (update-in db [:votes :down id] inc)
      :removed (update-in db [:votes :down id] dec)
      :switched (-> db
                    (update-in [:votes :down id] inc)
                    (update-in [:votes :up id] dec)))))

(rf/reg-sub
  :votes/upvoted-or-downvoted
  (fn [db [_ statement-id]]
    [(get-in db [:votes :own :up statement-id])
     (get-in db [:votes :own :down statement-id])]))
