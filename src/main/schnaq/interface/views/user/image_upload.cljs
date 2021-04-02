(ns schnaq.interface.views.user.image-upload
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]))

(defn store-temporary-profile-picture
  "Store image file from event to database."
  [event]
  (when-not (= "" (oget event [:target :value]))
    (let [^js/File file (first (oget event [:target :files]))]
      (rf/dispatch [:user.profile.picture/store file]))))

(rf/reg-event-db
  :user.profile.picture/store
  (fn [db [_ picture-file]]
    (let [file-reader (js/FileReader.)
          _ (.readAsDataURL file-reader picture-file)]
      (.addEventListener
        file-reader "load"
        (fn []
          (let [picture-data-url (oget file-reader :result)]
            (.log js/console picture-data-url)
            (assoc-in db [:user :profile-picture :temporary]
                      picture-data-url)))))))