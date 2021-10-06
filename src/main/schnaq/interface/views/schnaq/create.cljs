(ns schnaq.interface.views.schnaq.create
  (:require [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reitit.frontend.easy :as rfe]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.icons :refer [fa]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as jq]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.howto.elements :as how-to-elements]
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
  []
  (let [end-time (reagent/atom false)]
    (fn []
      [:section.text-center
       [:h4.mb-3 (labels :discussion.progress.creation/heading)]
       (when @end-time
         [:div
          [:label {:for :input-num-days-to-end} (labels :discussion.progress.creation/label)]
          [common/form-input {:type :number
                              :min 1
                              :id :input-num-days-to-end
                              :placeholder 7
                              :defaultValue 7
                              :required true
                              :onChange #(reset! end-time (oget % [:currentTarget :value]))}]])
       [:button.btn.btn-outline-primary.btn-lg.rounded-1.p-3
        {:class (when @end-time "active")
         :type "button"
         :on-click (fn [_e] (swap! end-time #(or @end-time 7)))}
        [fa :calendar "mr-3"]
        (gstring/format (labels :discussion.progress.creation/button-limit) (or @end-time 7))]
       [:button.btn.btn-outline-secondary.btn-lg.rounded-1.p-3.mx-4
        {:class (when-not @end-time "active")
         :type "button"
         :on-click #(reset! end-time false)}
        [:i.mr-3 {:class (str "fas " (fa :circle-notch))}]
        (labels :discussion.progress.creation/button-unlimited)]])))

(defn- create-schnaq-options
  "Options that can be chosen when creating a schnaq."
  []
  [:div.row.my-5
   [:div.col-12.col-xl-6.pb-5
    [end-time-schnaq-options]]
   [:div.col-12.col-xl-6.border-left
    [add-schnaq-to-hub]]])

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
       [:button.btn.btn-dark-highlight.p-3.rounded-1.ml-auto
        (labels :schnaq.create.button/save)
        [fa :arrow-right "ml-2"]]]]
     [how-to-elements/quick-how-to-create]]]])

(defn- create-schnaq-type-selection-page
  "Choose whether the type of schnaq you are starting is a Q&A or a discussion."
  []
  [pages/with-nav-and-header
   {:page/title (labels :schnaq.create/title)
    :page/more-for-heading
    [:section
     {:style {:min-height "600px"}}
     [:h1.text-center.pb-5 (labels :schnaq.create.dispatch/heading)]
     [:div.row.mx-auto.pb-5
      {:style {:max-width "800px"}}
      [:div.col-md-6.col-12.text-center
       [buttons/a-big
        (labels :schnaq.create.dispatch/qanda)
        :todo
        "btn-outline-white mb-3 miw-75 disabled"]
       [:p.small.text-left
        [:i.my-auto.mr-1 {:class (str "fa " (fa :info))}]
        [:strong (labels :schnaq.create.dispatch.qanda/coming-soon)] [:br]
        (labels :schnaq.create.dispatch.qanda/explain)]
       [:p.small.text-left
        (labels :schnaq.create.dispatch.qanda/share)]]
      [:div.col-md-6.col-12.text-center
       [buttons/a-big
        (labels :schnaq.create.dispatch/discussion)
        (rfe/href :routes.schnaq.create/discussion)
        "btn-outline-white mb-3 miw-75"]
       [:p.small.text-left
        [:i.my-auto.mr-1 {:class (str "fa " (fa :info))}]
        (labels :schnaq.create.dispatch.discussion/explain)]
       [:p.small.text-left
        (labels :schnaq.create.dispatch.discussion/share)]]]]}])

(defn create-schnaq-view []
  [create-schnaq-type-selection-page])

(defn create-discussion-view []
  [create-schnaq-page])


;; -----------------------------------------------------------------------------

(rf/reg-event-fx
  :schnaq.create/new
  (fn [{:keys [db]} [_ form-elements]]
    (let [authenticated? (get-in db [:user :authenticated?] false)
          use-origin? (and authenticated?
                           (seq (get-in db [:user :groups] [])))
          nickname (tools/current-display-name db)
          discussion-title (oget form-elements [:schnaq-title :value])
          exclusive? (when use-origin? (oget form-elements [:hub-exclusive :checked]))
          origin-hub (when use-origin? (oget form-elements [:exclusive-hub-select :value]))
          end-from-now (oget form-elements [:?input-num-days-to-end :value])
          payload (cond-> {:discussion-title discussion-title}
                          origin-hub (assoc :hub-exclusive? exclusive?
                                            :hub origin-hub)
                          end-from-now (assoc :ends-in-days (js/parseInt end-from-now))
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
       :fx [[:dispatch [:navigation/navigate :routes.schnaq/value {:share-hash share-hash}]]
            [:dispatch [:schnaq/select-current new-schnaq]]
            [:dispatch [:notification/add
                        #:notification{:title (labels :schnaq/created-success-heading)
                                       :body (labels :schnaq/created-success-subheading)
                                       :context :success}]]
            [:localstorage/assoc [:schnaq.last-added/share-hash share-hash]]
            [:localstorage/assoc [:schnaq.last-added/edit-hash edit-hash]]
            [:localstorage/assoc [:discussion.schnaqs/creation-secrets updated-secrets]]
            [:dispatch [:schnaqs.save-admin-access/to-localstorage-and-db share-hash edit-hash]]]})))
