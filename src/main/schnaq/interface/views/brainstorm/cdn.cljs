(ns schnaq.interface.views.brainstorm.cdn
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [ajax.core :as ajax]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.config :as config]))

(def ^:private image-form-name "image-url")

(defn image-url-input []
  (let [input-id "admin-image-url"]
    [:form.form.text-left.mb-5
     {:on-submit (fn [e]
                   (js-wrap/prevent-default e)
                   (rf/dispatch [:schnaq.admin/set-preview-image-url
                                 (oget e [:target :elements])]))}
     [:div.form-group
      [:label {:for input-id} (labels :schnaq.preview-image-url/label)]
      [:input.form-control.m-1.input-rounded
       {:id input-id
        :name image-form-name
        :auto-complete "off"
        :required true
        :placeholder (labels :schnaq.preview-image-url/placeholder)}]
      [:small.form-text.text-muted.float-right
       (labels :schnaq.preview-image-url/note)]]
     [:button.btn.btn-outline-primary
      (labels :schnaq.preview-image-url/button)]]))

;; subs

(rf/reg-event-fx
  :schnaq.admin/set-preview-image-url
  (fn [{:keys [db]} [_ form]]
    (let [current-route (:current-route db)
          {:keys [share-hash edit-hash]} (:path-params current-route)]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config/config) "/preview-image/image")
                          :format (ajax/transit-request-format)
                          :params {:image-url (oget form [image-form-name :value])
                                   :share-hash share-hash
                                   :edit-hash edit-hash
                                   :admin-center (common/get-admin-center-link current-route)}
                          :response-format (ajax/transit-response-format)
                          :on-success [:schnaq.admin/set-preview-image-url-success form]
                          :on-failure [:ajax.error/as-notification]}]]})))

(rf/reg-event-fx
  :schnaq.admin/set-preview-image-url-success
  (fn [_ [_ form {:keys [error]}]]
    {:fx [[:dispatch [:notification/add
                      #:notification{:title (labels :schnaq.preview-image-url/successful-set)
                                     :body (labels :schnaq.preview-image-url/successful-set-body)
                                     :context :success}]]
          [:form/clear form]
          (when error
            [:dispatch [:notification/add
                        #:notification{:title (labels :schnaq.preview-image-url/failed-setting-title)
                                       :body [:<>
                                              (labels :schnaq.preview-image-url/failed-setting-body)
                                              [:span error]]
                                       :context :warning
                                       :stay-visible? true}]])]}))