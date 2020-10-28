(ns schnaq.interface.text.display-data
  "Texts used as labels in the whole application.")

(defn labels
  "Returns a label as String for a given identifier"
  [identifier]
  (identifier
    {
     ;; Common
     :common/save "Speichern"

     ;; navbar labels
     :nav/startpage "Home"
     :nav/schnaqs "Schnaqs"
     :nav.schnaqs/show-all "Alle schnaqs"
     :nav.schnaqs/create-meeting "Meeting vorbereiten"
     :nav.schnaqs/create-brainstorm "Brainstorm anlegen"
     :nav.schnaqs/last-added "Zuletzt angelegter schnaq"
     :nav-meeting-agenda "Agenda erzeugen"
     :nav/blog "Zum Blog"

     ;; Startpage
     :startpage/heading "Qualifizierte Brainstormings"
     :startpage/subheading "Schnaq ermöglicht nachhaltigen Gedankenaustausch"

     :startpage.heading-list/community "Gemeinsame Brainstormings"
     :startpage.heading-list/exchange "Ideen strukturiert diskutieren"
     :startpage.heading-list/reports "Entscheidungen gemeinsam treffen"
     :startpage.under-construction/heading "Betreten der Baustelle erwünscht!"
     :startpage.under-construction/body "schnaq befindet sich in einer kostenlosen Beta-Phase und Feedback ist uns wichtig!"

     :startpage.value-cards/heading "Weitere Anwendungsgebiete"
     :startpage.value-cards/lead "Strukturierte Diskussionen werden in vielen Gebieten benötigt. Hier finden Sie weitere Beispiele."
     :startpage.value-cards.discussion/title "Diskussionen führen"
     :startpage.value-cards.discussion/description "Es ist nicht einfach über das Internet miteinander zu Diskutieren, ohne sich schnell verloren zu fühlen. Mit schnaq können Sie strukturierte Diskussionen führen, und dabei leicht den Überblick über die Argumente und kontroverse Themen behalten."
     :startpage.value-cards.discussion/alt-text "Ein Symbolbild einer Sprechblase"
     :startpage.value-cards.meetings/title "Meetings optimieren"
     :startpage.value-cards.meetings/description "Vor allem seitdem die Arbeitswelt verteilter geworden ist, hetzt man von einem (digitalen) Meeting zum anderen. Damit die Vor- und Nachbereitung nicht mehr zur Nebensache wird, gibt es schnaq. Sparen Sie Arbeitszeit und optimieren Sie die Ergebnisse Ihrer Meetings."
     :startpage.value-cards.meetings/alt-text "Menschen in einem Meeting, eine Frau redet gerade"
     :startpage.value-cards.knowledge/title "Brainstorming und Wissensgenerierung"
     :startpage.value-cards.knowledge/description "Nutzen Sie schnaq, um Ihr Team nachdenken und diskutieren zu lassen. Automatisch wird eine MindMap der Beiträge erstellt, die dann analysiert werden kann und das Ergebnis der Session darstellt. Perfekt für Brainstorming-Sitzungen!"
     :startpage.value-cards.knowledge/alt-text "Etliche Klebezettel auf einer Wand"
     :startpage.value-cards.button/text "Mehr erfahren"

     :startpage.usage/lead "Wofür kann ich schnaq verwenden?"

     :startpage.features/more-information "Mehr Informationen"

     :startpage.demo.request/title "Demo anfordern"
     :startpage.demo.request/body "Möchten Sie erfahren, wie Sie Ihrem Unternehmenswissen ein Langzeitgedächtnis verleihen? Wir führen Ihnen Schnaq dazu gerne persönlich vor. Nutzen Sie einfach den Button und wir werden uns schnellstmöglich bei Ihnen melden."
     :startpage.demo.request/button "Jetzt Demo anfordern!"
     :startpage.demo.request.modal.name/label "Ihr Name"
     :startpage.demo.request.modal.name/placeholder "Mein Name"
     :startpage.demo.request.modal.email/label "E-Mail Adresse"
     :startpage.demo.request.modal.email/placeholder "meine@email.de"
     :startpage.demo.request.modal.company/label "Name des Unternehmens"
     :startpage.demo.request.modal.company/placeholder "Firmenname, falls zutreffend"
     :startpage.demo.request.modal.phone/label "Telefon"
     :startpage.demo.request.modal.phone/placeholder "0 1234 56789"
     :startpage.demo.request.send.notification/title "Anfrage versendet!"
     :startpage.demo.request.send.notification/body "Es wird sich schnellstmöglich jemand bei Ihnen melden."
     :startpage.demo.request.send.notification/failed-title "Anfrage fehlgeschlagen!"
     :startpage.demo.request.send.notification/failed-body "Etwas ist schief gelaufen. Überprüfen Sie Ihre Eingaben und versuchen Sie es bitte erneut."

     :startpage.features.meeting-organisation/lead "Gemeinsam Arbeiten"
     :startpage.features.meeting-organisation/title "Kollaborativer Meinungsaustausch"
     :startpage.features.meeting-organisation/body "Binden Sie Ihre Mitarbeiter:innen mit in Diskussionen ein! Aktivieren Sie ungenutzte Ressourcen und erreichen Sie so eine höhere Zufriedenheit in Ihrem Team. Brainstorming-Sessions sind kreative Prozesse, in denen die Diversität Ihres Teams sehr gut zur Geltung kommt und zu wunderbaren Ergebnissen führt."
     :startpage.features.discussion/lead "Strukturierte Diskussionen"
     :startpage.features.discussion/title "Nachhaltiger Austausch"
     :startpage.features.discussion/body "Brainstorming-Sessions sind Teil der kreativen Arbeit in vielen Teams. Im Nachgang lässt sich aber nur schwer nachvollziehen, was die einzelnen Teilnehmer:innen beigetragen haben. Durch die strukturierte Erfassung im verteilten Brainstorming werden die Aussagen so zusammengefügt, dass sie auch nach einer längeren Zeit wieder nachvollzogen werden können."
     :startpage.features.graph/lead "Übersichtliche Darstellung"
     :startpage.features.graph/title "Mindmaps!"
     :startpage.features.graph/body "Alle Beiträge der Teilnehmer:innen werden automatisch in einer Mindmap angeordnet und können dann nachvollzogen werden. Sehen Sie alle Zusammenhänge und beobachten Sie die gesamte Ideen- und Entscheidungsfindung in einer interaktiven Mindmap."

     ;; Meeting Feature Page
     :feature.meetings/lead "Organisierte Meetings mit schnaq"
     :feature.meetings/title "Mitarbeiter:innen aktivieren, Zeit sparen"
     :feature.meetings/body "Schnaq lässt sich nutzen, um Meetings effizient vorzubereiten, alle Teilnehmer:innen während dem Meeting zu unterstützen und erlaubt eine Nachbereitung des Meetings. Damit werden alle Teilnehmer:innen aktiviert, Arbeitszeit eingespart und Ergebnisse effizienter erzielt."
     :feature.meetings.schedule/lead "Gezielte Vorbereitung"
     :feature.meetings.schedule/title "Agenda kollaborativ vorbereiten"
     :feature.meetings.schedule/body "Bereiten Sie eine erste Version der Agenda vor. Die Agenda und angehängte vorbereitende Materialien werden per E-Mail und Kalendereinladung an alle Teilnehmer:innen verteilt. Durch schnaq können alle Kolleg:innen Änderungsvorschläge und Ergänzungen für einzelne Agendapunkte vorschlagen, die von der Meetingersteller:in angenommen werden können."
     :feature.meetings.discuss/lead "Strittige Punkte klären"
     :feature.meetings.discuss/title "Diskutieren Sie unklare Punkte im Vorhinein"
     :feature.meetings.discuss/body "Bei Punkten, die nicht klar umrissen sind, bietet sich eine Online-Diskussion an. Diese kann man direkt über die vorgeschlagenen Agendapunkte erreichen und ad-hoc erledigen. Die Diskussion kann der Ausrichtung einer Agenda dienen, aber auch der Klärung von offenen Punkten, bei denen nicht klar ist, wie die Agenda gestaltet werden sollte. Dies geschieht strukturiert und asynchron, so dass alle Mitarbeiter:innen teilnehmen können. Das Ergebnis sind fokussierte Meetings."
     :feature.meetings.admin-center/lead "Accounts nicht notwendig"
     :feature.meetings.admin-center/title "Verwalten Sie schnaq ohne zusätzliche Accounts"
     :feature.meetings.admin-center/body "Schnaq lässt sich bequem durch gesicherte Links verwalten. Sie benötigen keine weiteren Accounts, sondern lediglich einen Browser. So können Sie von überall auf schnaq und Ihre Meetings zugreifen. Im Admin-Center können Sie Ihr Meeting administrieren und alle wichtigen Aktionen von überall ausführen."
     :feature.meetings/heading "Sparen Sie Arbeitszeit durch strukturierte Meetings"
     :feature.meetings/features-subheading "Schnaq für Meetings einsetzen"
     :feature.meetings/tbd-subheading "More to come..."
     :feature.meetings/tbd-lead "Werden Sie Early Adopter:in und genießen Sie folgende Features als erste, sobald diese verfügbar sind:"
     :feature.meetings.tbd/teams "Integration mit MS Teams und Slack"
     :feature.meetings.tbd/outlook "Outlook-Export"
     :feature.meetings.tbd/protocols "Kollaborative Protokollführung im Meeting"
     :feature.meetings.tbd/prereads "Dateianhänge / Prereads während der Agendaerstellung"
     :feature.meetings.tbd/assignments "Personenzuteilung für einzelne Agendapunkte"
     :feature.meetings.tbd/timeboxing "Timeboxing während des Meetings"
     :feature.meetings.tbd/task-tracking "Übertragung der Ergebnisse in Task-Tracker"
     :feature.meetings/feedback "Haben Sie weitere Wünsche? Kontaktieren Sie uns gerne über das Feedback-Formular auf der rechten Seite."
     :features.privacy/lead "EU-Konformes Vorgehen"
     :features.privacy/title "Datenschutz ist uns wichtig!"
     :features.privacy/body "Das Entwicklerteam von schnaq besteht aus Informatiker:innen, die es Leid sind, dass mit Daten nicht sorgfältig umgegangen wird. Deshalb legen wir besonderen Wert darauf, DSGVO-Konform zu agieren und sämtliche Daten sicher auf europäischen Servern zu speichern. Kein Datenaustausch mit anderen Unternehmen, keine faulen Kompromisse!"


     ;; calendar invitation
     :calendar-invitation/title "Termin festlegen und herunterladen"
     :calendar-invitation/download-button "Termin herunterladen"
     :calendar-invitation/date-error "Das Ende des Meetings darf nicht vor dem Start liegen."

     :feature.discussions/lead "Strukturierte Diskussionen mit Kolleg:innen und Kunden"
     :feature.discussions/title "Strukturierte Diskussionen für strukturierte Ergebnisse"
     :feature.discussions/body "Mit schnaq können strukturierte Diskussionen online und ohne weitere Gerätschaften geführt werden. Wir haben Ergebnisse aus unserer langjährigen Forschung genutzt, um eine optimale Diskussionsform zu entwickeln, die strukturierte Diskussionen ermöglicht. Die Ergebnisse der Diskussionen lassen sich strukturiert und automatisch in graphischer Form aufbereiten und erlauben so die Nachvollziehbarkeit einer Diskussion auf einen Blick. Wertvolle Informationen für alle Entscheider:innen und deren Teams!"
     :feature.discussions/features-subheading "Schnaq als Diskussionsplattform"
     :feature.discussions.spaces/lead "Egal ob Brainstorming oder Diskussion"
     :feature.discussions.spaces/title "Schaffen Sie Platz für Diskussionen"
     :feature.discussions.spaces/body "Erstellen Sie Diskussionsthemen, die Sie ihren Teams einfach per Link zugänglich machen können. Die eingeladenen Kolleg:innen können den geschaffenen Raum nutzen, um zeitversetzt und online miteinander zu diskutieren und ihr Wissen einfließen zu lassen."
     :feature.discussions.discuss/lead "Einfach online diskutieren"
     :feature.discussions.discuss/title "Strukturierter Austausch per schnaq"
     :feature.discussions.discuss/body "Das Interface von schnaq-Diskussionen basiert auf wissenschaftlichen Erkenntnissen aus der Diskussionsforschung. Die Teilnehmer:innen werden dabei immer angeleitet sachliche und begründete Beiträge abzugeben. Dadurch bleiben Diskussionen und auf ihnen basierende Entscheidungen leicht nachvollziehbar."
     :feature.discussions.graph/lead "Alles auf einen Blick"
     :feature.discussions.graph/title "Graphische Aufbereitung der Diskussion"
     :feature.discussions.graph/body "Alle Diskussionen werden automatisiert graphisch aufbereitet. So bekommt man sofort einen Überblick bezüglich stark diskutierter Bereiche, dem Zusammenhang der Beiträge und kontroversen Standpunkten innerhalb der Diskussion. Durch die graphische Aufbereitung lassen sich Entscheidungen bequem und einfach visualisieren."
     :feature.discussions.tbd/reports "Automatisierte Zusammenfassungen der Diskussionen"
     :feature.discussions.tbd/wikis "Anbindung an Wiki-Systeme, wie Confluence"
     :feature.discussions.tbd/ideas "Automatisierte Benachrichtigungen, wenn Themen diskutiert werden, die abonniert wurden"
     :feature.discussions.tbd/navigation "Innovative Navigation durch große Diskussionen"
     :feature.discussions.tbd/connect "Anbindung an MS Teams, Slack und Co."
     :feature.discussions.tbd/bot "AI-basierte Hinweise, welche Bereiche weiteren Input benötigen"

     :feature.knowledge/lead "Wissen sammeln in Brainstormings"
     :feature.knowledge/subheading "Entscheidungsfindungen nachhaltig verbessern"
     :feature.knowledge.general/lead "Nachhaltige Wissensaufbereitung"
     :feature.knowledge.general/title "Wissen und Ideen greifbar machen"
     :feature.knowledge.general/body "Kommunikation ist der Schlüssel zum Erfolg. Jede Meinung sollte gehört werden können, damit im Anschluss eine qualifizierte Entscheidung gefasst werden kann. Um diese Meinungen und Argumente auch später wieder verfügbar zu haben, bietet schnaq die Möglichkeit zur strukturierten Erfassung von Ideen. Diese können später nachvollzogen werden und liefern Aufschluss über die ursprünglichen Gedanken."
     :feature.knowledge/features-subheading "Schnaq zur Ideenfindung 💡"
     :feature.knowledge.discussions/lead "Entscheidungsfindung"
     :feature.knowledge.discussions/title "Am Anfang steht das Brainstorming"
     :feature.knowledge.discussions/body "Offline entstehen die besten Ideen auf den Gängen oder in der Kaffeepause – schnaq bildet diese lockeren Diskussionen ab, ganz ohne 15 zeitraubende E-Mails oder 20 Chatnachrichten zu benötigen. Beim Brainstorming mit schnaq können alle Teammitglieder:innen gehört werden und ihre Ideen festhalten. Durch diesen kreativen Prozess können Ideen entstehen und festgehalten werden, die wertvoll für das Unternehmen sind – sogar wenn man nicht zusammen im Büro sitzt."

     :feature.knowledge.database/lead "Nachvollziehbarkeit"
     :feature.knowledge.database/title "Zentrale Anlaufstelle für Ideen und Entscheidungen"
     :feature.knowledge.database/body "Bewahren Sie Ideen aus Brainstormings und Diskussionen auf und ermöglichen Sie so ein späteres Nachvollziehen der Gedankengänge. Häufig fragt man sich schon nach Tagen, manchmal auch nach Monaten nach dem Sinn einer Entscheidung oder einem Gedankengang und hat mit schnaq nun so die Möglichkeit die einzelnen Diskussionspunkte genau nachzuvollziehen."

     :feature.knowledge.change-of-facts/lead "Graphische Darstellung"
     :feature.knowledge.change-of-facts/title "MindMaps!"
     :feature.knowledge.change-of-facts/body "Alle Ideen und Gedankengänge werden automatisch und sofort visuell aufbereitet. Schauen Sie sich jederzeit die generierte MindMap an und vollziehen Sie so die Gedanken Ihres Teams nach. Kontrovers diskutierte Bereiche werden für Sie automatisch hervorgehoben, sodass Sie sofort sehen können, welche Punkte weitere Klärung benötigen."

     :feature.knowledge.tbd/wiki "Einbindung in bestehende Wiki-Systeme (bspw. Confluence)"
     :feature.knowledge.tbd/search "Indexierung von Ideen, Gedanken und Diskussionen zum einfachen Finden"
     :feature.knowledge.tbd/evaluation "\"What if?\" Blenden Sie Argumente aus und sehen Sie, wie sich die Entscheidungsfindung verändert"
     :feature.knowledge.tbd/live-changes "Live-Veränderungen der Diskussionsgrundlage mitverfolgen"
     :feature.knowledge.tbd/changes-over-time "Springen Sie an jeden Punkt in der Vergangenheit und schauen Sie sich die Entwicklung der Ideen an"
     :feature.knowledge.tbd/accounts "Integration in bestehende Kommunikationssysteme (bspw. Slack, MS Teams, ...)"

     :how-to.startpage/title "Wie benutze ich schnaq?"
     :how-to.startpage/body "Sie möchten schnaq nutzen, sind aber unsicher, wie die Bedienung funktioniert? Wir haben eine ausführliche Anleitung mit kurzen Videos erstellt, um Ihnen den Einstieg zu erleichtern."
     :how-to.startpage/button "Wie schnaqqe ich?"
     :how-to/title "Wie benutze ich schnaq?"
     :how-to.why/title "Wozu dient schnaq?"
     :how-to.why/body "Schnaq dient dazu Meetings und andere Treffen im Voraus mit den Teilnehmer:innen zu planen und zu diskutieren."
     :how-to.create/title "schnaq erstellen"
     :how-to.create/body "Legen Sie zuerst einen schnaq an. Geben Sie Ihrem schnaq danach einen Titel und eine Beschreibung. Sie können auch Bilder und Dokumente verlinken."
     :how-to.agenda/title "Agenda erstellen"
     :how-to.agenda/body "Sie können mehrere Agendapunkte anlegen, um Ihren schnaq granularer zu planen und um Themen einzeln zu diskutieren."
     :how-to.admin/title "Teilnehmer:innen einladen"
     :how-to.admin/body "Teilnehmer:innen können entweder per Link oder Mail eingeladen werden. Weitere Admins laden Sie über den Admin Zugang ein. Administrator:innen können ebenfalls Teilnehmer:innen einladen oder den schnaq editieren."
     :how-to.call-to-action/title "Genug gequatscht, jetzt wird geschnaqqt!"
     :how-to.call-to-action/body "Starten Sie jetzt Ihren schnaq bequem mit einem Klick! Laden Sie Teilnehmer:innen ein und diskutieren Sie Vorschläge untereinander. Kollaborative Vorbereitung ohne Hürden, ganz einfach gemacht."

     :startpage.early-adopter/title "Neugierig geworden?"
     :startpage.early-adopter/body "Nutzen Sie exklusiv während der Beta-Phase schnaq.com und zählen Sie damit zu den Vorreitern."
     :startpage.early-adopter.buttons/join-schnaq "Beispielschnaq ansehen"
     :startpage.early-adopter/or "oder"

     :startpage.mailing-list/title "Fordern Sie mehr Informationen zu schnaq an"
     :startpage.mailing-list/body "Holen Sie sich regelmäßig Updates zu schnaq, DisqTec und den aktuellsten Produkten."
     :startpage.mailing-list/button "Zum Newsletter anmelden"

     :footer.buttons/about-us "Über uns"
     :footer.buttons/legal-note "Impressum"
     :footer.buttons/privacy "Datenschutz"

     ;; Create schnaqs
     :schnaqs/create "schnaq anlegen"

     ;; Create meeting
     :meeting-create-header "Meeting vorbereiten"
     :meeting-create-subheader "Geben Sie Ihrem schnaq einen Namen und eine Beschreibung"
     :meeting-form-title "Titel"
     :meeting-form-title-placeholder "Wie soll Ihr schnaq heißen?"
     :meeting-form-description "Beschreibung"
     :meeting-form-description-placeholder "Dauer: X Minuten\n\nThema"
     :meeting-form-end-date "Datum"
     :meeting-form-end-time "Uhrzeit"
     :meeting/copy-share-link "Link kopieren:"
     :meeting/copy-link-tooltip "Hier klicken um Link zu kopieren"
     :meeting/link-copied-heading "Link kopiert"
     :meeting/link-copied-success "Der Link wurde in Ihre Zwischenablage kopiert!"
     :meeting/created-success-heading "Ihr schnaq wurde erstellt!"
     :meeting/created-success-subheading "Nun können Sie den Zugangslink verteilen oder andere Personen per Mail einladen 🎉"
     :meetings/continue-with-schnaq-after-creation "Alle eingeladen? Legen Sie los!"
     :meetings/continue-to-schnaq-button "Zum schnaq"
     :meetings/edit-schnaq-button "schnaq editieren"
     :meetings/share-calendar-invite "Kalendareinladung versenden"
     :meetings.suggestions/header "Vorschläge einreichen"
     :meetings.suggestions/subheader "Die erstellende Person kann die Vorschläge einsehen und berücksichtigen"

     :meeting.admin/addresses-label "E-Mail Adressen der Teilnehmer:innen"
     :meeting.admin/addresses-placeholder "E-Mail Adressen getrennt mit Leerzeichen oder Zeilenumbruch eingeben."
     :meeting.admin/addresses-privacy "Diese Adressen werden ausschließlich zum Mailversand genutzt und danach sofort von unseren Servern gelöscht."
     :meeting.admin/send-invites-button-text "Einladungen versenden"
     :meeting.admin/send-invites-heading "Laden Sie die Teilnehmer:innen per E-Mail ein"
     :meeting.admin.notifications/emails-successfully-sent-title "Mail(s) verschickt!"
     :meeting.admin.notifications/emails-successfully-sent-body-text "Ihre Mail(s) wurden erfolgreich versendet."
     :meeting.admin.notifications/sending-failed-title "Fehler bei Zustellung!"
     :meeting.admin.notifications/sending-failed-lead "Die Einladung konnte an folgende Adressen nicht zugestellt werden: "

     ;; Brainstorming time
     :brainstorm/heading "Brainstorm anlegen"
     :brainstorm.buttons/start-now "Jetzt ein Brainstorming starten"

     ;; schnaqs not found
     :schnaqs.not-found/alert-lead "Leider wurden keine schnaqs gefunden, zu denen Sie Zugriff haben."
     :schnaqs.not-found/alert-body "Laden Sie zu Ihrem ersten schnaq ein, indem Sie einen erstellen."

     ;; Admin Center
     :meeting/educate-on-link-text "Teilen Sie den untenstehenden Link mit Ihren KollegInnen."
     :meetings/educate-on-link-text-subtitle "Teilnahme ist für alle, die den Link kennen, möglich!"
     :meeting/educate-on-edit "Titel ändern oder Agendapunkte editieren?"
     :meeting/educate-on-admin "Später jederzeit zum Admin-Center zurückkehren!"
     :meeting.admin-center/heading "Admin-Center"
     :meeting.admin-center/subheading "schnaq: \"%s\""
     :meeting.admin-center.edit.link/header "Zugang zum Admin-Center"
     :meeting.admin-center.edit.link/primer "Administration ist Arbeit, lassen Sie sich helfen!"
     :meeting.admin-center.edit.link/admin "Zugang zum Admin-Center per Mail"
     :meeting.admin-center.edit.link/admin-privilges "Editieren und Vorschläge verwalten"
     :meeting.admin-center.edit.link.form/label "E-Mail Adresse der Administrator:innen"
     :meeting.admin-center.edit.link.form/placeholder "Eine E-Mailadresse eingeben"
     :meeting.admin-center.edit.link.form/submit-button "Link verschicken"
     :meeting.admin-center.invite/via-link "Link verteilen"
     :meeting.admin-center.invite/via-mail "Per E-Mail einladen"
     :meeting/admin-center-tooltip "Schnaq administrieren"

     ;; Suggestions
     :suggestions.modal/header "Eingereichte Vorschläge"
     :suggestions.modal/primer "Einige TeilnehmerInnen haben Ihnen Vorschläge zu Ihrem schnaq gegeben."
     :suggestions.modal/primer-delete "Folgende Teilnehmer:innen schlagen die Löschung des Agendapunktes vor."
     :suggestions.modal.delete/button "Entgültig löschen"
     :suggestions.modal.table/nickname "Nickname"
     :suggestions.modal.table/suggestion-title "Titel"
     :suggestions.modal.table/suggestion-description "Beschreibung"
     :suggestions.modal.table/suggestion-accept "Übernehmen"
     :suggestions.modal.delete/title "Löschanfragen zu diesem Agendapunkt"
     :suggestions.modal.update/title "Änderungsvorschläge"
     :suggestions.modal.new/title "Vorschläge folgende neuen Agendapunkte hinzuzufügen"
     :suggestions.notification/title "Vorschläge eingereicht"
     :suggestions.notification/body "Ihre Vorschläge wurden erfolgreich verschickt!"
     :suggestions.update.agenda/success-title "Vorschlag übernommen"
     :suggestions.update.agenda/success-body "Der Vorschlag wurde übernommen und ist für alle TeilnehmerInnen sichtbar."
     :suggestions.agenda/delete-title "Agendapunkt gelöscht"
     :suggestions.agenda/delete-body "Der Agendapunkt wurde erfolgreich gelöscht"
     :suggestion.feedback/label "Zusätzliches Feedback"
     :suggestions.feedback/title "Feedback zum Meeting"
     :suggestions.feedback/primer "Folgendes Feedback wurde zu diesem Meeting im Vorfeld abgegeben."
     :suggestions.feedback.table/nickname "Nickname"
     :suggestions.feedback.table/content "Feedback"
     :suggestions.feedback/header "Freitext-Feedback"

     ;; Create Agenda
     :agenda/desc-for "Beschreibung für Agendapunkt "
     :agenda/point "Agendapunkt "
     :agenda.create/optional-agenda "Agenda hinzufügen"

     ;; Edit Agenda
     :agenda/edit-title "Schnaq editieren"
     :agenda/edit-subtitle "Beschreibung und Agendapunkte editieren"
     :agenda/edit-button "Änderungen speichern"

     :agendas.button/navigate-to-suggestions "Änderungsvorschläge erstellen"

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
     :discussion.badges/user-overview "Alle Teilnehmer:innen"
     :discussion.notification/new-content-title "Neuer Beitrag!"
     :discussion.notification/new-content-body "Ihr Beitrag wurde erfolgreich gespeichert."
     :discussion.carousel/heading "Beiträge Anderer"
     :discussion/discuss "Diskutieren"
     :discussion/discuss-tooltip "Diskutieren Sie mit anderen über diesen Agendapunkt."

     ;; meetings overview
     :meetings/header "Übersicht Ihrer schnaqs"
     :meetings/subheader "Auf diese schnaqs haben Sie Zugriff"

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

     :feedbacks.survey/primer
     [:<> "Wir würden uns freuen, wenn Sie bei einer
     kleinen Umfrage teilnehmen würden. Diese wird bei Google Forms gehostet
     und unterliegt den "
      [:a {:href "https://policies.google.com/privacy"} "Datenschutzbestimmungen von Google"]
      ". Mit der Teilnahme an der Umfrage akzeptieren Sie diese Datenschutzbestimmungen."]
     :feedbacks.survey/checkbox "Ja, ich möchte an der Umfrage teilnehmen"
     :feedbacks.survey/loading "Formular wird geladen..."
     :feedbacks.survey/tab "Umfrage"

     ;; login
     :login/as "Hallo, "
     :login/set-name "Geben Sie Ihren Namen ein"

     ;; analytics
     :analytics/heading "Analytics"
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
     :user.set-name.modal/header "Geben Sie einen Namen ein"
     :user.set-name.modal/primer "Der Name wird den anderen Teilnehmer:innen im schnaq angezeigt."

     ;; Errors
     :errors/navigate-to-startpage "Zurück zur Startseite"
     :errors/generic "Es ist ein Fehler aufgetreten"

     :error.generic/contact-us [:span "Sollten Sie hier landen nachdem Sie etwas auf schnaq.com angeklickt haben, geben Sie uns gerne Bescheid unter " [:a {:href "mailto:info@dialogo.io"} "info@dialogo.io"]]

     :error.404/heading "Diese Seite existiert nicht 🙉"
     :error.404/body "Die URL der Sie gefolgt sind existiert leider nicht. Möglicherweise hat sich ein Tippfehler
     oder ein Zeichen zu viel eingeschlichen."

     :error.403/heading "Sie haben nicht die Berechtigung diese Seite aufzurufen 🧙‍♂️"
     :error.403/body "Ihnen fehlt die Berechtigung diese Seite aufzurufen oder es handelt sich um einen Tippfehler in Ihrer URL."

     ;; Graph Texts
     :graph/heading "Diskussionsübersicht"
     :graph.button/text "Zeige Diskussionsgraphen an"

     ;; Route Link Texts
     :router/all-meetings "Alle schnaqs"
     :router/all-feedbacks "Alle Feedbacks"
     :router/create-meeting "Schnaq anlegen"
     :router/create-brainstorm "Brainstorm anlegen"
     :router/meeting-created "Zuletzt angelegter schnaq"
     :router/my-schnaqs "Meine schnaqs"
     :router/show-single-meeting "Schnaq anzeigen"
     :router/start-discussion "Starte Besprechung"
     :router/continue-discussion "Führe Besprechung fort"
     :router/startpage "Startseite"
     :router/how-to "Wie benutze ich schnaq?"
     :router/analytics "Analyse-Dashboard"
     :router/invalid-link "Fehlerseite"
     :router/true-404-view "404 Fehlerseite"
     :router/not-found-label "Not Found route redirect"
     :router/graph-view "Graph View"
     :router.features/meetings "Meeting Features"
     :router.features/discussion "Diskussionsfeatures"
     :router/pricing "Preise"}))


(defn img-path
  "Returns an image path as String for a given identifier"
  [identifier]
  (identifier
    {:animation-discussion "/animations/animation_discussion.webm"
     :animation-discussion-mp4 "/animations/animation_discussion.mp4"
     :elephant-admin "/imgs/elephants/admin.png"
     :elephant-erase "/imgs/elephants/erase.png"
     :elephant-share "/imgs/elephants/share.png"
     :elephant-stop "/imgs/elephants/stop.png"
     :elephant-talk "/imgs/elephants/talk.png"
     :how-to/taskbar "/imgs/howto/taskbar.svg"
     :icon-add "/imgs/buttons/add-button.svg"
     :icon-community "/imgs/community.svg"
     :icon-crane "/imgs/crane.svg"
     :icon-graph "/imgs/graph/graph-icon.svg"
     :icon-reports "/imgs/reports.svg"
     :icon-robot "/imgs/robot.svg"
     :logo "/imgs/Schnaq-Logo.svg"
     :logo-white "/imgs/Schnaq-Logo-White.svg"
     :schnaqqifant/original "/imgs/schnaqqifant.svg"
     :schnaqqifant/white "/imgs/schnaqqifant_white.svg"
     :startpage.features/meeting-organisation "/imgs/startpage/meeting_organisation_500px.png"
     :startpage.features/sample-discussion "/imgs/startpage/discussion_elearning.png"
     :startpage.features/discussion-graph "/imgs/startpage/discussion_graph_500px.png"
     :startpage.value-cards.discussion/image "/imgs/stock/discussion.jpeg"
     :startpage.value-cards.meetings/image "/imgs/stock/meeting.jpeg"
     :startpage.value-cards.knowledge/image "/imgs/stock/knowledge.jpeg"
     :feature.meetings/hero-image "/imgs/stock/meeting_landing_hero_500w.jpeg"
     :feature.meetings/schedule-meetings "/imgs/startpage/features/meeting-erstellen.png"
     :feature.discussions/hero-image "/imgs/stock/discussion_landing_hero.jpeg"
     :feature.discussions/create-discussion-spaces "/imgs/startpage/features/discussion-agendas.png"
     :feature.knowledge/hero-image "/imgs/stock/two_people_discussing_500w.jpg"
     :feature.knowledge/overview "/imgs/startpage/features/schnaqs-uebersicht_500w.png"
     :startpage.features/admin-center "/imgs/startpage/features/admin-center.png"}))

(defn video
  "Returns an video path"
  [identifier]
  (identifier
    {:how-to.why/webm "/animations/howto/Why.webm"
     :how-to.why/mp4 "/animations/howto/Why.mp4"
     :how-to.create/webm "/animations/howto/Create.webm"
     :how-to.create/mp4 "/animations/howto/Create.mp4"
     :how-to.agenda/webm "/animations/howto/Agenda.webm"
     :how-to.agenda/mp4 "/animations/howto/Agenda.mp4"
     :how-to.admin/webm "/animations/howto/Admin.webm"
     :how-to.admin/mp4 "/animations/howto/Admin.mp4"
     :start-page.work-together/webm "/animations/WorkTogether.webm"
     :start-page.work-together/mp4 "/animations/WorkTogether.mp4"}))

(defn fa
  "Returns an fontawesome icon id as String for a given identifier"
  [identifier]
  (identifier
    {:add "fa-plus-circle"
     :arrow-down "fa-arrow-down"
     :arrow-left "fa-arrow-left"
     :arrow-right "fa-arrow-right"
     :arrow-up "fa-arrow-up"
     :calendar "fa-calendar-plus"
     :carry "fa-people-carry"
     :cog "fa-cogs"
     :comment "fa-comments"
     :comment-alt "fa-comment-alt"
     :copy "fa-copy"
     :check "fa-check-square"
     :delete-icon "fa-times-circle"
     :edit "fa-edit"
     :eraser "fa-eraser"
     :flask "fa-flask"
     :heart "fa-heart"
     :laptop "fa-laptop-code"
     :shield "fa-shield-alt"
     :terminal "fa-terminal"
     :trash "fa-trash-alt"
     :users "fa-users"}))

(defn colors
  "Color definitions according to our css styles."
  [identifier]
  (identifier
    {:blue/dark "#052740"
     :blue/default "#1292ee"
     :blue/light "#4cacf4"
     :blue/selected "#0181dd"
     :orange/default "#ff772d"
     :orange/selected "#fe661e"
     :white "#ffffff"}))