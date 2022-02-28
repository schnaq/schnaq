(ns schnaq.interface.utils.images
  (:require [clojure.string :as str]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.config :as config]
            [schnaq.interface.translations :refer [labels]]))

(defn store-temporary-image
  "Store image file from event to database in the specified path.
  e.g.: [:user :profile-picture :temporary]"
  [event db-path]
  (when-not (empty? (oget event [:target :value]))
    (let [^js/File file (first (oget event [:target :files]))]
      (rf/dispatch [:image/store file db-path]))))

(rf/reg-event-fx
 :image/store
 (fn [_ [_ file db-path]]
   {:fx [[:readfile {:files [file]
                     :on-success [:image.store/success db-path]
                     :on-error [:ajax.error/to-console]}]]}))

(rf/reg-event-fx
 :image.store/success
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

(rf/reg-event-fx
 :image.store/error
 (fn [{:keys [db]} [_ {:keys [response]}]]
   (let [mime-types (str/join ", " shared-config/allowed-mime-types)
         error-message (case (:error response)
                         :scaling (labels :user.settings.profile-picture.errors/scaling)
                         :invalid-file-type (gstring/format (labels :user.settings.profile-picture.errors/invalid-file-type) mime-types)
                         (labels :user.settings.profile-picture.errors/default))]
     {:db (assoc-in db [:user :profile-picture :temporary] nil)
      :fx [[:dispatch [:notification/add
                       #:notification{:title (labels :user.settings.profile-picture-title/error)
                                      :body error-message
                                      :context :danger}]]]})))
