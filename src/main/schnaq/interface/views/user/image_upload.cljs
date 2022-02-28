(ns schnaq.interface.views.user.image-upload
  (:require [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :as config]
            [schnaq.interface.translations :refer [labels]]))

(defn store-temporary-image
  "Store image file from event to database in the specified path.
  e.g.: [:user :profile-picture :temporary]"
  [event db-path]
  (when-not (empty? (oget event [:target :value]))
    (let [^js/File file (first (oget event [:target :files]))]
      (rf/dispatch [:file/store file db-path]))))

(rf/reg-event-fx
 :file/store
 (fn [_ [_ picture-file db-path]]
   {:fx [[:readfile {:files [picture-file]
                     :on-success [:file.store/success db-path]
                     :on-error [:ajax.error/to-console]}]]}))

(rf/reg-event-fx
 :file.store/success
 (fn [{:keys [db]} [_ db-path files]]
   (let [image (first files)
         actual-size (:size image)
         size-valid? (< actual-size config/max-allowed-profile-picture-size)]
     (if size-valid?
       {:db (assoc-in db db-path image)}
       {:fx [[:dispatch
              [:notification/add
               #:notification{:title (labels :user.settings.profile-picture-title/error)
                              :body (gstring/format (labels :user.settings.profile-picture-too-large/error)
                                                    actual-size config/max-allowed-profile-picture-size)
                              :context :danger}]]]}))))
