(ns meetly.interface.views.errors
  (:require [meetly.interface.views.base :as base]
            [meetly.interface.text.display-data :refer [labels img-path]]
            [re-frame.core :as rf]))


(defn- educate-element []
  [:div
   [:div.single-image-span-container.pb-4 [:img {:src (img-path :elephant-stop)}]]
   [:div
    [:h1 (labels :errors/comic-relief)]
    [:h4 (labels :errors/insufficient-access-rights)]]])



(defn invalid-admin-link-view
  "Shall tell the user they have no rights to view the content, they are trying to access."
  []
  [:div.text-center
   [base/nav-header]
   [:div.container.px-5.py-3
    [:div.pb-4
     [educate-element]]
    [:button.btn.button-primary.btn-lg.center-block
     {:role "button"
      :on-click #(rf/dispatch [:navigate :routes/startpage])}
     (labels :errors/navgate-to-startpage)]]])
