(ns schnaq.interface.views.schnaq.create
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [default-anonymous-display-name]]
            [schnaq.interface.text.display-data :refer [fa labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as jq]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.howto.elements :as how-to-elements]
            [schnaq.interface.views.pages :as pages]))


(defn- public-private-discussion [user-groups]
  (let [public? @(rf/subscribe [:schnaq.create/public?])
        no-hub-exclusive-fn #(when (seq user-groups)
                               (jq/prop (jq/$ "#hub-exclusive") "checked" false))]
    [:div
     (if (empty? user-groups)
       {:class "col-12"}
       {:class "col-6"})
     [:h4.mb-5 (labels :discussion.create.public-checkbox/label)]
     [:button.btn.btn-outline-primary.btn-lg.p-3
      {:class (when public? "active")
       :type "button"
       :on-click (fn [_e]
                   (rf/dispatch [:schnaq.create/public! true])
                   (no-hub-exclusive-fn))}
      [:i.mr-3 {:class (str "fa " (fa :lock-open))}]
      (labels :discussion.create.public-checkbox/public)]
     [:button.btn.btn-outline-secondary.btn-lg.p-3.mx-4
      {:class (when-not public? "active")
       :type "button"
       :on-click (fn [_e] (rf/dispatch [:schnaq.create/public! false]))}
      [:i.mr-3 {:class (str "fa " (fa :lock-closed))}]
      (labels :discussion.create.public-checkbox/private)]]))

(defn- add-schnaq-to-hub [user-groups]
  (let [hubs @(rf/subscribe [:hubs/all])]
    (when (seq user-groups)
      [:div.col-6.border-left.pl-5
       [:h4.mb-5 (labels :discussion.create.hub-exclusive-checkbox/label)]
       [:div.form-check.text-center
        [:input.form-check-input.big-checkbox
         {:type :checkbox
          :id :hub-exclusive
          :defaultChecked false
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

(defn- create-schnaq-options
  "Options that can be chosen when creating a schnaq."
  []
  (let [user-groups @(rf/subscribe [:user/groups])]
    [:div.row.my-5
     [public-private-discussion user-groups]
     [add-schnaq-to-hub user-groups]]))

(defn- create-schnaq-page []
  (let [dispatch-schnaq-creation #(rf/dispatch [:schnaq.create/new (oget % [:currentTarget :elements])])]
    [pages/with-nav-and-header
     {:page/heading (labels :schnaq.create/heading)
      :page/subheading (labels :schnaq.create/subheading)
      :page/class "base-wrapper bg-white"}
     [:div.container
      [:div.py-3
       [:form
        {:on-submit (fn [e]
                      (jq/prevent-default e)
                      (dispatch-schnaq-creation e))}
        [:h4.mb-5 (labels :schnaq.create.input/title)]
        [:div.panel-grey.row.p-4
         [:div.col-12
          [common/form-input {:id :schnaq-title
                              :placeholder (labels :schnaq.create.input/placeholder)
                              :css "font-150"}]]]
        [:div.row.text-primary.p-3
         [:i.my-auto.mr-3 {:class (str "fa " (fa :info))}]
         [:span (labels :schnaq.create/info)]]
        [create-schnaq-options]
        [:div.row.px-1.py-3
         [:button.btn.btn-dark.p-3.rounded-1.ml-auto
          (labels :schnaq.create.button/save)
          [:i.ml-2 {:class (str "fa " (fa :arrow-right))}]]]]
       [how-to-elements/quick-how-to-create]]]]))

(defn create-schnaq-view []
  [create-schnaq-page])

(rf/reg-event-fx
  :schnaq.create/new
  (fn [{:keys [db]} [_ form-elements]]
    (let [use-origin? (and (get-in db [:user :authenticated?] false)
                           (seq (get-in db [:user :groups] [])))
          nickname (get-in db [:user :names :display] default-anonymous-display-name)
          discussion-title (oget form-elements [:schnaq-title :value])
          public? (get-in db [:schnaq :create :public] false)
          exclusive? (when use-origin? (oget form-elements [:hub-exclusive :checked]))
          origin-hub (when use-origin? (oget form-elements [:exclusive-hub-select :value]))]
      {:fx [(http/xhrio-request db :post "/schnaq/add"
                                [:schnaq/created]
                                (cond->
                                  {:nickname nickname
                                   :discussion-title discussion-title
                                   :public-discussion? public?}
                                  use-origin? (merge {:hub-exclusive? exclusive?
                                                      :origin origin-hub}))
                                [:ajax.error/as-notification])]})))

(rf/reg-event-fx
  :schnaq/created
  (fn [{:keys [db]} [_ {:keys [new-discussion]}]]
    (let [{:discussion/keys [share-hash edit-hash]} new-discussion]
      {:db (-> db
               (assoc-in [:schnaq :last-added] new-discussion)
               (update-in [:schnaqs :all] conj new-discussion))
       :fx [[:dispatch [:navigation/navigate :routes.schnaq/start {:share-hash share-hash}]]
            [:dispatch [:schnaq/select-current new-discussion]]
            [:dispatch [:schnaq.create/public! false]]
            [:dispatch [:notification/add
                        #:notification{:title (labels :schnaq/created-success-heading)
                                       :body (labels :schnaq/created-success-subheading)
                                       :context :success}]]
            [:localstorage/assoc [:schnaq.last-added/share-hash share-hash]]
            [:localstorage/assoc [:schnaq.last-added/edit-hash edit-hash]]
            [:dispatch [:schnaqs.save-admin-access/to-localstorage-and-db share-hash edit-hash]]]})))

(rf/reg-event-db
  :schnaq.create/public!
  (fn [db [_ public?]]
    (assoc-in db [:schnaq :create :public] public?)))

(rf/reg-sub
  :schnaq.create/public?
  (fn [db _] (get-in db [:schnaq :create :public] false)))