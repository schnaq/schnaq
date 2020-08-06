(ns meetly.meeting.interface.text.display-data)


(defn labels
  "Returns a label as String for a given identifier"
  [identifier]
  (identifier
    {
     ;; navbar labels
     :nav-startpage "Home"
     :nav-example "Examples"
     :nav-meeting "Meetings"
     :nav-meeting-create "Meeting Erstellen"
     :nav-meeting-agenda "Create Agenda"

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
     :create-your-meeting-sub "Mit einem Klick erstellen und an alle Teilnehmenden schicken"

     ;; startpage grid
     :innovative "Innovativ"
     :innovative-why "Meetly nutzt wissenschaftlich erprobte neue Technologien"
     :communicative "Kommunikativ"
     :communicative-why "Lassen Sie ihre NutzerInnen direkt zu Wort kommen"
     :cooperative "Kooperativ"
     :cooperative-why "Fördern Sie den Meinungsaustausch zwischen Ihren TeilnehmerInnen"

     ;; create meetly
     :meeting-create-header "Meetly erstellen"
     :meeting-create-subheader "Geben Sie ihrem Meetly einen Namen und eine Beschreibung"
     :meeting-form-title "Titel"
     :meeting-form-title-placeholder "Wie soll ihr Meetly heißen?"
     :meeting-form-desc "Beschreibung"
     :meeting-form-desc-placeholder "Worum geht es in ihrem Meetly?"
     :meeting-form-deadline "Frist"
     :meeting-form-end-date "Datum"
     :meeting-form-end-time "Uhrzeit"
     :meeting/copy-share-link "Link kopieren:"
     :meeting/copy-link-tooltip "Hier klicken um Link zu kopieren"
     :meeting/link-copied-success "Der Link wurde in Ihre Zwischenablage kopiert!"
     :meeting/created-success-heading "Ihr Meetly wurde erstellt!"
     :meeting/created-success-subheading "Link verteilen und loslegen"
     :meeting/educate-on-link-text "Verteilen Sie den untenstehenden Link an Ihre KollegInnen.
     Teilnahme ist für alle die den Link kennen möglich!"
     :meetings/continue-with-meetly-after-creation "Link kopiert? Legen Sie los!"
     :meetings/continue-to-meetly-button "Zum Meetly"

     ;; Create Agenda
     :agenda-header "Agenda erstellen"
     :agenda-subheader "Fügen Sie zu besprechende Punkte hinzu"
     :agenda-desc-for "Beschreibung für Top "
     :agenda-point "Agendapunkt "

     ;; Discussion Language
     :discussion/agree "Zustimmung"
     :discussion/disagree "Ablehnung"
     :discussion/create-argument-action "Meinung hinzufügen"
     :discussion/create-argument-heading "Eigene Meinung abgeben / Informationen hinzufügen"
     :discussion/add-argument-conclusion-placeholder "Das denke ich darüber."
     :discussion/add-argument-premise-placeholder "Und das ist meine Begründung dafür."
     :discussion/add-starting-premise-placeholder "weil..."
     :discussion/add-premise-supporting "Ich möchte die Aussage unterstützen"
     :discussion/add-premise-against "Ich habe einen Grund dagegen"
     :discussion/add-undercut "Die letzten beiden Aussagen passen nicht zusammen"
     :discussion/reason-nudge "Was denken Sie darüber?"
     :discussion/premise-placeholder "Ich denke..."
     :discussion/create-starting-premise-action "Beitrag hinzufügen"
     :discussion/others-think "Andere denken folgendes:"
     :discussion/undercut-bubble-intro "Der letzte Beitrag hat nichts mit dem vorherigen zu tun. Begründung:"

     ;; meetings overview
     :meetings/header "Meetings"
     :meetings/subheader "Ihre aktuellen Meetings"

     ;; login
     :login/as "Hallo, "
     :login/set-name "Geben Sie ihren Namen ein"

     :modals/enter-name-header "Geben Sie einen Namen ein"
     :modals/enter-name-primer "Der Name wird den anderen Teilnehmenden im Meetly angezeigt."}))


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
     :carry "fa-people-carry"
     :arrow-right "fa-arrow-right"
     :arrow-left "fa-arrow-left"
     :arrow-up "fa-arrow-up"
     :arrow-down "fa-arrow-down"
     :copy "fa-copy"}))
