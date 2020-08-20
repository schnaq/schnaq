(ns meetly.interface.text.display-data)


(defn labels
  "Returns a label as String for a given identifier"
  [identifier]
  (identifier
    {
     ;; navbar labels
     :nav-startpage "Home"
     :nav-example "Examples"
     :nav-meeting "Meetings"
     :nav-meeting-create "Meeting erstellen"
     :nav-meeting-agenda "Agenda erzeugen"

     ;; startpage header
     :start-page-header "Meetly"
     :start-page-subheader "Das Meeting Tool der Zukunft ist (fast) da!"
     :start-page-subheader-2 "Digitale Meetings neu gedacht und auf den Punkt gebracht"
     ;; key feeatures
     :start-page-point-community "Flexible Teilnahme"
     :start-page-point-moderation "Automatische Moderation"
     :start-page-point-reports "Zeitersparnis durch fokussierte Diskussion"
     :start-page-point-alpha "Betreten der Baustelle erwünscht!"
     :start-page-point-alpha-subtext "Meetly befindet sich im Aufbau und Feedback ist uns wichtig!"
     :create-meetly-button "Erstellen Sie jetzt Ihr Meetly"
     ;; startpage call to action
     :more-info "Fordern Sie mehr Informationen zum Aufbau Ihrer Community an"
     :more-info-newsletter "Holen Sie sich regelmäßig Updates zu Dialogo und den aktuellsten Produkten."
     :create-your-meeting "Erstellen Sie jetzt Ihr Meetly!"
     :create-your-meeting-sub "Mit einem Klick erstellen und teilen"

     ;; startpage grid
     :innovative "Innovativ"
     :innovative-why "Meetly nutzt wissenschaftlich erprobte neue Technologien"
     :communicative "Kommunikativ"
     :communicative-why "Lassen Sie Ihre NutzerInnen direkt zu Wort kommen"
     :cooperative "Kooperativ"
     :cooperative-why "Fördern Sie den Meinungsaustausch zwischen Ihren TeilnehmerInnen"

     ;; create meetly
     :meeting-create-header "Meetly erstellen"
     :meeting-create-subheader "Geben Sie Ihrem Meetly einen Namen und eine Beschreibung"
     :meeting-form-title "Titel"
     :meeting-form-title-placeholder "Wie soll Ihr Meetly heißen?"
     :meeting-form-desc "Beschreibung"
     :meeting-form-desc-placeholder "Worum geht es in Ihrem Meetly?"
     :meeting-form-deadline "Frist"
     :meeting-form-end-date "Datum"
     :meeting-form-end-time "Uhrzeit"
     :meeting/copy-share-link "Link kopieren:"
     :meeting/copy-link-tooltip "Hier klicken um Link zu kopieren"
     :meeting/link-copied-success "Der Link wurde in Ihre Zwischenablage kopiert!"
     :meeting/admin-link-copied-success "Der Admin-Link wurde in Ihre Zwischenablage kopiert!"
     :meeting/created-success-heading "Ihr Meetly wurde erstellt!"
     :meeting/created-success-subheading "Link verteilen und loslegen"
     :meeting/educate-on-link-text "Teilen Sie den untenstehenden Link mit Ihren KollegInnen."
     :meetings/educate-on-link-text-subtitle "Teilnahme ist für alle, die den Link kennen, möglich!"
     :meeting/educate-on-edit "Titel ändern oder Agendapunkte editieren?"
     :meeting/educate-on-admin "Später jederzeit per Admin Link editieren!"
     :meetings/continue-with-meetly-after-creation "Link kopiert? Legen Sie los!"
     :meetings/continue-to-meetly-button "Zum Meetly"
     :meetings/edit-meetly-button "Meetly editieren"

     :meeting.step2/button "2. Schritt: Agenda hinzufügen"

     ;; Create Agenda
     :agenda/header "Agenda erstellen"
     :agenda/subheader "Fügen Sie zu besprechende Punkte hinzu"
     :agenda/desc-for "Beschreibung für Agendapunkt "
     :agenda/point "Agendapunkt "

     ;; Edit Agenda
     :agenda/edit-title "Meeting editieren"
     :agenda/edit-subtitle "Beschreibung und Agendapunkte editieren"
     :agenda/edit-button "Änderungen speichern"

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

     ;; Feedbacks
     :feedbacks.overview/header "Rückmeldungen"
     :feedbacks.overview/subheader "Alle abgegebenen Rückmeldungen"
     :feedbacks.overview/description "Beschreibung"
     :feedbacks.overview/contact-name "Von"
     :feedbacks.overview/contact-mail "E-Mail"
     :feedbacks/button "Feedback"
     :feedbacks/screenshot "Screenshot"
     :feedbacks.modal/primer "Feedback ist wichtig! Wir freuen uns sehr über
     jede Art von Feedback, je ehrlicher desto besser 🥳 Hinterlassen Sie uns
     gerne einen kleinen Kommentar und helfen Sie uns damit diese Software
     weiter zu verbessern. Dankeschön!"
     :feedbacks.modal/contact-name "Ihr Name"
     :feedbacks.modal/contact-mail "E-Mail Adresse"
     :feedbacks.modal/description "Ihre Rückmeldung"
     :feedbacks.modal/optional "Optional"
     :feedbacks.modal/screenshot "Foto der Anwendung mit abschicken?"
     :feedbacks.modal/disclaimer "Ihre Daten werden nur auf unseren Servern
     abgespeichert und keinen Dritten zugänglich gemacht."

     ;; login
     :login/as "Hallo, "
     :login/set-name "Geben Sie Ihren Namen ein"

     :modals/enter-name-header "Geben Sie einen Namen ein"
     :modals/enter-name-primer "Der Name wird den anderen Teilnehmenden im Meetly angezeigt."

     ;; analytics
     :analytics/overall-meetings "Meetings erstellt"
     :analytics/user-numbers "Usernamen angelegt"
     :analytics/average-agendas-title "Durchschnittliche Zahl an Agendas pro Meeting"
     :analytics/statements-num-title "Anzahl Statements"
     :analytics/active-users-num-title "Aktive User (min. 1 Beitrag)"
     :analytics/statement-lengths-title "Beitragslängen"
     :analytics/argument-types-title "Argumenttypen"

     ;; User related
     :user.button/set-name "Name speichern"
     :user.button/set-name-placeholder "Ihr Name"

     ;; Errors
     :errors/navigate-to-startpage "Zurück zur Startseite"
     :errors/comic-relief "Ups ..."
     :errors/insufficient-access-rights "Sie haben nicht genügend Rechte um diese Seite anzusehen."


     ;; Route Link Texts
     :router/all-meetings "Alle Meetings"
     :router/all-feedbacks "Alle Feedbacks"
     :router/create-meeting "Meeting anlegen"
     :router/meeting-created "Zuletzt angelegtes Meeting"
     :router/show-single-meeting "Meeting anzeigen"
     :router/add-agendas "Agendas hinzufügen"
     :router/start-discussion "Starte Besprechung"
     :router/continue-discussion "Führe Besprechung fort"
     :router/startpage "Startseite"
     :router/analytics "Analyse-Dashboard"
     :router/invalid-link "Fehlerseite"
     :router/graph-view "Graph View"}))


(defn img-path
  "Returns an image path as String for a given identifier"
  [identifier]
  (identifier
    {:icon-community "imgs/community.svg"
     :icon-robot "imgs/robot.svg"
     :icon-reports "imgs/reports.svg"
     :icon-crane "imgs/crane.svg"
     :elephant-share "imgs/elephants/share.png"
     :elephant-talk "imgs/elephants/talk.png"
     :elephant-stop "imgs/elephants/stop.png"
     :elephant-admin "imgs/elephants/admin.png"
     :elephant-erase "imgs/elephants/erase.png"
     :logo "imgs/Meetly-Logo.svg"
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
     :delete-icon "fa-times-circle"
     :copy "fa-copy"}))
