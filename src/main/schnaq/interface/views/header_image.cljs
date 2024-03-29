(ns schnaq.interface.views.header-image
  (:require [oops.core :refer [oget oget+]]
            [re-frame.core :as rf]
            [schnaq.interface.config :as config]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.links :as links]))

(def ^:private image-form-name "image-url")

(defn check-for-header-img
  "Check if url is set and if not return path to placeholder image"
  [url]
  (or url config/place-holder-header-img))

(defn image-url-input []
  (let [input-id "admin-image-url"
        pro-user? @(rf/subscribe [:user/pro?])]
    [:form.form.text-start.mb-5
     {:on-submit (fn [e]
                   (.preventDefault e)
                   (rf/dispatch [:schnaq.admin/set-header-image-url
                                 (oget e [:target :elements])]))}
     [:div.mb-2
      [:label.form-label.h4 {:for input-id} (labels :schnaq.header-image.url/label)]
      [:input.form-control.m-1.rounded-3
       {:id input-id
        :name image-form-name
        :auto-complete "off"
        :required true
        :disabled (not pro-user?)
        :placeholder (labels :schnaq.header-image.url/placeholder)}]
      [:small.form-text.text-muted.float-end
       (labels :schnaq.header-image.url/note)]]
     [:button.btn.btn-outline-primary
      {:disabled (not pro-user?)}
      (labels :schnaq.header-image.url/button)]]))

;; events

(rf/reg-event-fx
 :schnaq.admin/set-header-image-url
 (fn [{:keys [db]} [_ form]]
   (let [current-route (:current-route db)
         {:keys [share-hash]} (:path-params current-route)]
     {:fx [(http/xhrio-request db :post "/discussion/header-image"
                               [:schnaq.admin/set-header-image-url-success form]
                               {:image-url (oget+ form [image-form-name :value])
                                :share-hash share-hash
                                :admin-center (links/get-moderator-center-link share-hash)}
                               [:ajax.error/as-notification])]})))

(rf/reg-event-fx
 :schnaq.admin/set-header-image-url-success
 (fn [_ [_ form {:keys [error]}]]
   {:fx
    (if error
      ;; when error occurred display a warning and do not clear form
      [[:dispatch [:notification/add
                   #:notification{:title (labels :schnaq.header-image.url/failed-setting-title)
                                  :body [:<>
                                         (labels :schnaq.header-image.url/failed-setting-body)
                                         [:span error]]
                                  :context :danger
                                  :stay-visible? true}]]]
      ;; when no error occurred clear form
      [[:dispatch [:notification/add
                   #:notification{:title (labels :schnaq.header-image.url/successful-set)
                                  :body (labels :schnaq.header-image.url/successful-set-body)
                                  :context :success}]]
       [:form/clear form]])}))
