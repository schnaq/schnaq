(ns meetly.interface.views.errors
  (:require [meetly.interface.views.base :as base]
            [meetly.interface.text.display-data :refer [labels]]))


(defn invalid-admin-link-view
  "Shall tell the user they have no rights to view the content, they are trying to access."
  []
  [:div
   [base/nav-header]
   [:div.container.px-5.py-3
    [:p.text-center (labels :errors/invalid-admin-link)]]])