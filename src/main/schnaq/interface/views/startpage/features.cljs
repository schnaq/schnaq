(ns schnaq.interface.views.startpage.features
  (:require [reitit.frontend.easy :as rfe]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.rows :as rows]))

(defn- what-is-schnaq
  "Box describing schnaq and its advantages"
  []
  [rows/video-right
   :animation-discussion/webm
   :animation-discussion/mp4
   :startpage.objections
   true "video-background-primary"])

(defn- schnaq-promise
  "Box describing schnaq and its advantages"
  []
  [rows/video-left
   :start-page.work-together/webm
   :start-page.work-together/mp4
   :startpage.promise
   true "video-background-primary"])

(defn- elephant-in-the-room
  "Feature box showcasing the elephant in the room."
  []
  [rows/image-right
   :schnaqqifant/admin
   :startpage.elephant-in-the-room
   [:p.text-center.mb-0
    [:a.btn.btn-primary
     {:href (rfe/href :routes/about-us)}
     (labels :startpage.elephant-in-the-room/button)]]])

(defn- feature-columns
  "Arguments for getting schnaq in three columns."
  []
  ;; TODO labelize
  [:div.row
   [:div.col-12.col-md-4
    [:h4.text-center "Know-how sichern"]
    [:p.text-center.text-primary.mt-0.py-0
     [:i {:class (str " m-auto fas fa-3x " (fa :book))}]]
    [:p "Digitalisierung und Mobile Office haben die Art, wie wir kommunizieren, verändert.
    Doch auch in modernen Prozessen bleibt die Herausforderung, Wissen im Unternehmen zu bündeln.
    Wir bieten dir mit unserem Produkt die Lösung, zeitgemäß zu kommunizieren und dabei das Know-how aller Experten zusammen zu bringen."]]
   [:div.col-12.col-md-4
    [:h4.text-center "Diskussionen demokratisieren"]
    [:p.text-center.text-primary.mt-0.py-0
     [:i {:class (str " m-auto fas fa-3x " (fa :comment))}]]
    [:p "Wer viel redet, muss nicht recht haben.
    Umgekehrt sind die größten Genies auf ihrem Gebiet manchmal eher introvertiert und reden ungerne vor anderen Menschen.
    Mit schnaq räumen wir mit diesem Missstand auf.
    Expert:innen können ihr Know-how einbringen und in konstruktive Diskussionen einsteigen – auch ohne viele Worte."]]
   [:div.col-12.col-md-4
    [:h4.text-center "Learnings nutzen"]
    [:p.text-center.text-primary
     [:i {:class (str " m-auto fas fa-3x " (fa :lightbulb))}]]
    [:p "Moderne Prozesse haben einen Haken: die Dokumentation.
    Oft werden die Learnings generiert, landen dann aber bestenfalls in Protokollen, die archiviert, aber nie wieder geöffnet werden.
    Schnaq schafft ein lebendiges Wissensmanagement, das einlädt, in Learnings zu stöbern und neue Wege zu gehen."]]])

;; -----------------------------------------------------------------------------

(defn feature-rows
  "Collection of feature rows."
  []
  [:<>
   [what-is-schnaq]
   [schnaq-promise]
   [elephant-in-the-room]
   [feature-columns]])
