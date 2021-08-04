(ns schnaq.interface.views.schnaq.create
  (:require [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [schnaq.interface.config :refer [default-anonymous-display-name]]
            [schnaq.interface.text.display-data :refer [fa labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as jq]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.howto.elements :as how-to-elements]
            [schnaq.interface.views.pages :as pages]))

(defn- public-private-discussion
  "Show buttons to toggle between public and private discussion."
  []
  (let [public? (reagent/atom false)]
    (fn []
      (let [user-groups @(rf/subscribe [:user/groups])
            no-hub-exclusive-fn #(when (seq user-groups)
                                   (jq/prop (jq/$ "#hub-exclusive") "checked" false))]
        [:div {:class "col-6"}
         [:h4.mb-5 (labels :discussion.create.public-checkbox/label)]
         [:input#input-public-schnaq {:type :hidden :value @public?}]
         [:button.btn.btn-outline-primary.btn-lg.rounded-1.p-3
          {:class (when @public? "active")
           :type "button"
           :on-click (fn [_e] (reset! public? true)
                       (no-hub-exclusive-fn))}
          [:i.mr-3 {:class (str "fa " (fa :lock-open))}]
          (labels :discussion.create.public-checkbox/public)]
         [:button.btn.btn-outline-secondary.btn-lg.rounded-1.p-3.mx-4
          {:class (when-not @public? "active")
           :type "button"
           :on-click #(reset! public? false)}
          [:i.mr-3 {:class (str "fa " (fa :lock-closed))}]
          (labels :discussion.create.public-checkbox/private)]]))))

(defn- add-schnaq-to-hub
  "Selection if schnaq should be added to a hub."
  []
  (let [user-groups @(rf/subscribe [:user/groups])
        hubs @(rf/subscribe [:hubs/all])]
    (when (seq user-groups)
      [:div.col-6.border-left.pl-5
       [:h4.mb-5 (labels :discussion.create.hub-exclusive-checkbox/title)]
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

(defn- end-time-schnaq-options
  "Options to give a schnaq an end-time."
  ;; TODO labelize and make pretty
  []
  (let [end-time (reagent/atom false)]
    (fn []
      [:div.col-6.border-left.pl-5
       [:h4.mb-5 "Begrenze die Laufzeit deiner Diskussion"]
       (when @end-time
         [:div
          [:label {:for :input-num-days-to-end} "Ende in Tagen"]
          [common/form-input {:type "number"
                              :min 1
                              :id :input-num-days-to-end
                              :placeholder 7
                              :defaultValue 7
                              :required true
                              :onChange #(reset! end-time (oget % [:currentTarget :value]))}]])
       [:button.btn.btn-outline-primary.btn-lg.rounded-1.p-3
        {:class (when @end-time "active")
         :type "button"
         :on-click (fn [_e]
                     (swap! end-time #(or @end-time 7)))}
        [:i.mr-3 {:class (str "fa " (fa :lock-open))}]
        (if @end-time (str @end-time " " "Tage") "7 Tage")]
       [:button.btn.btn-outline-secondary.btn-lg.rounded-1.p-3.mx-4
        {:class (when-not @end-time "active")
         :type "button"
         :on-click (fn [_e]
                     (reset! end-time false))}
        [:i.mr-3 {:class (str "fa " (fa :lock-closed))}]
        "Unbegrenzt"]])))

(defn- create-schnaq-options
  "Options that can be chosen when creating a schnaq."
  []
  [:div.row.my-5
   [public-private-discussion]
   [add-schnaq-to-hub]
   [end-time-schnaq-options]])

(defn- create-schnaq-page []
  [pages/with-nav-and-header
   {:page/heading (labels :schnaq.create/heading)
    :page/subheading (labels :schnaq.create/subheading)
    :page/title (labels :schnaq.create/title)
    :page/classes "base-wrapper bg-white"}
   [:div.container
    [:div.py-3
     [:form
      {:on-submit (fn [e]
                    (jq/prevent-default e)
                    (rf/dispatch [:schnaq.create/new (oget e [:currentTarget :elements])]))}
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
     [how-to-elements/quick-how-to-create]]]])

(defn create-schnaq-view []
  [create-schnaq-page])


;; -----------------------------------------------------------------------------

(rf/reg-event-fx
  :schnaq.create/new
  (fn [{:keys [db]} [_ form-elements]]
    (let [authenticated? (get-in db [:user :authenticated?] false)
          use-origin? (and authenticated?
                           (seq (get-in db [:user :groups] [])))
          nickname (get-in db [:user :names :display] default-anonymous-display-name)
          discussion-title (oget form-elements [:schnaq-title :value])
          public? (= "true" (oget form-elements [:input-public-schnaq :value]))
          exclusive? (when use-origin? (oget form-elements [:hub-exclusive :checked]))
          origin-hub (when use-origin? (oget form-elements [:exclusive-hub-select :value]))
          payload (cond-> {:discussion-title discussion-title
                           :public-discussion? public?}
                          origin-hub (assoc :hub-exclusive? exclusive?
                                            :hub origin-hub)
                          (not authenticated?) (assoc :nickname nickname))
          route (cond origin-hub "/with-hub"
                      authenticated? ""
                      :else "/anonymous")]
      {:fx [(http/xhrio-request db :post (gstring/format "/schnaq/add%s" route)
                                [:schnaq/created]
                                payload
                                [:ajax.error/as-notification])]})))

(rf/reg-event-fx
  :schnaq/created
  (fn [{:keys [db]} [_ {:keys [new-schnaq]}]]
    (let [{:discussion/keys [share-hash edit-hash]} new-schnaq]
      {:db (-> db
               (assoc-in [:schnaq :last-added] new-schnaq)
               (update-in [:schnaqs :all] conj new-schnaq))
       :fx [[:dispatch [:navigation/navigate :routes.schnaq/value {:share-hash share-hash}]]
            [:dispatch [:schnaq/select-current new-schnaq]]
            [:dispatch [:notification/add
                        #:notification{:title (labels :schnaq/created-success-heading)
                                       :body (labels :schnaq/created-success-subheading)
                                       :context :success}]]
            [:localstorage/assoc [:schnaq.last-added/share-hash share-hash]]
            [:localstorage/assoc [:schnaq.last-added/edit-hash edit-hash]]
            [:dispatch [:schnaqs.save-admin-access/to-localstorage-and-db share-hash edit-hash]]]})))
