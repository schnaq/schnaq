(ns schnaq.interface.views.startpage.alternatives.e-learning
  (:require [reitit.frontend.easy :as rfe]
            [schnaq.interface.text.display-data :refer [img-path fa]]
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
     [:div.col-6.align-self-center
      moving-heading
      [:p.display-6.pb-5 "Die App, die deinen Lernenden hilft online strukturiert Lehrinhalte zu diskutieren."]
      [:div.text-center.pt-3.pb-5
       [:a.btn.btn-lg.btn-secondary.d-inline-block
        {:href (rfe/href :routes.schnaq/create)}
        "Gestalte einen Raum für deine Lernenden"]
       [:p.small.pt-1 "100 % anonym und kostenfrei"]]
      [:div.d-flex
       [:img.rounded-circle.social-proof-img.mr-2 {:src (img-path :testimonial-picture/bjorn)}]
       [:img.rounded-circle.social-proof-img.mr-2 {:src (img-path :testimonial-picture/raphael-bialon)}]
       [:img.rounded-circle.social-proof-img.mr-2 {:src (img-path :testimonial-picture/florian-clever)}]
       [:div.border-right.mr-2
        [:img.rounded-circle.social-proof-img.mr-2 {:src (img-path :testimonial-picture/frank-stampa)}]
        [:i {:class (str "mr-2 my-auto " (fa :plus))}]]
       [:p.small.my-auto "Mit hunderten Lernenden getestet!"]]]
     [:div.col-6.align-self-center
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
