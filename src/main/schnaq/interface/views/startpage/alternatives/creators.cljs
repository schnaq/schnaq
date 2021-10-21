(ns schnaq.interface.views.startpage.alternatives.creators
  (:require [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.components.wavy :as wavy]
            [schnaq.interface.utils.rows :as rows]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.startpage.core :as startpage]))

(defn- startpage-content []
  [pages/with-nav-and-header
   {:page/title "Dein Community-Zuhause mit einem Klick"
    :page/vertical-header? true
    :page/wrapper-classes "container container-85 mx-auto"
    :page/more-for-heading
    [:div.row.pb-5
     [:div.col-md-6.col-12.col-lg-6.pt-sm-5.text-center
      [:img.img-fluid.rounded
       {:src "https://s3.disqtec.com/schnaq-common/startpage/screenshots/splashpage_creator.jpeg"
        :alt "Ein Consultant nutzt schnaq auf einem Notebook"}]]
     [:div.col-md-6.col-12.align-self-center.text-center
      "Lasse deine Community miteinander und mit dir diskutieren"
      [:p.display-6.pb-5
       "Aktiviere deine Community"]
      [:div.text-center.pb-5
       [:a.btn.btn-lg.btn-secondary.d-inline-block
        {:href "https://schnaq.outgrow.us/creator"}
        "Umfrage starten"]]
      [:div.d-flex
       [:img.rounded-circle.social-proof-img.mr-2 {:src (img-path :testimonial-picture/bjorn)}]
       [:img.rounded-circle.social-proof-img.mr-2 {:src (img-path :testimonial-picture/raphael-bialon)}]
       [:img.rounded-circle.social-proof-img.mr-2 {:src (img-path :testimonial-picture/florian-clever)}]
       [:div.border-right.mr-2
        [:img.rounded-circle.social-proof-img.mr-2 {:src (img-path :testimonial-picture/frank-stampa)}]
        [icon :plus "my-auto mr-2"]]
       [:p.small.my-auto "Lerne, was deine Community bewegt!"]]]]}
   [:<>
    [:section.container.my-5
     [rows/row-builder-text-left
      [:article.feature-text-box
       [:h3.h1.text-purple.mb-3 "Minimaler Aufwand, maximale Aktivierung"]
       [:p "Gib deiner Community mit wenigen Klicks ihren eigenen Space zum Diskutieren, oder frage sie direkt zu Dingen, die dich interessieren."]]
      [motion/zoom-image {:src "https://s3.disqtec.com/schnaq-common/startpage/screenshots/podcast_discussion.png"
                          :alt "Eine Beispieldiskussion innerhalb eines schnaqs"}]]]
    [:section.container.mb-5
     [rows/row-builder-text-right
      [:img {:src (img-path :startpage.alternatives.e-learning/student-smartphone)}]
      [:article.feature-text-box
       [:h3.h1.text-purple.mb-3 "Nimm deine Community auch unterwegs mit!"]
       [:p "Um schnaq zu benutzen, braucht es nur Internet.
       Kompatibel mit allen Smartphones, Tablets und Computern."]]]]
    [:section.container.mb-5
     [rows/row-builder-text-left
      [:article.feature-text-box
       [:h3.h1.text-purple.mb-3 "Was passiert in deiner Community?"]
       [:p "Verschaffe dir einen schnellen Überblick über das Diskutierte. Vollziehe einfach nach worüber deine Community redet. Oder schaue dir die verschiedenen K.I. Auswertungen der Diskussion an."]]
      [motion/zoom-image {:src (img-path :startpage.example/dashboard)
                          :alt "Eine Beispieldiskussion innerhalb eines schnaqs"}]]]
    [:section.container.mb-5
     [rows/row-builder-text-right
      [:img {:src (img-path :startpage.alternatives.e-learning/oma)}]
      [:article.feature-text-box
       [:h3.h1.text-purple.mb-3 "So easy, selbst der eine Rentner #1 Fan schafft das!"]
       [:p "Jeder aus deiner Community schafft es schnaq zu nutzen. Garantiert! Kommen mal Fragen auf? Kontaktiere den Support jederzeit."]]]]
    [:section.overflow-hidden.py-3.my-5
     [wavy/top-and-bottom
      :white
      [:div.container-lg.text-center
       [:section.container.text-center
        (let [img-classes "rounded-circle social-proof-img-lg mr-5 mt-3"]
          [:div.mx-auto {:style {:max-width "900px"}}
           [:img {:class img-classes
                  :src (img-path :testimonial-picture/bjorn)}]
           [:img {:class img-classes
                  :src (img-path :testimonial-picture/raphael-bialon)}]
           [:img {:class img-classes
                  :src (img-path :testimonial-picture/eugen-bialon)}]
           [:img {:class img-classes
                  :src (img-path :testimonial-picture/frauke-kling)}]
           [:img {:class img-classes
                  :src (img-path :startpage.alternatives.e-learning/mike)}]
           [:img {:class img-classes
                  :src (img-path :testimonial-picture/florian-clever)}]
           [:img {:class img-classes
                  :src (img-path :startpage.alternatives.e-learning/david)}]
           [:img {:class img-classes
                  :src (img-path :testimonial-picture/frank-stampa)}]
           [:img {:class img-classes
                  :src (img-path :testimonial-picture/hck)}]
           [:img {:class img-classes
                  :src (img-path :testimonial-picture/ingo-kupers)}]
           [:img {:class img-classes
                  :src (img-path :startpage.alternatives.e-learning/alex)}]
           [:img {:class img-classes
                  :src (img-path :testimonial-picture/lokay)}]
           [:img {:class img-classes
                  :src (img-path :testimonial-picture/meiko-tse)}]
           [:img {:class img-classes
                  :src (img-path :startpage.alternatives.e-learning/christian)}]
           [:img {:class img-classes
                  :src (img-path :testimonial-picture/tobias-schroeder)}]])
        [:h2.mt-5 "Versteh sofort was in deiner Community abgeht!"]
        [:p.small.text-muted "\"Ich hätte nicht gedacht, dass so viele Leute jeden Tag über meinen Podcast reden. Jetzt bekomme ich es auch mal mit.\" – Johann 'Quikz' Mollman"]
        [:a.btn.btn-lg.btn-secondary.mt-4
         {:href "https://schnaq.outgrow.us/creator"}
         "Umfrage starten"]]]]]
    [:section.container.pt-3
     [startpage/supporters]]]])

(defn creator-view []
  [startpage-content])
