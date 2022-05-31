(ns schnaq.interface.views.schnaq.create
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.matomo :as matomo]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.pages :as pages]))

(defn- add-schnaq-to-hub
  "Selection if schnaq should be added to a hub."
  []
  (let [user-groups @(rf/subscribe [:user/groups])
        hubs @(rf/subscribe [:hubs/all])
        selected-hub @(rf/subscribe [:hub/selected])
        checked? (not (nil? selected-hub))]
    (when (seq user-groups)
      [:<>
       [:div.form-check
        [:input.form-check-input.big-checkbox
         {:type :checkbox
          :id :hub-exclusive
          :defaultChecked checked?}]
        [:select.form-control.form-select
         {:id :exclusive-hub-select
          :defaultValue selected-hub
          :style {:max-width "80%"}}
         (for [group-id user-groups]
           [:option {:value group-id
                     :key group-id}
            (get-in hubs [group-id :hub/name])])]]
       [:p.small.form-text.text-muted.ms-4 (labels :schnaq.create.hub/help-text)]])))

(defn- create-schnaq-button []
  [:div.text-end
   [:button.btn.btn-dark.p-3.rounded-1
    {:on-click #(matomo/track-event "Active User", "Action", "Create Schnaq")}
    (labels :schnaq.create.button/save)
    [icon :arrow-right "ms-2"]]])

(defn- create-qanda-page []
  (let [selected-hub @(rf/subscribe [:hub/selected])]
    [pages/with-nav-and-header
     {:page/heading (labels :schnaq.create/heading)
      :page/vertical-header? true
      :page/title (labels :schnaq.create/title)
      :page/classes "base-wrapper bg-white"
      :condition/create-schnaq? true}
     [:div.container
      [:div.py-3
       [:form
        {:on-submit (fn [e]
                      (.preventDefault e)
                      (let [form (oget e [:currentTarget :elements])
                            title (oget form [:schnaq-title :value])
                            hub-exclusive? (oget form [:?hub-exclusive :checked])
                            origin-hub (oget form [:?exclusive-hub-select :value])]
                        (rf/dispatch [:schnaq/create {:title title
                                                      :hub-exclusive? hub-exclusive?
                                                      :origin-hub origin-hub
                                                      :selected-hub selected-hub}
                                      [:schnaq.create/success]])))}
        [:div.panel-grey.row.p-4
         [:div.col-12
          [common/form-input {:id :schnaq-title
                              :placeholder (labels :schnaq.create.input/placeholder)
                              :css "font-150"}]]]
        [:div.text-primary.p-3
         [icon :info " my-auto me-3"]
         [:span (labels :schnaq.create/info)]]
        [:div.row.my-5
         [:div.col-12.col-md-8.col-lg-6
          [add-schnaq-to-hub]]
         [:div.col-12.col-md-4.col-lg-6
          [create-schnaq-button]]]]]]]))

(defn create-schnaq-view []
  [create-qanda-page])

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :schnaq/create
 (fn [{:keys [db]} [_ {:keys [title hub-exclusive? origin-hub selected-hub]} success-event]]
   (let [authenticated? (get-in db [:user :authenticated?] false)
         use-origin? (and authenticated?
                          (seq (get-in db [:user :groups] [])))
         nickname (tools/current-display-name db)
         exclusive? (when use-origin? (and hub-exclusive? (not (nil? selected-hub))))
         origin-hub (when use-origin? (or origin-hub selected-hub))
         payload (cond-> {:discussion-title title}
                   origin-hub (assoc :hub-exclusive? exclusive?
                                     :hub origin-hub)
                   (not authenticated?) (assoc :nickname nickname))]
     {:fx [(http/xhrio-request db :post "/schnaq/add"
                               success-event
                               payload
                               [:ajax.error/as-notification])]})))

(rf/reg-event-fx
 :schnaq.create/success
 (fn [{:keys [db]} [_ {:keys [new-schnaq]}]]
   (let [{:discussion/keys [share-hash edit-hash creation-secret]} new-schnaq
         updated-secrets (assoc (get-in db [:discussion :schnaqs :creation-secrets]) share-hash creation-secret)]
     {:db (-> db
              (assoc-in [:schnaq :last-added] new-schnaq)
              (assoc-in [:discussion :schnaqs :creation-secrets] updated-secrets)
              (update-in [:schnaqs :all] conj new-schnaq))
      :fx [[:dispatch [:navigation/navigate :routes.schnaq/start {:share-hash share-hash}]]
           [:dispatch [:schnaq/select-current new-schnaq]]
           [:dispatch [:notification/add
                       #:notification{:title (labels :schnaq/created-success-heading)
                                      :body (labels :schnaq/created-success-subheading)
                                      :context :success}]]
           [:localstorage/assoc [:schnaq.last-added/share-hash share-hash]]
           [:localstorage/assoc [:schnaq.last-added/edit-hash edit-hash]]
           [:localstorage/assoc [:discussion.schnaqs/creation-secrets updated-secrets]]
           [:dispatch [:schnaqs.save-admin-access/to-localstorage-and-db share-hash edit-hash]]]})))

;; -----------------------------------------------------------------------------
;; Create Demo schnaq

(rf/reg-event-fx
 :schnaq.create/demo
 (fn [_ _]
   {:fx [[:dispatch [:schnaq/create {:title (labels :schnaq.create.demo/title)}
                     [:schnaq.create.demo/success]]]]}))

(rf/reg-event-fx
 :schnaq.create.demo/success
 (fn [_ [_ {:keys [new-schnaq] :as response}]]
   {:fx [[:dispatch [:schnaq.create/success response]]
         [:dispatch [:schnaq/select-current new-schnaq]]
         [:dispatch [:discussion.add.statement/starting (labels :schnaq.create.demo/pinned-post) true]]
         [:dispatch [:discussion.add.statement/starting (labels :schnaq.create.demo/post-1) false]]
         ;; Activate after it is possible to create activations as a free user
         #_[:dispatch [:schnaq.poll/create {:title (labels :schnaq.create.demo.poll/title)
                                            :poll-type :poll.type/single-choice
                                            :options [(labels :schnaq.create.demo.poll/option-1)
                                                      (labels :schnaq.create.demo.poll/option-2)
                                                      (labels :schnaq.create.demo.poll/option-3)]}]]
         #_[:dispatch [:activation/start]]]}))
