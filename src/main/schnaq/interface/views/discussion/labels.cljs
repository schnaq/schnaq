(ns schnaq.interface.views.discussion.labels
  (:require [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as jsw]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.modal :as modal]))

(defn build-label
  "Takes a label and builds the necessary html."
  [label set? hover?]
  (let [[badge-color icon-name]
        (case label
          ":comment" ["label-blue" :comment]
          ":arrow-right" ["label-purple" :arrow-right]
          ":calendar-alt" ["label-yellow" :calendar-alt]
          ":check" ["label-green" :check/normal]
          ":ghost" ["label-dark" :ghost]
          ":question" ["label-cyan" :question]
          ":times" ["label-red" :cross]
          ":unchecked" ["label-teal" :check/normal])
        extra-class (if set? (str badge-color " label-set") badge-color)]
    [:span.badge.badge-pill.px-4
     {:class (if hover? (str extra-class " label") extra-class)}
     [icon icon-name "m-auto"]]))

(defn- anonymous-labels-modal
  "Explain to anonymous users that they need to log in to set and remove labels."
  []
  (modal/anonymous-modal :discussion.anonymous-labels.modal/title
                         :discussion.anonymous-labels.modal/explain
                         :discussion.anonymous-labels.modal/cta))

(defn build-labels
  [statement]
  [:div.text-center
   (let [set-labels (set (:statement/labels statement))]
     (for [label shared-config/allowed-labels]
       [:span.mx-2
        {:key (str "label-" (:db/id statement) "-" label)
         :on-click #(if (set-labels label)
                      (rf/dispatch [:statement.labels/remove statement label])
                      (rf/dispatch [:statement.labels/add statement label]))}
        [build-label label (set-labels label) :hover]]))])

(defn edit-labels-button
  "Give the registered user the ability to add or remove labels to a statement."
  [statement]
  (let [authenticated? @(rf/subscribe [:user/authenticated?])]
    ;; This outer div helps accessibility when popup is open – https://atomiks.github.io/tippyjs/v6/accessibility/#interactivity
    [:div#label-selector
     (if authenticated?
       [tooltip/html
        [build-labels statement]
        [:div.pr-2.clickable
         [icon :tag]]
        {:animation "scale"
         :appendTo jsw/document-body}]
       [:div.pr-2.clickable
        {:tabIndex 30
         :on-click #(rf/dispatch [:modal {:show? true
                                          :child [anonymous-labels-modal]}])}
        [icon :tag]])]))

(rf/reg-event-fx
  :statement.labels/remove
  (fn [{:keys [db]} [_ statement label]]
    (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
          updated-statement (update statement :statement/labels (fn [labels] (-> labels set (disj label))))]
      {:db (update-in db [:search :schnaq :current :result] #(tools/update-statement-in-list % updated-statement))
       :fx [(http/xhrio-request db :put "/discussion/statement/label/remove"
                                [:statement.labels.update/success]
                                {:share-hash share-hash
                                 :statement-id (:db/id statement)
                                 :label label
                                 :display-name (tools/current-display-name db)})]})))

(rf/reg-event-db
  :statement.labels.update/success
  (fn [db [_ response]]
    (let [updated-statement (:statement response)]
      (-> db
          (update-in [:discussion :premises :current] #(tools/update-statement-in-list % updated-statement))
          (update-in [:discussion :conclusion :selected] #(if (= (:db/id %) (:db/id updated-statement))
                                                            updated-statement
                                                            %))))))

(rf/reg-event-fx
  :statement.labels/add
  (fn [{:keys [db]} [_ statement label]]
    (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
          updated-statement (update statement :statement/labels conj label)]
      {:db (update-in db [:search :schnaq :current :result] #(tools/update-statement-in-list % updated-statement))
       :fx [(http/xhrio-request db :put "/discussion/statement/label/add"
                                [:statement.labels.update/success]
                                {:share-hash share-hash
                                 :statement-id (:db/id statement)
                                 :label label
                                 :display-name (tools/current-display-name db)})]})))
