(ns schnaq.interface.views.discussion.labels
  (:require ["react-tippy" :refer [Tooltip]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.text.display-data :refer [fa]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.modal :as modal]))

(defn build-label
  "Takes a label and builds the neccesary html."
  [label set? hover?]
  (let [[badge-color icon-class]
        (case label
          ":comment" ["badge-primary" "fa-comment"]
          ":arrow-right" ["badge-purple" "fa-arrow-right"]
          ":calendar-alt" ["badge-info" "fa-calendar-alt"]
          ":check" ["badge-success" "fa-check"]
          ":ghost" ["badge-dark" "fa-ghost"]
          ":question" ["badge-warning" "fa-question"]
          ":times" ["badge-danger" "fa-times"])
        extra-class (if set? (str badge-color " label-set") badge-color)]
    [:span.badge.badge-pill.px-4
     {:class (if hover? (str extra-class " label") extra-class)}
     [:i {:class (str "m-auto fas " icon-class)}]]))

(defn- anonymous-labels-modal
  "Explain to anonymous users that they need to log in to set and remove labels."
  []
  (modal/anonymous-modal :discussion.anonymous-labels.modal/title
                         :discussion.anonymous-labels.modal/explain
                         :discussion.anonymous-labels.modal/cta))

(defn build-labels
  [statement]
  [:<>
   (let [set-labels (set (:statement/labels statement))]
     (for [label shared-config/allowed-labels]
       [:span.mr-3
        {:key (str "label-" (:db/id statement) "-" label)
         :on-click #(if (set-labels label)
                      (rf/dispatch [:statement.labels/remove (:db/id statement) label])
                      (rf/dispatch [:statement.labels/add (:db/id statement) label]))}
        [build-label label (set-labels label) :hover]]))])

(defn edit-labels-button
  "Give the registered user the ability to add or remove labels to a statement."
  [statement]
  (let [authenticated? @(rf/subscribe [:user/authenticated?])]
    (if authenticated?
      [:> Tooltip
       {:animation "scale"
        :arrow true
        :html (r/as-element [build-labels statement])
        :interactive true
        :offset 5
        :position "bottom"
        :size "big"
        :theme "light"
        :trigger "click"}
       [:div.pr-2.clickable
        [:i {:class (fa :tag)}]]]
      [:div.pr-2.clickable
       {:tabIndex 30
        :on-click #(rf/dispatch [:modal {:show? true
                                         :child [anonymous-labels-modal]}])}
       [:i {:class (fa :tag)}]])))

(rf/reg-event-fx
  :statement.labels/remove
  (fn [{:keys [db]} [_ statement-id label]]
    (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
      {:fx [(http/xhrio-request db :put "/discussion/statement/label/remove"
                                [:statements.labels.update/success]
                                {:share-hash share-hash
                                 :statement-id statement-id
                                 :label label})]})))

(rf/reg-event-db
  :statements.labels.update/success
  (fn [db [_ response]]
    (let [updated-statement (:statement response)]
      (-> db
          (update-in [:discussion :premises :current] #(tools/update-statement-in-list % updated-statement))
          (update-in [:discussion :conclusion :selected] #(if (= (:db/id %) (:db/id updated-statement))
                                                            updated-statement
                                                            %))))))

(rf/reg-event-fx
  :statement.labels/add
  (fn [{:keys [db]} [_ statement-id label]]
    (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
      {:fx [(http/xhrio-request db :put "/discussion/statement/label/add"
                                [:statements.labels.update/success]
                                {:share-hash share-hash
                                 :statement-id statement-id
                                 :label label})]})))
