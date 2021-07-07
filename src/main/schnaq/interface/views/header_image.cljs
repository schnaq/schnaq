(ns schnaq.interface.views.header-image
  (:require [oops.core :refer [oget oget+]]
            [re-frame.core :as rf]
            [schnaq.interface.config :as config]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.links :as links]))

(def ^:private image-form-name "image-url")

(defn check-for-header-img
  "Check if url is set and if not return path to placeholder image"
  [url]
  (if url
    url
    config/place-holder-header-img))

(defn image-url-input []
  (let [input-id "admin-image-url"]
    [:form.form.text-left.mb-5
     {:on-submit (fn [e]
                   (js-wrap/prevent-default e)
                   (rf/dispatch [:schnaq.admin/set-header-image-url
                                 (oget e [:target :elements])]))}
     [:div.form-group
      [:label {:for input-id} (labels :schnaq.header-image.url/label)]
      [:input.form-control.m-1.rounded-3
       {:id input-id
        :name image-form-name
        :auto-complete "off"
        :required true
        :placeholder (labels :schnaq.header-image.url/placeholder)}]
      [:small.form-text.text-muted.float-right
       (labels :schnaq.header-image.url/note)]]
     [:button.btn.btn-outline-primary
      (labels :schnaq.header-image.url/button)]]))

;; events

(rf/reg-event-fx
  :schnaq.admin/set-header-image-url
  (fn [{:keys [db]} [_ form]]
    (let [current-route (:current-route db)
          {:keys [share-hash edit-hash]} (:path-params current-route)]
      {:fx [(http/xhrio-request db :post "/discussion/header-image"
                                [:schnaq.admin/set-header-image-url-success form]
                                {:image-url (oget+ form [image-form-name :value])
                                 :share-hash share-hash
                                 :edit-hash edit-hash
                                 :admin-center (links/get-admin-link share-hash edit-hash)}
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