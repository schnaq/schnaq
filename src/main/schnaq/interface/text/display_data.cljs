(ns schnaq.interface.text.display-data)


(defn labels
  "Returns a label as String for a given identifier"
  [identifier]
  (identifier
    {
     ;; navbar labels
     :nav-startpage "Home"
     :nav-example "Examples"
     :nav-meeting "Alle schnaqs"
     :nav-meeting-create "Schnaq erstellen"
     :nav-meeting-agenda "Agenda erzeugen"

     ;; startpage header
     :start-page-header "schnaq"
     :start-page-subheader "Nie wieder langwierige Meetings ohne Ziel!"
     :start-page-subheader-2 "Schnaq strukturiert Meetings und bringt sie auf den Punkt"
     ;; key features
     :start-page-point-1 "Meetingziele kollaborativ festlegen"
     :start-page-point-2 "Informationen gezielt austauschen"
     :start-page-point-3 "Zeit sparen durch fokussierte Meetings"
     :start-page-point-alpha "Betreten der Baustelle erwünscht!"
     :start-page-point-alpha-subtext "schnaq befindet sich in einer Beta-Phase und Feedback ist uns wichtig!"
     :create-schnaq-button "Jetzt einen schnaq starten!"
     ;; startpage call to action
     :more-info "Fordern Sie mehr Informationen zum Aufbau Ihrer Community an"
     :more-info-newsletter "Holen Sie sich regelmäßig Updates zu DisqTec und den aktuellsten Produkten."
     :create-your-meeting-sub "Mit einem Klick erstellen und teilen"

     ;; startpage grid
     :innovative "Zielgerichtet"
     :innovative-why "Meetings mit einer klaren Agenda sind produktiver"
     :communicative "Kommunikativ"
     :communicative-why "Lassen Sie alle WissensträgerInnen zu Wort kommen"
     :cooperative "Kooperativ"
     :cooperative-why "Gemeinsame Planung fördert die aktive Teilnahme an Meetings"

     ;; create schnaq
     :meeting-create-header "schnaq erstellen"
     :meeting-create-subheader "Geben Sie Ihrem schnaq einen Namen und eine Beschreibung"
     :meeting-form-title "Titel"
     :meeting-form-title-placeholder "Wie soll Ihr schnaq heißen?"
     :meeting-form-desc "Beschreibung"
     :meeting-form-desc-placeholder "Worum geht es in Ihrem schnaq?"
     :meeting-form-deadline "Frist"
     :meeting-form-end-date "Datum"
     :meeting-form-end-time "Uhrzeit"
     :meeting/copy-share-link "Link kopieren:"
     :meeting/copy-link-tooltip "Hier klicken um Link zu kopieren"
     :meeting/link-copied-heading "Link kopiert"
     :meeting/link-copied-success "Der Link wurde in Ihre Zwischenablage kopiert!"
     :meeting/created-success-heading "Ihr schnaq wurde erstellt!"
     :meeting/created-success-subheading "Link verteilen und loslegen"
     :meeting/educate-on-link-text "Teilen Sie den untenstehenden Link mit Ihren KollegInnen."
     :meetings/educate-on-link-text-subtitle "Teilnahme ist für alle, die den Link kennen, möglich!"
     :meeting/educate-on-edit "Titel ändern oder Agendapunkte editieren?"
     :meeting/educate-on-admin "Später jederzeit per Admin Link editieren!"
     :meetings/continue-with-schnaq-after-creation "Link kopiert? Legen Sie los!"
     :meetings/continue-to-schnaq-button "Zum schnaq"
     :meetings/edit-schnaq-button "schnaq editieren"
     :meeting.step2/button "Schritt 2: Agenda hinzufügen"

     :meeting.admin/addresses-label "Teilnehmer-Adressen"
     :meeting.admin/addresses-placeholder "E-Mail Adressen getrennt mit Leerzeichen oder Zeilenumbruch eingeben."
     :meeting.admin/send-invites-button-text "Einladungen versenden"
     :meeting.admin/send-invites-heading "Laden Sie die Teilnehmenden per Email ein"
     :meeting.admin.notifications/emails-successfully-sent-title "Einladungen verschickt!"
     :meeting.admin.notifications/emails-successfully-sent-body-text "Ihre Einladungen wurden erfolgreich an die Teilnehmenden versendet."
     :meeting.admin.notifications/sending-failed-title "Fehler bei Zustellung!"
     :meeting.admin.notifications/sending-failed-lead "Die Einladung konnte an folgende Adressen nicht zugestellt werden: "

     ;; schnaqs not found
     :schnaqs.not-found/alert-lead "Leider wurden keine schnaqs gefunden, zu denen Sie Zugriff haben."
     :schnaqs.not-found/alert-body "Laden Sie zu Ihrem ersten schnaq ein, indem Sie einen erstellen."


     ;; Create Agenda
     :agenda/header "Agenda erstellen"
     :agenda/subheader "Fügen Sie zu besprechende Punkte hinzu"
     :agenda/desc-for "Beschreibung für Agendapunkt "
     :agenda/point "Agendapunkt "
     :agenda.create/optional-agenda "Agenda hinzufügen"

     ;; Edit Agenda
     :agenda/edit-title "Schnaq editieren"
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
     :discussion.badges/user-overview "Alle Teilnehmenden"
     :discussion.notification/new-content-title "Neuer Beitrag!"
     :discussion.notification/new-content-body "Ihr Beitrag wurde erfolgreich gespeichert."

     ;; meetings overview
     :meetings/header "Schnaqs"
     :meetings/subheader "Ihre aktuellen Schnaqs"

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
     :feedbacks.notification/title "Vielen Dank für Ihre Rückmeldung!"
     :feedbacks.notification/body "Ihr Feedback wurde erfolgreich an uns
     gesendet 🎉"

     ;; login
     :login/as "Hallo, "
     :login/set-name "Geben Sie Ihren Namen ein"

     :modals/enter-name-header "Geben Sie einen Namen ein"
     :modals/enter-name-primer "Der Name wird den anderen Teilnehmenden im schnaq angezeigt."

     ;; analytics
     :analytics/overall-meetings "Schnaqs erstellt"
     :analytics/user-numbers "Usernamen angelegt"
     :analytics/average-agendas-title "Durchschnittliche Zahl an Agendas pro Schnaq"
     :analytics/statements-num-title "Anzahl Statements"
     :analytics/active-users-num-title "Aktive User (min. 1 Beitrag)"
     :analytics/statement-lengths-title "Beitragslängen"
     :analytics/argument-types-title "Argumenttypen"
     :analytics/fetch-data-button "Hole Daten"

     ;; User related
     :user.button/set-name "Name speichern"
     :user.button/set-name-placeholder "Ihr Name"
     :user.button/success-body "Name erfolgreich gespeichert"
     :user.set-name/dialog-header "Hallo 👋"
     :user.set-name/dialog-lead "Schön, dass Sie hier sind!"
     :user.set-name/dialog-body "Um an Diskussionen teilzunehmen ist es notwendig, dass Sie einen Namen eingeben."
     :user.set-name/dialog-button "Wie möchten Sie genannt werden?"

     ;; Errors
     :errors/navigate-to-startpage "Zurück zur Startseite"
     :errors/comic-relief "Ups ..."
     :errors/insufficient-access-rights "Sie haben nicht genügend Rechte um diese Seite anzusehen."
     :errors/generic "Es ist ein Fehler aufgetreten"
     :error.404/heading "Diese Seite existiert nicht 🙉"
     :error.404/body-text "Die URL der Sie gefolgt sind existiert leider nicht. Möglicherweise hat sich ein Tippfehler
     oder ein Zeichen zu viel eingeschlichen."
     :error.404/help-text "\uD83D\uDC50 Sollten Sie hier landen nachdem Sie etwas auf schnaq.com angeklickt haben, geben Sie uns gerne Bescheid unter "

     ;; Graph Texts
     :graph/heading "Diskussionsübersicht"

     ;; Route Link Texts
     :router/all-meetings "Alle schnaqs"
     :router/all-feedbacks "Alle Feedbacks"
     :router/create-meeting "Schnaq anlegen"
     :router/meeting-created "Zuletzt angelegter schnaq"
     :router/my-schnaqs "Meine schnaqs"
     :router/show-single-meeting "Schnaq anzeigen"
     :router/add-agendas "Agendas hinzufügen"
     :router/start-discussion "Starte Besprechung"
     :router/continue-discussion "Führe Besprechung fort"
     :router/startpage "Startseite"
     :router/analytics "Analyse-Dashboard"
     :router/invalid-link "Fehlerseite"
     :router/true-404-view "404 Fehlerseite"
     :router/not-found-label "Not Found route redirect"
     :router/graph-view "Graph View"}))


(defn img-path
  "Returns an image path as String for a given identifier"
  [identifier]
  (identifier
    {:icon-community "/imgs/community.svg"
     :icon-robot "/imgs/robot.svg"
     :icon-reports "/imgs/reports.svg"
     :icon-crane "/imgs/crane.svg"
     :icon-graph "/imgs/graph/graph-icon.svg"
     :elephant-share "/imgs/elephants/share.png"
     :elephant-talk "/imgs/elephants/talk.png"
     :elephant-stop "/imgs/elephants/stop.png"
     :elephant-admin "/imgs/elephants/admin.png"
     :elephant-erase "/imgs/elephants/erase.png"
     :logo "/imgs/Schnaq-Logo.svg"
     :animation-discussion "/animations/animation_discussion.gif"}))

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
     :copy "fa-copy"
     :users "fa-users"}))
