(ns schnaq.interface.views.user
  (:require [ajax.core :as ajax]
            [clojure.string :as clj-string]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels]]
            [re-frame.core :as rf]))

(rf/reg-event-fx
  :user/set-display-name
  (fn [{:keys [db]} [_ username]]
    ;; only update when string contains
    (when (not (clj-string/blank? username))
      (let [fx {:http-xhrio {:method :post
                             :uri (str (:rest-backend config) "/author/add")
                             :params {:nickname username}
                             :format (ajax/transit-request-format)
                             :response-format (ajax/transit-response-format)
                             :on-success [:user/hide-display-name-input]
                             :on-failure [:ajax-failure]}
                :db (assoc-in db [:user :name] username)}]
        (if (= "Anonymous" username)
          fx
          (assoc fx :write-localstorage [:username username]))))))

(rf/reg-sub
  :user/display-name
  (fn [db]
    (get-in db [:user :name] "Anonymous")))

(rf/reg-sub
  :user/show-display-name-input?
  (fn [db]
    (get-in db [:controls :username-input :show?] true)))

(rf/reg-event-fx
  :user/hide-display-name-input
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:controls :username-input :show?] false)
     :dispatch [:notification/add
                #:notification{:title (labels :user.button/set-name)
                               :body (labels :user.button/success-body)
                               :context :success}]}))

(rf/reg-event-db
  :user/show-display-name-input
  (fn [db _]
    (assoc-in db [:controls :username-input :show?] true)))