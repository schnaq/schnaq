(ns schnaq.interface.pages.publications
  (:require [schnaq.interface.text.display-data :refer [img-path labels]]
            [schnaq.interface.views.pages :as pages]))

(defn- publication-card
  [pub-type title link summary & authors]
  [:div.card.shadow-sm
   [:div.card-body
    [:p.card-text
     [:a {:href link} title] [:br]
     [:span.text-muted [:i (labels pub-type)]]]
    [:p.card-text (labels summary)]
    (when authors
      [:p.card-text [:small "Autoren: " authors]])]])

(defn- publication-primer []
  [:section
   [:div.row.pb-5
    [:div.col-lg-4
     [:img.img-fluid.shadow {:src (img-path :team/vision-mindmap-team)}]]
    [:div.offset-lg-1.col-lg-7
     [:h2 (labels :publications.primer/heading)]
     [:p.lead (labels :publications.primer/body)]]]])

(defn- publications []
  [:section.pt-3
   [:div.card-columns.pb-5
    [publication-card :publications.kind/newspaper-article
     "\"Kann eine Software die Wut im Netz zähmen? 2 Forscher haben es versucht\""
     "https://perspective-daily.de/article/1355/YzjQDzO2"
     :publications.perspective-daily/summary]
    [publication-card :publications.kind/interview "\"Im Internet läuft es mies ab\""
     "https://www.salto.bz/de/article/12082020/im-internet-laeuft-es-mies-ab"
     :publications.salto/summary]
    [publication-card :publications.kind/dissertation "Untangling Internet Debate – Decentralization and Reuse of Arguments for Online Discussion Software"
     "https://docserv.uni-duesseldorf.de/servlets/DerivateServlet/Derivate-57043/Dissertation_Alexander_Schneider_Druckversion.pdf"
     :publications.dissertation-alex/summary]
    [publication-card :publications.kind/dissertation "Rethinking Online Discussions"
     "https://docserv.uni-duesseldorf.de/servlets/DerivateServlet/Derivate-57044/Dissertation_Christian_Meter.pdf"
     :publications.dissertation-christian/summary]
    [publication-card :publications.kind/paper "Structure or Content? Towards Assessing Argument Relevance"
     "https://wwwcn.cs.uni-duesseldorf.de/publications/publications/library/Feger2020a.pdf"
     :publications.structure-or-content/summary
     "Marc Feger, Jan Steimann, Dr. Christian Meter. "]
    [publication-card :publications.kind/paper "Various Efforts of Enhancing Real World Online Discussions"
     "https://wwwcn.cs.uni-duesseldorf.de/publications/publications/library/SchneiderMeter2019a.pdf"
     :publications.overview-paper/summary
     "Alexander Schneider, Christian Meter"]
    [publication-card :publications.kind/paper "D-BAS – A Dialog-Based Online Argumentation System"
     "https://wwwcn.cs.uni-duesseldorf.de/publications/publications/library/Krauthoff2018b.pdf"
     :publications.dbas/summary
     "Tobias Krauthoff, Christian Meter, Gregor Betz, Michael Baurmann, Martin Mauve"]
    [publication-card :publications.kind/article "Dialogbasierte Online-Diskussionen"
     "https://wwwcn.cs.uni-duesseldorf.de/publications/publications/library/Meter2018b.pdf"
     :publications.dbas-politics/summary
     "Christian Meter, Tobias Krauthoff, Alexander Schneider"]
    [publication-card :publications.kind/paper "EDEN: Extensible Discussion Entity Network"
     "https://wwwcn.cs.uni-duesseldorf.de/publications/publications/library/MeterSchneider2018a.pdf"
     :publications.eden/summary
     "Christian Meter, Alexander Schneider, Martin Mauve"]
    [publication-card :publications.kind/short-paper "Jebediah – Arguing With a Social Bot"
     "https://wwwcn.cs.uni-duesseldorf.de/publications/publications/library/Meter2018a.pdf"
     :publications.jebediah/summary
     "Christian Meter, Björn Ebbinghaus, Martin Mauve"]
    [publication-card :publications.kind/paper "Dialog-Based Online Argumentation: Findings from a Field Experiment"
     "https://wwwcn.cs.uni-duesseldorf.de/publications/publications/library/Krauthoff2017a.pdf"
     :publications.dbas-experiment/summary
     "Tobias Krauthoff, Christian Meter, Martin Mauve"]
    [publication-card :publications.kind/paper "Reusable Statements in Dialog-Based Argumentation Systems"
     "https://wwwcn.cs.uni-duesseldorf.de/publications/publications/library/Schneider2017b.pdf"
     :publications.reusable-statements/summary
     "Alexander Schneider, Christian Meter"]
    [publication-card :publications.kind/paper "discuss: Embedding Dialog-Based Discussions into Websites"
     "https://wwwcn.cs.uni-duesseldorf.de/publications/publications/library/Meter2017a.pdf"
     :publications.discuss/summary
     "Christian Meter, Tobias Krauthoff, Martin Mauve"]]])


;; ----------------------------------------------------------------------------

(defn- content [_request]
  [pages/with-nav-and-header
   {:page/heading (labels :publications/heading)
    :page/subheading [:<> (labels :publications/subheading) " " [:i.fas.fa-flask]]
    :page/vertical-header? true}
   [:div.container
    [publication-primer]
    [publications]]])

(defn view []
  [content])