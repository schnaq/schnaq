(ns schnaq.interface.views.schnaq.create
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as jq]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.pages :as pages]))

(defn- add-schnaq-to-hub
  "Selection if schnaq should be added to a hub."
  []
  (let [user-groups @(rf/subscribe [:user/groups])
        hubs @(rf/subscribe [:hubs/all])]
    (when (seq user-groups)
      [:section.text-center
       [:h4.mb-3 (labels :discussion.create.hub-exclusive-checkbox/title)]
       [:div.form-check
        [:input.form-check-input.big-checkbox
         {:type :checkbox
          :id :hub-exclusive
          :defaultChecked true
          :on-change
          #(when (oget % [:target :checked])
             (jq/prop (jq/$ "#public-discussion") "checked" false))}]
        [:label.form-check-label.display-6.pl-1 {:for :hub-exclusive}
         (labels :discussion.create.hub-exclusive-checkbox/label)]
        [:small.form-text.text-muted (labels :schnaq.create.hub/help-text)]
        [:select.form-control.custom-select.mt-3
         {:id :exclusive-hub-select
          :style {:max-width "80%"}}
         (for [group-id user-groups]
           [:option {:value group-id
                     :key group-id}
            (get-in hubs [group-id :hub/name])])]]])))

(defn- create-schnaq-button []
  [:button.btn.btn-dark.p-3.rounded-1.ml-auto
   (labels :schnaq.create.button/save)
   [icon :arrow-right "ml-2"]])

(defn- create-qanda-page []
  (let [selected-hub @(rf/subscribe [:hub/selected])]
    [pages/with-nav-and-header
     {:page/heading (labels :schnaq.create/heading)
      :page/subheading (labels :schnaq.create.qanda/subheading)
      :page/title (labels :schnaq.create/title)
      :page/classes "base-wrapper bg-white"
      :condition/create-schnaq? true}
     [:div.container
      [:div.py-3
       [:form
        {:on-submit (fn [e]
                      (jq/prevent-default e)
                      (rf/dispatch [:schnaq.create/new
                                    (oget e [:currentTarget :elements])
                                    selected-hub]))}
        [:h4.mb-5 (labels :schnaq.create.input/title)]
        [:div.panel-grey.row.p-4
         [:div.col-12
          [common/form-input {:id :schnaq-title
                              :placeholder (labels :schnaq.create.input/placeholder)
                              :css "font-150"}]]]
        [:div.row.text-primary.p-3
         [icon :info "my-auto mr-3"]
         [:span (labels :schnaq.create/info)]]
        (when (= :none selected-hub)
          [:div.row.my-5
           [:div.col-12.col-xl-6
            [add-schnaq-to-hub]]])
        [:div.row.px-1.py-3
         [create-schnaq-button]]]]]]))

(defn create-schnaq-view []
  [create-qanda-page])
;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :schnaq.create/new
 (fn [{:keys [db]} [_ form-elements selected-hub]]
   (let [authenticated? (get-in db [:user :authenticated?] false)
         use-origin? (and authenticated?
                          (seq (get-in db [:user :groups] [])))
         nickname (tools/current-display-name db)
         discussion-title (oget form-elements [:schnaq-title :value])
         exclusive? (when use-origin? (or (oget form-elements [:?hub-exclusive :checked]) (not= :none selected-hub)))
         origin-hub (when use-origin? (or (oget form-elements [:?exclusive-hub-select :value]) selected-hub))
         payload (cond-> {:discussion-title discussion-title}
                   origin-hub (assoc :hub-exclusive? exclusive?
                                     :hub origin-hub)
                   (not authenticated?) (assoc :nickname nickname))]
     {:fx [(http/xhrio-request db :post "/schnaq/add"
                               [:schnaq/created]
                               payload
                               [:ajax.error/as-notification])]})))

(rf/reg-event-fx
 :schnaq/created
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
