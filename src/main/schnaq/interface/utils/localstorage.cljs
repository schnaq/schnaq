(ns schnaq.interface.utils.localstorage
  (:require [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]))

(defn from-localstorage
  "Load specified key from localstorage, if localstorage is available."
  [key]
  (when local-storage
    (get local-storage key)))

;; -----------------------------------------------------------------------------

(rf/reg-fx
 ;; Associates a value into local-storage. Can be retrieved as EDN via get or get-in.
 :localstorage/assoc
 (fn [[key value]]
   (when local-storage
     (assoc! local-storage key value))))

(rf/reg-fx
 :localstorage/dissoc
 (fn [key]
   (when local-storage
     (dissoc! local-storage key))))
