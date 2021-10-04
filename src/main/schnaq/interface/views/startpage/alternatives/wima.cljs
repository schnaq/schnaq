(ns schnaq.interface.views.startpage.alternatives.wima
  (:require [schnaq.interface.components.icons :refer [fa]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.rows :as rows]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.startpage.core :as startpage]))

(defn- startpage-content []
  [:div.overflow-hidden
   [pages/with-nav-and-header
    {:page/title "Vertiefender Austausch mit deinen Kunden"
     :page/vertical-header? true
     :page/wrapper-classes "container container-85"
     :page/more-for-heading
     [:section.row.mt-3 {:key "HeaderExtras-Bullet-Points-and-Animation"}
      [:div.col-lg-5.py-lg-4.pr-lg-5.my-auto
       [:h1 (labels :startpage/subheading)]
       [:section.mt-5.text-center
        [:a.btn.btn-lg.btn-secondary.d-inline-block
         {:href "https://schnaq.outgrow.us/WiMa"}
         "Umfrage starten "
         [:i.m-auto {:class (str "fa " (fa :arrow-right))}]]]
       [:p.text-social-proof.text-center.pt-2
        [:img.social-proof-icon
         {:src (img-path :schnaqqifant/white)}]
        (labels :startpage.social-proof/numbers)]]
      [:div.col-lg-7.py-lg-4
       [:section.above-the-fold-screenshot
        [:img.taskbar-background.mb-2 {:src (img-path :how-to/taskbar)}]
        [:img.img-fluid {:src (img-path :startpage.example/discussion)}]]]]}
    [:<>
     [:section.container.my-5
      [rows/row-builder-text-left
       [:article.feature-text-box
        [:h3.h1.text-purple.mb-3 "Nie wieder Wissen suchen"]
        [:p "Durch K.I. gestütztes Q&A und eine Wissensdatenbank können deine Mitarbeiter:innen immer effizient finden, was sie suchen."]]
       [motion/zoom-image {:src (img-path :startpage.example/statements)
                           :alt "Eine Beispieldiskussion innerhalb eines schnaqs"}]]]
     [:section.container.mb-5
      [rows/row-builder-text-right
       [:img {:src (img-path :startpage.alternatives.e-learning/student-smartphone)}]
       [:article.feature-text-box
        [:h3.h1.text-purple.mb-3 "Auch unterwegs bei Kunden nutzbar"]
        [:p "Um schnaq zu benutzen, braucht es nur Internet.
       Kompatibel mit allen Smartphones, Tablets und Computern."]]]]
     [:section.container.mb-5
      [rows/row-builder-text-left
       [:article.feature-text-box
        [:h3.h1.text-purple.mb-3 "Einfacher Überblick zu jeder Zeit"]
        [:p "Verschaffe dir einen schnellen Überblick über das Diskutierte. Vollziehe einfach nach worüber deine Kolleg:innen reden. Oder schaue dir die verschiedenen K.I. Auswertungen der Diskussion an."]]
       [motion/zoom-image {:src (img-path :startpage.example/dashboard)
                           :alt "Eine Beispieldiskussion innerhalb eines schnaqs"}]]]
     [:section.container.mb-5
      [rows/row-builder-text-right
       [:img {:src (img-path :startpage.alternatives.e-learning/oma)}]
       [:article.feature-text-box
        [:h3.h1.text-purple.mb-3 "Für alle einfach zu bedienen"]
        [:p "Für schnaq braucht es keine technischen Kentnisse! Alle schaffen es schnaq zu bedienen. Kommen mal Fragen auf? Kontaktiere den Support jederzeit."]]]]
     [:section.overflow-hidden.py-3.my-5
      [:div.wave-bottom-white]
      [:div.bg-white
       [:div.container-lg.text-center.early-adopter-schnaqqifant-wrapper
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
         [:h2.mt-5 "Mach es wie viele andere Unternehmen und nutze den Wissensvorsprung deiner Mitarbeiter:innen!"]
         [:p.small.text-muted "\"Schnaq ist Raketenwissenschaft im Backend und einfach wie ein Dreirad im Frontend.\" – Frank Stampa"]
         [:a.btn.btn-lg.btn-secondary.mt-4
          {:href "https://schnaq.outgrow.us/WiMa"}
          "Umfrage starten"]]]]
      [:div.wave-bottom-white-inverted]]
     [:section.container.pt-3
      [startpage/supporters]]]]])

(defn wima-view []
  [startpage-content])
