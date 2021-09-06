(ns schnaq.interface.views.startpage.alternatives.e-learning
  (:require [schnaq.interface.text.display-data :refer [labels img-path]]
            [schnaq.interface.utils.rows :as rows]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.startpage.core :as startpage]))

(defn- startpage-content []
  [pages/with-nav-and-header
   {:page/title "Vertiefender Austausch für deine Lernenden"
    :page/vertical-header? true
    :page/more-for-heading
    [:header.ms-header
     [:h1.ms-header__title "Vertiefender Austausch für deine "
      [:div.ms-slider
       [:ul.ms-slider__words
        [:li.ms-slider__word "Student:innen"]
        [:li.ms-slider__word "Schüler:innen"]
        [:li.ms-slider__word "Kursteilnehmer:innen"]
        [:li.ms-slider__word "Lernenden"]
        ;; The last one needs to duplicate the first for a smooth transition
        [:li.ms-slider__word "Student:innen"]]]]]}
   [:<>
    [:section.container
     [rows/row-builder-text-left
      [:img {:src (img-path :alphazulu/logo)}]
      [:div "bla"]]]
    [:section.container
     [startpage/supporters]]]])

(defn e-learning-view []
  [startpage-content])
