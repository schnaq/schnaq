(ns schnaq.interface.views.discussion.common
  (:require [re-frame.core :as rf]))

(defn navigate-to-statement-on-click [statement path-parameters]
  (fn [_e]
    (rf/dispatch [:discussion.select/conclusion statement])
    (rf/dispatch [:discussion.history/push statement])
    (rf/dispatch [:navigation/navigate :routes.schnaq.select/statement
                  (assoc path-parameters :statement-id (:db/id statement))])))
