(ns schnaq.interface.views.user.image-upload
  (:require [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :as config]
            [schnaq.interface.text.display-data :refer [labels]]))

(defn store-temporary-profile-picture
  "Store image file from event to database in the specified path.
  e.g.: [:user :profile-picture :temporary]"
  [event db-path]
  (when-not (= "" (oget event [:target :value]))
    (let [^js/File file (first (oget event [:target :files]))]
      (rf/dispatch [:user.profile.picture/store file db-path]))))

(rf/reg-event-fx
  :user.profile.picture/store
  (fn [_ [_ picture-file db-path]]
    {:fx [[:readfile {:files [picture-file]
                      :on-success [:picture-read-file-success db-path]
                      :on-error [:ajax.error/to-console]}]]}))

(rf/reg-event-fx
  :picture-read-file-success
  (fn [{:keys [db]} [_ db-path files]]
    (let [profile-picture (first files)
          actual-size (:size profile-picture)
          size-valid? (< actual-size config/max-allowed-profile-picture-size)]
      (if size-valid?
        {:db (assoc-in db db-path profile-picture)}
        {:fx [[:dispatch
               [:notification/add
                #:notification{:title (labels :user.settings.profile-picture-title/error)
                               :body (gstring/format (labels :user.settings.profile-picture-too-large/error)
                                                     actual-size config/max-allowed-profile-picture-size)
                               :context :danger}]]]}))))

(rf/reg-sub
  :user.profile.picture/temporary
  (fn [db _] (get-in db [:user :profile-picture :temporary])))