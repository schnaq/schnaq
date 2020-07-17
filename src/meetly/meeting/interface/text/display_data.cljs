(ns meetly.meeting.interface.text.display-data)


(defn labels
  "Returns a label as String for a given identifier"
  [identifier]
  (identifier
    {
     ;; navbar labels
     :nav-example "Examples"
     :nav-meeting "Show Meetings"
     :nav-meeting-create "Create Meeting"
     :nav-meeting-agenda "Create Agenda"
     :nav-overview "Overview"
     ;; startpage header
     :start-page-header "Meetly"
     :start-page-subheader "Das Meeting Tool der Zukunft ist da!"
     :start-page-subheader-2 "Digitale Meetings neu gedacht und auf den Punkt gebracht"
     ;; key feeatures
     :start-page-point-community "Flexible Teilnahme"
     :start-page-point-moderation "Gezielte Abfrage von Informationen"
     :start-page-point-reports "Zeitersparnis durch fokussierte Diskussion"
     :create-meetly-button "Create Meetly"
     ;; startpage call to action
     :more-info "Fordern Sie mehr Informationen zum Aufbau Ihrer Community an"
     :more-info-newsletter "Holen Sie sich regelmäßig Updates zu Dialogo und den aktuellsten Produkten."
     :create-your-meeting "Erstellen Sie jetzt ihr Meetly!"
     :create-your-meeting-sub "Mit einem Klick erstellen, Agenda bestimmen und an alle Teilnehmenden schicken"
     ;; startpage grid
     :innovative "Innovativ"
     :innovative-why "Meetly nutzt wissenschaftlich erprobte neue Technologien"
     :communicative "Kommunikativ"
     :communicative-why "Lassen Sie ihre NutzerInnen direkt zu Wort kommen"
     :cooperative "Kooperativ"
     :cooperative-why "Fördern Sie den Meinungsaustausch zwischen Ihren TeilnehmerInnen"}))

(defn img-path
  "Returns an image path as String for a given identifier"
  [identifier]
  (identifier
    {:icon-community "imgs/community.svg"
     :icon-robot "imgs/robot.svg"
     :icon-reports "imgs/reports.svg"
     :woman-pointing "imgs/stock/woman_pointing.jpg"
     :logo "imgs/logo.svg"
     :animation-discussion "animations/animation_discussion.gif"}))

(defn fa
  "Returns an fontawesome icon id as String for a given identifier"
  [identifier]
  (identifier
    {:laptop "fa-laptop-code"
     :comment "fa-comments"
     :carry "fa-people-carry"}))