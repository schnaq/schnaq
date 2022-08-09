(ns schnaq.interface.utils.files
  (:require [clojure.string :as str]
            [goog.string :refer [format]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.config :as config]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [taoensso.timbre :as log]))

(defn store-temporary-file
  "Store image file from event to database in the specified path.
  e.g.: [:user :profile-picture :temporary]"
  [event db-path]
  (when-not (empty? (oget event [:target :value]))
    (let [^js/File file (first (oget event [:target :files]))]
      (rf/dispatch [:file/store file db-path]))))

(rf/reg-event-fx
 :file/store
 (fn [_ [_ file db-path]]
   {:fx [[:readfile {:files [file]
                     :on-success [:file.store/success db-path]
                     :on-error [:ajax.error/to-console]}]]}))

(rf/reg-event-fx
 :file.store/success
 (fn [{:keys [db]} [_ db-path files]]
   (let [file (first files)
         actual-size (-> (:size file) (/ 1024) (/ 1024))
         size-valid? (< actual-size config/max-allowed-file-size)]
     (if size-valid?
       {:db (assoc-in db db-path file)}
       {:fx [[:dispatch
              [:notification/add
               #:notification{:title (labels :file.store.error/title)
                              :body (format (labels :file.store.error/file-too-large)
                                            actual-size config/max-allowed-file-size)
                              :stay-visible? true
                              :context :danger}]]]}))))

(rf/reg-event-fx
 :file.store/error
 (fn [_ [_ {:keys [response]}]]
   (let [mime-types (str/join ", " shared-config/allowed-mime-types-images)
         error-message (case (:error response)
                         :image.error/scaling (labels :file.store.error/scaling-problem)
                         :image.error/invalid-file-type (format (labels :file.store.error/invalid-file-type) mime-types)
                         (labels :file.store.error/generic))]
     {:fx [[:dispatch [:notification/add
                       #:notification{:title (labels :file.store.error/title)
                                      :body error-message
                                      :context :danger}]]]})))

(rf/reg-event-fx
 :file/upload
 (fn [{:keys [db]} [_ share-hash file bucket success-event error-event]]
   (if (and share-hash file bucket)
     {:fx [(http/xhrio-request db :put "/discussion/upload/file"
                               success-event
                               {:share-hash share-hash
                                :file file
                                :bucket bucket}
                               error-event)]}
     (log/error (format "Some properties are missing. share-hash: %s, bucket: %s, file: %s" share-hash bucket file)))))
