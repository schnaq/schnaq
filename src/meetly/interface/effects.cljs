(ns meetly.interface.effects
  (:require [re-frame.core :as rf]
            [meetly.interface.utils.localstorage :as ls]
            [meetly.interface.utils.toolbelt :as toolbelt]))

(rf/reg-fx
  :write-localstorage
  (fn [[key value]]
    (ls/set-item! key value)))

(rf/reg-fx
  :form/clear
  (fn [form-elements]
    (toolbelt/reset-form-fields! form-elements)))