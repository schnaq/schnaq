(ns schnaq.interface.utils.localstorage
  (:require [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]))

(rf/reg-fx
 ;; Associates a value into local-storage. Can be retrieved as EDN via get or get-in.
 :localstorage/assoc
 (fn [[key value]]
   (assoc! local-storage key value)))

(rf/reg-fx
 :localstorage/dissoc
 (fn [key]
   (dissoc! local-storage key)))
