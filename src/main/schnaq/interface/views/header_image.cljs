(ns schnaq.interface.views.header-image
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [ajax.core :as ajax]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.config :as config]))

(def ^:private image-form-name "image-url")

(defn check-for-header-img
  "Check if url is set and if not return path to placeholder image"
  [url]
  (if url
    url
    config/place-holder-img))

(defn image-url-input []
  (let [input-id "admin-image-url"]
    [:form.form.text-left.mb-5
     {:on-submit (fn [e]
                   (js-wrap/prevent-default e)
                   (rf/dispatch [:schnaq.admin/set-header-image-url
                                 (oget e [:target :elements])]))}
     [:div.form-group
      [:label {:for input-id} (labels :schnaq.header-image.url/label)]
      [:input.form-control.m-1.input-rounded
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
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config/config) "/header-image/image")
                          :format (ajax/transit-request-format)
                          :params {:image-url (oget form [image-form-name :value])
                                   :share-hash share-hash
                                   :edit-hash edit-hash
                                   :admin-center (common/get-admin-center-link current-route)}
                          :response-format (ajax/transit-response-format)
                          :on-success [:schnaq.admin/set-header-image-url-success form]
                          :on-failure [:ajax.error/as-notification]}]]})))

(rf/reg-event-fx
  :schnaq.admin/set-header-image-url-success
  (fn [_ [_ form {:keys [error]}]]
    {:fx
     (if error
       ;; when error occured display a warning and do not clear form
       [[:dispatch [:notification/add
                    #:notification{:title (labels :schnaq.header-image.url/failed-setting-title)
                                   :body [:<>
                                          (labels :schnaq.header-image.url/failed-setting-body)
                                          [:span error]]
                                   :context :danger
                                   :stay-visible? true}]]]
       ;; when no error occured clear form
       [[:dispatch [:notification/add
                    #:notification{:title (labels :schnaq.header-image.url/successful-set)
                                   :body (labels :schnaq.header-image.url/successful-set-body)
                                   :context :success}]]
        [:form/clear form]])}))