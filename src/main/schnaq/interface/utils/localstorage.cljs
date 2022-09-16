(ns schnaq.interface.utils.localstorage
  (:require [hodgepodge.core :refer [local-storage]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]))

(defn- localstorage-available?
  "Check that the localstorage is available."
  []
  (try
    (oget js/window :localStorage)
    true
    (catch js/Object _e false)))

(defn get-value [key]
  (when (localstorage-available?)
    (get key local-storage)))

;; -----------------------------------------------------------------------------

(rf/reg-fx
 ;; Associates a value into local-storage. Can be retrieved as EDN via get or get-in.
 :localstorage/assoc
 (fn [[key value]]
   (when (localstorage-available?)
     (assoc! local-storage key value))))

(rf/reg-fx
 :localstorage/dissoc
 (fn [key]
   (when (localstorage-available?)
     (dissoc! local-storage key))))

;; WIP vllt unnötig
(rf/reg-event-db
 :localstorage/availability
 (fn [db]
   (assoc-in db [:localstorage :available?] (localstorage-available?))))
