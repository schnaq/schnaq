(ns schnaq.interface.effects
  (:require [re-frame.core :as rf]
            [schnaq.interface.utils.localstorage :as ls]
            [schnaq.interface.utils.toolbelt :as toolbelt]))

(rf/reg-fx
  :localstorage/write
  (fn [[key value]]
    (ls/set-item! key value)))

(rf/reg-fx
  :localstorage/remove
  (fn [key]
    (ls/remove-item! key)))

(rf/reg-fx
  :form/clear
  (fn [form-elements]
    (toolbelt/reset-form-fields! form-elements)))