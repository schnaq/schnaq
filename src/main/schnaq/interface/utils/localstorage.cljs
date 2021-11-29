(ns schnaq.interface.utils.localstorage
  (:require [clojure.string :as string]
            [ghostwheel.core :refer [>defn]]
            [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]))

(>defn localstorage->map
  "Dump complete content of localstorage into a map. Removes debug data from
  re-frame-10x."
  []
  [:ret map?]
  (into {}
        (remove #(string/starts-with? (str (first %)) "day8.re-frame-10x")
                (persistent! local-storage))))

(rf/reg-fx
 ;; Associates a value into local-storage. Can be retrieved as EDN via get or get-in.
 :localstorage/assoc
 (fn [[key value]]
   (assoc! local-storage key value)))

(rf/reg-fx
 :localstorage/dissoc
 (fn [key]
   (dissoc! local-storage key)))
