(ns schnaq.interface.effects
  (:require [re-frame.core :as rf]
            [schnaq.interface.utils.toolbelt :as toolbelt]))

(rf/reg-fx
  :form/clear
  (fn [form-elements]
    (toolbelt/reset-form-fields! form-elements)))