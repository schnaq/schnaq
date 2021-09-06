(ns schnaq.interface.views.startpage.alternatives.e-learning
  (:require [reitit.frontend.easy :as rfe]
            [schnaq.interface.text.display-data :refer [labels img-path]]
            [schnaq.interface.utils.rows :as rows]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.startpage.core :as startpage]))

(def moving-heading
  [:header.ms-header.pb-2
   [:h1.ms-header__title "Vertiefender Austausch für deine "
    [:div.ms-slider
     [:ul.ms-slider__words
      [:li.ms-slider__word "Student:innen"]
      [:li.ms-slider__word "Schüler:innen"]
      [:li.ms-slider__word "Kursteilnehmer:innen"]
      [:li.ms-slider__word "Lernenden"]
      ;; The last one needs to duplicate the first for a smooth transition
      [:li.ms-slider__word "Student:innen"]]]]])

(defn- startpage-content []
  [pages/with-nav-and-header
   {:page/title "Vertiefender Austausch für deine Lernenden"
    :page/vertical-header? true
    :page/wrapper-classes "container container-85 mx-auto"
    :page/more-for-heading
    [:div.row.pb-5
     [:div.col-6
      moving-heading
      [:p.display-6.pb-5 "Die App, die deinen Lernenden hilft online strukturiert Lehrinhalte zu diskutieren."]
      [:div.d-flex
       [:img]]
      [:div.text-center
       [:a.btn.btn-lg.btn-secondary.d-inline-block
        {:href (rfe/href :routes.schnaq/create)}
        "Gestalte einen Raum für deine Lernenden"]
       [:p.small.pt-1 "100 % anonym und kostenfrei"]]]
     [:div.col-6
      [:img.img-fluid.above-the-fold-screenshot
       {:src (img-path :startpage.alternatives.e-learning/header)
        :alt "Eine Studentin nutzt schnaq auf ihrem Notebook"}]]]}
   [:<>
    [:section.container
     [rows/row-builder-text-left
      [:img {:src (img-path :alphazulu/logo)}]
      [:div "bla"]]]
    [:section.container
     [startpage/supporters]]]])

(defn e-learning-view []
  [startpage-content])
