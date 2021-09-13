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
     [:div.col-md-6.col-12.align-self-center
      moving-heading
      [:p.display-6.pb-5
       "Die App, die deinen Lernenden hilft, online strukturiert Lehrinhalte zu diskutieren.
        Direkt integrierbar in dein Lernmanagementsystem (ILIAS, Moodle, ...)!"]
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
       [:p.small.my-auto "Mit tausenden Lernenden getestet!"]]]
     [:div.col-md-6.col-12.col-lg-5.offset-lg-1.pt-sm-5.text-center
      [:img.img-fluid.w-75
       {:src (img-path :startpage.alternatives.e-learning/header)
        :alt "Eine Studentin nutzt schnaq auf ihrem Notebook"}]]]}
   [:<>
    [:section.container.my-5
     [rows/row-builder-text-left
      [:article.feature-text-box
       [:h3.h1.text-purple.mb-3 "Minimaler Aufwand, maximale Aktivierung"]
       [:p "Schnaqs starten ist so einfach wie: Titel wählen und Link verteilen.
       Schnell, sicher und datenschutzkonform nach deutschem Recht."]]
      [:img.align-self-center {:src (img-path :startpage.example/discussion)
                               :alt "Eine Beispieldiskussion innerhalb eines schnaqs"}]]]
    [:section.container.mb-5
     [rows/row-builder-text-right
      [:img {:src (img-path :startpage.alternatives.e-learning/student-smartphone)}]
      [:article.feature-text-box
       [:h3.h1.text-purple.mb-3 "Kein Notebook? Kein Problem!"]
       [:p "Um schnaq zu benutzen, braucht es keine Installation. Alles läuft über das Web.
       Kompatibel mit allen Smartphones, Tablets und Computern."]]]]
    [:section.container.mb-5
     [rows/row-builder-text-left
      [:article.feature-text-box
       [:h3.h1.text-purple.mb-3 "Verstehen wo es hakt"]
       [:p "Verschaffe dir einen schnellen Überblick über das Diskutierte. Vollziehe einfach nach worüber deine Lernenden reden. Oder schaue dir die verschiedenen K.I. Auswertungen der Diskussion an."]]
      [:img.align-self-center {:src (img-path :startpage.example/dashboard)
                               :alt "Eine Beispieldiskussion innerhalb eines schnaqs"}]]]
    [:section.container.mb-5
     [rows/row-builder-text-right
      [:img {:src (img-path :startpage.alternatives.e-learning/oma)}]
      [:article.feature-text-box
       [:h3.h1.text-purple.mb-3 "So einfach, selbst ein Seniorenkurs nutzt es!"]
       [:p "Schnaq kann von allen bedient werden. Egal ob du Erfahrung mit Software hast, oder dich gerade erst damit anfreundest. Kommen mal Fragen auf? Kontaktiere den Support jederzeit."]]]]
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
        [:h2.mt-5 "Steigere den Lernerfolg deiner hybriden Lehrveranstaltung"]
        [:p.small.text-muted "\"Ich hätte niemals gedacht, dass Diskussionen als Hausarbeit klappen.\" – David Hanio"]
        [:a.btn.btn-lg.btn-secondary.mt-4
         {:href (rfe/href :routes.schnaq/create)}
         "Upgrade deine Lehrveranstaltung mit nur einem Schritt"]]]]
     [:div.wave-bottom-white-inverted]]
    [:section.container.pt-3
     [startpage/supporters]]]])

(defn e-learning-view []
  [startpage-content])
