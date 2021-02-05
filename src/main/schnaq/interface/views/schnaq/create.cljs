(ns schnaq.interface.views.schnaq.create
  (:require [ajax.core :as ajax]
            [oops.core :refer [oget]]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.pages :as pages]
            [re-frame.core :as rf]))

(defn title-input
  "The input and label for a new schnaq."
  []
  [:<>
   [:input#schnaq-title.form-control.form-title.form-border-bottom.mb-2
    {:type "text"
     :autoComplete "off"
     :required true
     :placeholder (labels :schnaq.create.input/placeholder)}]])

(defn- create-brainstorm []
  ;; todo relabel
  [pages/with-nav-and-header
   {:page/heading (labels :brainstorm/heading)}
   [:div.container
    [:div.py-3.mt-3
     [:form
      {:on-submit (fn [e]
                    (let [title (oget e [:target :elements :schnaq-title :value])
                          public? (oget e [:target :elements :public-discussion? :checked])]
                      (js-wrap/prevent-default e)
                      (rf/dispatch [:schnaq.create/new {:discussion/title title} public?])))}
      [:div.agenda-meeting-container.shadow-straight.p-3
       [title-input]]
      [:div.form-check.pt-2.text-center
       [:input.form-check-input.big-checkbox {:type :checkbox
                                              :id :public-discussion?
                                              :defaultChecked true}]
       [:label.form-check-label.display-6.pl-1 {:for :public-discussion?}
        (labels :discussion.create.public-checkbox/label)]]
      [:div.pt-3.text-center
       [:button.btn.button-primary (labels :brainstorm.create.button/save)]]]]]])

(defn create-brainstorm-view []
  ;; todo relabel
  [create-brainstorm])

(rf/reg-event-fx
  :schnaq.create/new
  (fn [{:keys [db]} [_ new-discussion public?]]
    (let [nickname (get-in db [:user :name] "Anonymous")]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/schnaq/add")
                          :params {:nickname nickname
                                   :discussion new-discussion
                                   :public-discussion? public?}
                          :format (ajax/transit-request-format)
                          :response-format (ajax/transit-response-format)
                          :on-success [:schnaq/created]
                          :on-failure [:ajax.error/as-notification]}]]})))

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
            [:localstorage/write [:schnaq.last-added/share-hash share-hash]]
            [:localstorage/write [:schnaq.last-added/edit-hash edit-hash]]
            [:dispatch [:schnaqs.save-admin-access/to-localstorage share-hash edit-hash]]]})))