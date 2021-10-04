(ns schnaq.interface.views.startpage.alternatives.consulting
  (:require [reitit.frontend.easy :as rfe]
            [schnaq.interface.components.icons :refer [fa]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.utils.rows :as rows]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.startpage.core :as startpage]))

(def moving-heading
  [:header.ms-header.pb-2
   [:h1.ms-header__title "Die Plattform für Projektgespräche mit deinen "
    [:div.ms-slider
     [:ul.ms-slider__words
      [:li.ms-slider__word "Kund:innen"]
      [:li.ms-slider__word "Stakeholdern"]
      [:li.ms-slider__word "Kolleg:innen"]
      [:li.ms-slider__word "Partnern"]
      ;; The last one needs to duplicate the first for a smooth transition
      [:li.ms-slider__word "Kund:innen"]]]]])

(defn- startpage-content
  ([]
   [startpage-content (rfe/href :routes.schnaq/create)])
  ([cta-link]
   [pages/with-nav-and-header
    {:page/title "Vertiefender Austausch mit deinen Kunden"
     :page/vertical-header? true
     :page/wrapper-classes "container container-85 mx-auto"
     :page/more-for-heading
     [:div.row.pb-5
      [:div.col-md-6.col-12.align-self-center
       moving-heading
       [:p.display-6.pb-5
        "Lastenheft war gestern. Digitalisiere deine Kund:innenkommunikation mit schnaq."]
       [:div.text-center.pt-3.pb-5
        [:a.btn.btn-lg.btn-secondary.d-inline-block
         {:href cta-link}
         "Digitalisiere jetzt deine Projektabstimmung"]
        [:p.small.pt-1 "100 % anonym und kostenfrei"]]
       [:div.d-flex
        [:img.rounded-circle.social-proof-img.mr-2 {:src (img-path :testimonial-picture/bjorn)}]
        [:img.rounded-circle.social-proof-img.mr-2 {:src (img-path :testimonial-picture/raphael-bialon)}]
        [:img.rounded-circle.social-proof-img.mr-2 {:src (img-path :testimonial-picture/florian-clever)}]
        [:div.border-right.mr-2
         [:img.rounded-circle.social-proof-img.mr-2 {:src (img-path :testimonial-picture/frank-stampa)}]
         [:i {:class (str "mr-2 my-auto " (fa :plus))}]]
        [:p.small.my-auto "von Software-Designer:innen für Software-Designer:innen!"]]]
      [:div.col-md-6.col-12.col-lg-6.pt-sm-5.text-center
       [:img.img-fluid
        {:src "https://s3.disqtec.com/startpage/consulting-small.png"
         :alt "Ein Consultant nutzt schnaq auf einem Notebook"}]]]}
    [:<>
     [:section.container.my-5
      [rows/row-builder-text-left
       [:article.feature-text-box
        [:h3.h1.text-purple.mb-3 "Minimaler Aufwand, maximale Aktivierung"]
        [:p "Schnaqs starten ist so einfach wie: Titel wählen und Link verteilen.
       Schnell, sicher und datenschutzkonform nach deutschem Recht."]]
       [motion/zoom-image {:src (img-path :startpage.example/discussion)
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
        [:p "Verschaffe dir einen schnellen Überblick über das Diskutierte. Vollziehe einfach nach worüber deine Stakeholder reden. Oder schaue dir die verschiedenen K.I. Auswertungen der Diskussion an."]]
       [motion/zoom-image {:src (img-path :startpage.example/dashboard)
                           :alt "Eine Beispieldiskussion innerhalb eines schnaqs"}]]]
     [:section.container.mb-5
      [rows/row-builder-text-right
       [:img {:src (img-path :startpage.alternatives.e-learning/oma)}]
       [:article.feature-text-box
        [:h3.h1.text-purple.mb-3 "So einfach, selbst dein ältester Kunde schafft das!"]
        [:p "Jira zu kompliziert? Dokumente in der Cloud ablegen zu umständlich? Schnaq kann von allen bedient werden! Egal ob du Erfahrung mit Software hast, oder dich gerade erst damit anfreundest. Kommen mal Fragen auf? Kontaktiere den Support jederzeit."]]]]
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
         [:h2.mt-5 "Steigere den Projekterfolg bereits vor dem Kickoff"]
         [:p.small.text-muted "\"In agilen Projekten ändern sich die Anforderungen häufiger, als dass sie bestehen bleiben. Wichtig ist es, das auch transparent festzuhalten.\" – Mike Birkhoff"]
         [:a.btn.btn-lg.btn-secondary.mt-4
          {:href (rfe/href :routes.schnaq/create)}
          "Upgrade dein Requirements-Engineering mit nur einem Schritt"]]]]
      [:div.wave-bottom-white-inverted]]
     [:section.container.pt-3
      [startpage/supporters]]]]))

(defn consulting-view []
  [startpage-content])

(defn consulting-umfrage []
  [startpage-content "https://schnaq.outgrow.us/agency"])
