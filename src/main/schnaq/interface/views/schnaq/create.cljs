(ns schnaq.interface.views.schnaq.create
  (:require [oops.core :refer [oget]]
            [schnaq.interface.config :refer [default-anonymous-display-name]]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as jq]
            [schnaq.interface.views.howto.elements :as how-to-elements]
            [schnaq.interface.views.pages :as pages]
            [re-frame.core :as rf]
            [schnaq.interface.views.common :as common]))

(defn- create-schnaq-options
  "Options that can be chosen when creating a schnaq."
  []
  (let [user-groups @(rf/subscribe [:user/groups])
        hubs @(rf/subscribe [:hubs/all])]
    [:div.row.mt-4.p-4.text-center.panel-white
     [:div.form-check
      (if (empty? user-groups)
        {:class "col-12"}
        {:class "col-6"})
      [:input.form-check-input.big-checkbox
       {:type :checkbox
        :id :public-discussion
        :defaultChecked false
        :on-change
        #(when (and (seq user-groups) (oget % [:target :checked]))
           (jq/prop (jq/$ "#hub-exclusive") "checked" false))}]
      [:label.form-check-label.display-6.pl-1 {:for :public-discussion}
       (labels :discussion.create.public-checkbox/label)]
      [:small.form-text.text-muted (labels :schnaq.create.public/help-text)]]
     (when (seq user-groups)
       [:div.form-check.col-6
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
            (get-in hubs [group-id :hub/name])])]])]))

(defn- create-schnaq-page []
  (let [dispatch-fn #(rf/dispatch [:schnaq.create/new (oget % [:currentTarget :elements])])]
    [pages/with-nav-and-header
     {:page/heading (labels :schnaq.create/heading)}
     [:div.container
      [:div.py-3.mt-3
       [:form
        {:on-submit (fn [e]
                      (jq/prevent-default e)
                      (dispatch-fn e))
         :on-key-down (fn [e]
                        (when (jq/ctrl-press e 13) (dispatch-fn e)))}
        [:div.panel-white.row.p-4
         [:div.col-12
          [common/form-input {:id :schnaq-title
                              :placeholder (labels :schnaq.create.input/placeholder)
                              :css "font-150"}]]]
        [create-schnaq-options]
        [:div.pt-3.text-center
         [:button.btn.button-primary (labels :schnaq.create.button/save)]]]
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
          public? (oget form-elements [:public-discussion :checked])
          exclusive? (when use-origin? (oget form-elements [:hub-exclusive :checked]))
          origin-hub (when use-origin? (oget form-elements [:exclusive-hub-select :value]))]
      {:fx [(http/xhrio-request db :post "/schnaq/add"
                                [:schnaq/created]
                                (cond->
                                  {:nickname nickname
                                   :discussion {:discussion/title discussion-title}
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
            [:dispatch [:notification/add
                        #:notification{:title (labels :schnaq/created-success-heading)
                                       :body (labels :schnaq/created-success-subheading)
                                       :context :success}]]
            [:localstorage/assoc [:schnaq.last-added/share-hash share-hash]]
            [:localstorage/assoc [:schnaq.last-added/edit-hash edit-hash]]
            [:dispatch [:schnaqs.save-admin-access/to-localstorage share-hash edit-hash]]]})))