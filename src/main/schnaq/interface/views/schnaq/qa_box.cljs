(ns schnaq.interface.views.schnaq.qa-box
  (:require [com.fulcrologic.guardrails.core :refer [=> >defn-]]
            [re-frame.core :as rf]
            [schnaq.database.specs :as specs]))

(rf/reg-sub
 :qa-boxes
 (fn [db _]
   (get-in db [:schnaq :selected :discussion/qa-boxes])))

(rf/reg-sub
 :qa-box
 (fn [db [_ qa-box-id]]
   (->>
    (get-in db [:schnaq :selected :discussion/qa-boxes])
    (filter #(= (:db/id %) qa-box-id))
    first)))

(>defn- qa-box-card
  "Show a qa box card, where users can ask questions of the presenter."
  [qa-box]
  [::specs/poll => :re-frame/component]
  [:section.statement-card
   [:div.mx-4.my-2
    [:div.d-flex
     [:h6.pb-2.text-center.mx-auto (:qa-box/label qa-box)]
     [:p "TODO dropdown menu"]]
    [:h3 "Any content"]]])