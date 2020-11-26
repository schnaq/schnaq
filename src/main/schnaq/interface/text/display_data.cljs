(ns schnaq.interface.text.display-data
  "Texts used as labels in the whole application."
  (:require [schnaq.interface.config :refer [user-language]]
            [taoensso.tempura :refer [tr]]))

(def ^:private translations
  {:en {;; Common
        :common/save "Save"
        :common/language "Language"
        :error/export-failed "Export failed. Please try again later."

        ;; navbar labels
        :nav/startpage "Home"
        :nav/schnaqs "Schnaqs"
        :nav.schnaqs/show-all "All schnaqs"
        :nav.schnaqs/create-meeting "Prepare meeting"
        :nav.schnaqs/create-brainstorm "Create brainstorm"
        :nav.schnaqs/last-added "Last created schnaq"
        :nav-meeting-agenda "Create Agenda"
        :nav/blog "Blog"

        ;; Startpage
        :startpage/heading "Who needs whiteboards?"
        :startpage/subheading "Lost your train of thought again? Structure your teams discourse with schnaq!"

        :startpage.under-construction/heading "Ongoing Construction!"
        :startpage.under-construction/body "schnaq is currently in a free of charge beta phase. Your feedback is very important for further development!"

        :startpage.value-cards/heading "More applications!"
        :startpage.value-cards/lead "Structured discussions can have many applications. Take a look at a few examples."
        :startpage.value-cards.discussion/title "Lead a discussion"
        :startpage.value-cards.discussion/description "Discussing topics with multiple people over the Internet is hard. Its easy to
        feel lost. With schnaq you can structure your discussions and keep an eye on arguments and controversies with a single glance."
        :startpage.value-cards.discussion/alt-text "A symbolic representation of a speech-bubble"
        :startpage.value-cards.meetings/title "Optimize Meetings"
        :startpage.value-cards.meetings/description "Modern work got more distributed and we are finding ourselves jumping
        from one (digital) meeting to the next. Its hard to find time for proper preparation and debriefings.
        This is where schnaq helps you save time and streamline your meetings."
        :startpage.value-cards.meetings/alt-text "A group of people having a meeting."
        :startpage.value-cards.knowledge/title "Brainstorming and idea generation"
        :startpage.value-cards.knowledge/description "Use schnaq to let your team discuss and generate new ideas.
        An automatic Mindmap is generated from all statements. Furthermore, the statements are analyzed and summarized
        for all participants. Perfect for your next brainstorming session!"
        :startpage.value-cards.knowledge/alt-text "Several Sticky-Notes on a wall"
        :startpage.value-cards.button/text "More details"
        :startpage.usage/lead "What do I use schnaq for?"

        :startpage.features/more-information "More information"

        :startpage.demo.request/title "Request a demo"
        :startpage.demo.request/body "Do you want to know how you can boost innovation at your company?
        We will demonstrate schnaq personally. Just use the button on the left and we will get back to you as fast as possible."
        :startpage.demo.request/button "Request a demo now!"
        :startpage.demo.request.modal.name/label "Your name"
        :startpage.demo.request.modal.name/placeholder "My name"
        :startpage.demo.request.modal.email/label "E-Mail Address"
        :startpage.demo.request.modal.email/placeholder "my@email.com"
        :startpage.demo.request.modal.company/label "Company name"
        :startpage.demo.request.modal.company/placeholder "Company name, if any"
        :startpage.demo.request.modal.phone/label "Phone #"
        :startpage.demo.request.modal.phone/placeholder "0 1234 56789"
        :startpage.demo.request.send.notification/title "Request sent!"
        :startpage.demo.request.send.notification/body "We will get back to you as soon as possible."
        :startpage.demo.request.send.notification/failed-title "Request failed!"
        :startpage.demo.request.send.notification/failed-body "Something went wrong. Please check your input and try again."

        :startpage.features.meeting-organisation/lead "Work together"
        :startpage.features.meeting-organisation/title "Collaborative exchange of ideas"
        :startpage.features.meeting-organisation/body "Include all team members in the discussion and activate unused resources!
        This way you achieve have happy coworkers and a productive work environment.
        Brainstorming sessions are creative processes where the diverse members of your team can contribute their strengths
        and achieve wonderful results."
        :startpage.features.discussion/lead "Structured discussions"
        :startpage.features.discussion/title "Lasting exchange of thoughts"
        :startpage.features.discussion/body "Exchanges of ideas are an integral part of working in a creative team.
        But it is hard to understand after the fact, what everyone contributed and what the intention behind the process was.
        Through the structured capture of your coworkers statements, we can automatically generate graphical representations
        which help you understand what was said and done."
        :startpage.features.graph/lead "Simple overview"
        :startpage.features.graph/title "Mindmaps!"
        :startpage.features.graph/body "All your coworkers statements are automatically sorted into a mindmap.
        See all connections and statements neatly organized at one glance."

        ;; Meeting Feature Page
        :feature.meetings/lead "Structured meetings with schnaq"
        :feature.meetings/title "Activate coworkers, save time!"
        :feature.meetings/body "You can use schnaq to effectively prepare meetings, support all participants
        during the meeting and also for debriefing after the fact. This way you can activate all participants,
        save work time and produce results more efficiently."
        :feature.meetings.schedule/lead "Targeted preparation"
        :feature.meetings.schedule/title "Prepare the agenda collaboratively"
        :feature.meetings.schedule/body "You can start by preparing a first version of a meeting agenda.
        After you invite all meeting participants per mail or calendar invite, they can see the agenda, primers, one-pagers at one glance.
        They can also request to change, delete or add agenda points through schnaq and thus help you create a more productive meeting."
        :feature.meetings.discuss/lead "Discuss beforehand"
        :feature.meetings.discuss/title "Resolve controversial opinions before the meeting"
        :feature.meetings.discuss/body "Unclear points can be discussed beforehand to gain clarity.
        You can reach the discussion directly through the agenda. Use the discussion to clarify an agenda or to
        discuss things that do not belong in the meeting directly. The discussion is automatically structured and
        distributed. The participants do not need to be online at the same time. This way all coworkers are included.
        The result are laser-focused meetings."
        :feature.meetings.admin-center/lead "Not accounts needed"
        :feature.meetings.admin-center/title "Use schnaq without yet another account"
        :feature.meetings.admin-center/body "You can use schnaq solely through the secure links we generate.
        No need for extra accounts, all you need is a web-browser.
        Access schnaq from anywhere. There is also a special admin-link which lets you administer the meeting
        without any fuss."
        :feature.meetings/heading "Save work time by preparing your meetings with schnaq"
        :feature.meetings/features-subheading "Use schnaq for meetings"
        :feature.meetings/tbd-subheading "More to come…"
        :feature.meetings/tbd-lead "Become a pioneer and be the first to experience the upcoming features:"
        :feature.meetings.tbd/teams "Integration into MS Teams and Slack"
        :feature.meetings.tbd/outlook "Outlook export"
        :feature.meetings.tbd/protocols "Collaborative protocol generation during meetings"
        :feature.meetings.tbd/prereads "One pagers and pre-reads for agenda points, including check-ins"
        :feature.meetings.tbd/assignments "Assignment of duty per agenda"
        :feature.meetings.tbd/timeboxing "Timeboxing during the meeting"
        :feature.meetings.tbd/task-tracking "Transfer results into a task-tracker"
        :feature.meetings/feedback "Do you have more wishes or feature ideas? Contact us through the feedback form on the right side."}
   :de {;; Common
        :common/save "Speichern"
        :common/language "Sprache"
        :error/export-failed "Export hat nicht geklappt, versuchen Sie es später erneut."

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
        :startpage/heading "Wer braucht schon Whiteboards?"
        :startpage/subheading "Schon wieder den Faden verloren? Ideenaustausch besser strukturieren mit schnaq!"

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
        :meetings/share-calendar-invite "Kalendereinladung versenden"
        :meetings.suggestions/header "Vorschläge einreichen"
        :meetings.suggestions/subheader "Die erstellende Person kann die Vorschläge einsehen und berücksichtigen"

        :meeting.admin/addresses-label "E-Mail Adressen der Teilnehmer:innen"
        :meeting.admin/addresses-placeholder "E-Mail Adressen getrennt mit Leerzeichen oder Zeilenumbruch eingeben."
        :meeting.admin/addresses-privacy "Diese Adressen werden ausschließlich zum Mailversand genutzt und danach sofort von unseren Servern gelöscht."
        :meeting.admin/send-invites-button-text "Einladungen versenden"
        :meeting.admin/send-invites-heading "Laden Sie die Teilnehmer:innen per E-Mail ein"
        :meeting.admin/delete-statements-heading "Löschen Sie folgende Beiträge"
        :meeting.admin/statements-label "Statement-IDs, die gelöscht werden"
        :meeting.admin/statement-id-placeholder "Statement IDs getrennt mit Leerzeichen oder Zeilenumbruch eingeben."
        :meeting.admin/delete-statements-button-text "Beiträge endgültig löschen"
        :meeting.admin.notifications/emails-successfully-sent-title "Mail(s) verschickt!"
        :meeting.admin.notifications/emails-successfully-sent-body-text "Ihre Mail(s) wurden erfolgreich versendet."
        :meeting.admin.notifications/sending-failed-title "Fehler bei Zustellung!"
        :meeting.admin.notifications/sending-failed-lead "Die Einladung konnte an folgende Adressen nicht zugestellt werden: "
        :meeting.admin.notifications/statements-deleted-title "Nachrichten gelöscht!"
        :meeting.admin.notifications/statements-deleted-lead "Die von Ihnen gewählten Nachrichten wurden erfolgreich gelöscht."

        ;; Brainstorming time
        :brainstorm/heading "Brainstorm anlegen"
        :brainstorm.buttons/start-now "Jetzt ein Brainstorming starten"
        :brainstorm.create.button/save "Brainstorming starten!"

        ;; Privacy Page
        :privacy/heading "Was geschieht mit Ihren Daten?"
        :privacy/subheading "Wir erklären es Ihnen gerne!"
        :privacy.made-in-germany/lead "EU-Konformes Vorgehen"
        :privacy.made-in-germany/title "Datenschutz ist uns wichtig!"
        :privacy.made-in-germany/body "Das Entwicklerteam von schnaq besteht aus Informatiker:innen, die es Leid sind, dass mit Daten nicht sorgfältig umgegangen wird. Deshalb legen wir besonderen Wert darauf, DSGVO konform zu agieren und sämtliche Daten sicher auf deutschen Servern zu speichern. Kein Datenaustausch mit anderen Unternehmen, keine faulen Kompromisse!"
        :privacy.personal-data/lead "Welche Daten werden erhoben?"
        :privacy.personal-data/title "Persönliche Daten"
        :privacy.personal-data/body [:<> [:p "Standardmäßig werden nur technisch notwendige Daten erhoben. Es findet keine Auswertung über persönliche Daten statt und Ihr Verhalten auf unserer Website wird auch nur dann anonymisiert analysiert, wenn Sie dem zustimmen. "] [:p "Wenn Sie uns unterstützen wollen und der anonymisierten Analyse zustimmen, werden diese Daten mit Matomo erfasst und auf unseren Servern in Deutschland gespeichert. Matomo ist eine freie und selbstgehostete Alternative zu kommerziellen Anbietern. Wir geben keine Daten an Dritte damit weiter."] [:p [:button.btn.btn-outline-primary {:on-click #(.show js/klaro)} "Einstellungen prüfen"]]]
        :privacy.localstorage/lead "Welche Daten schicke ich an die Server?"
        :privacy.localstorage/title "Datenaustausch"
        :privacy.localstorage/body [:<> [:p "schnaq kann ganz auf Accounts verzichten. Es werden so keine Daten zu Ihnen auf unseren Servern gespeichert. Die meiste Interaktion findet über geteilte Links statt. Klicken Sie auf einen Link zu einem schnaq, wird ein Teil des Links (der Hash) in Ihrem Browser (im LocalStorage) abgespeichert. Besuchen Sie dann schnaq erneut, schickt Ihr Browser diesen Hash zurück an uns und erhält so erneut Zugang zum schnaq. Alternativ können Sie sich die Zugangslinks per E-Mail schicken lassen und halten so alle für den Betrieb notwendigen Daten selbst in der Hand."]
                                    [:p "Im Unterschied zu herkömmlichen Cookies verwenden wir den LocalStorage, welcher naturgemäß nur die wirklich notwendigen Daten von Ihnen an uns zurückschickt. Schauen Sie selbst, welche Daten das genau sind, indem Sie auf den Button klicken."]]
        :privacy.localstorage/show-data "Ihre Daten anzeigen"
        :privacy.localstorage.notification/title "Diese Daten hat Ihr Browser gespeichert"
        :privacy.localstorage.notification/body "Hinweis: \"Kryptische\" Zeichenketten sind die Zugangscodes zu Ihren schnaqs."
        :privacy.localstorage.notification/confirmation "Wollen Sie Ihre Daten wirklich löschen?"
        :privacy.localstorage.notification/delete-button "Daten löschen"
        :privacy.link-to-privacy/lead "Mehr Informationen finden Sie in unserer ausführlichen "
        :privacy.link-to-privacy/privacy "Datenschutzerklärung"

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
        :meeting.admin-center.edit.link/admin-privileges "Editieren und Vorschläge verwalten"
        :meeting.admin-center.edit.link.form/label "E-Mail Adresse der Administrator:innen"
        :meeting.admin-center.edit.link.form/placeholder "Eine E-Mailadresse eingeben"
        :meeting.admin-center.edit.link.form/submit-button "Link verschicken"
        :meeting.admin-center.invite/via-link "Link verteilen"
        :meeting.admin-center.invite/via-mail "Per E-Mail einladen"
        :meeting.admin-center.edit/administrate "Diskussion administrieren"
        :meeting/admin-center-export "Diskussion als Textdatei runterladen"
        :meeting/admin-center-tooltip "Schnaq administrieren"

        ;; Suggestions
        :suggestions.modal/header "Eingereichte Vorschläge"
        :suggestions.modal/primer "Einige TeilnehmerInnen haben Ihnen Vorschläge zu Ihrem schnaq gegeben."
        :suggestions.modal/primer-delete "Folgende Teilnehmer:innen schlagen die Löschung des Agendapunktes vor."
        :suggestions.modal.delete/button "Endgültig löschen"
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
        :discussion/create-argument-action "Beitrag hinzufügen"
        :discussion/create-argument-heading "Eigene Meinung abgeben / Informationen hinzufügen"
        :discussion/add-argument-conclusion-placeholder "Das denke ich darüber."
        :discussion/add-premise-supporting "Ich möchte die Aussage unterstützen"
        :discussion/add-premise-against "Ich habe einen Grund dagegen"
        :discussion/add-undercut "Die letzten beiden Aussagen passen nicht zusammen"
        :discussion/reason-nudge "Was denken Sie darüber?"
        :discussion/premise-placeholder "Ich denke..."
        :discussion/create-starting-premise-action "Beitrag hinzufügen"
        :discussion/others-think "Andere denken folgendes:"
        :discussion/undercut-bubble-intro "Der letzte Beitrag hat nichts mit dem vorherigen zu tun. Begründung:"
        :discussion.badges/user-overview "Alle Teilnehmer:innen"
        :discussion.badges/delete-statement "Beitrag löschen"
        :discussion.badges/delete-statement-confirmation "Wollen Sie den Beitrag wirklich löschen?"
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

        ;; Pricing Page
        :pricing.free-tier/description "Für kleine Teams und private Zwecke. Der Starter Plan ist der
     perfekte Einstieg in strukturierte Wissensgenerierung."
        :pricing.free-tier/beta-notice "Nach der Beta-Phase ist der Plan weiterhin verfügbar für bis zu 5 Nutzer:innen pro Team"
        :pricing.free-tier/call-to-action "Kostenfrei loslegen"
        :pricing.business-tier/description "Ob 10 oder 50 Nutzer:innen – der Preis ist der gleiche.
      Eignet sich für Unternehmen, Vereine, Bildungsinstitutionen und alle,
      die strukturiert Wissen sammeln möchten."
        :pricing.units/per-month "/ Monat"
        :pricing.notes/with-vat "zzgl. MwSt."
        :pricing.notes/yearly-rebate "Bei jährlicher Zahlweise im Voraus 15% Rabatt"
        :pricing.business-tier/call-to-action "Verfügbar ab 01.01.2021"
        :pricing.trial/call-to-action "30 Tage Business testen"
        :pricing.trial/description "Keine Kreditkarte nötig! Jederzeit kündbar."
        :pricing.trial.temporary/deactivation "Verfügbar ab 01.01.2021"
        :pricing.features/heading "Schnaq-Abonnement Vorteile"
        :pricing.features.user-numbers/heading "Unbegrenzte Teilnehmer:innen"
        :pricing.features.user-numbers/content "Lassen Sie so viele Mitarbeiter:innen, wie Sie möchten, kooperieren. *"
        :pricing.features.team-numbers/heading "Unbegrenzte Teams"
        :pricing.features.team-numbers/content "Die Anzahl der Teams, die Sie erstellen können, ist unlimitiert. *"
        :pricing.features.app-integration/heading "App-Integration"
        :pricing.features.app-integration/content "Verknüpfen Sie schnaq leicht mit Ihrem Slack, MS Teams, Confluence …"
        :pricing.features.analysis/heading "Automatische Analysen"
        :pricing.features.analysis/content "Die Beiträge werden automatisch analysiert und für alle Teilnehmer:innen aufbereitet."
        :pricing.features.knowledge-db/heading "Wissensdatenbank"
        :pricing.features.knowledge-db/content "Sammeln Sie erarbeitetes Wissen und Ideen an einem Ort."
        :pricing.features.mindmap/heading "Interaktive Mindmap"
        :pricing.features.mindmap/content "Alle Beiträge werden automatisch graphisch und interaktiv dargestellt."
        :pricing.features/disclaimer "* Gilt nur für Business-Abonnement"
        :pricing.competitors/per-month-per-user " € pro Monat pro Nutzer:in"
        :pricing.comparison/heading "Sie wachsen weiter – Sie sparen mehr!"
        :pricing.comparison/subheading "Egal wie groß Ihr Team wird, der Preis bleibt der Gleiche.
   So schlägt sich der Preis von schnaq im Vergleich zu Miro + Loomio + Confluence im Paket."
        :pricing.comparison.schnaq/price-point "79 € pro Monat für Ihr Unternehmen"
        :pricing.comparison.schnaq/brainstorm "Brainstorming"
        :pricing.comparison.schnaq/decision-making "Entscheidungsfindung"
        :pricing.comparison.schnaq/knowledge-db "Wissensdatenbank"
        :pricing.comparison.schnaq/async "Asynchrone Kommunikation"
        :pricing.comparison.schnaq/mindmap "Mindmapping"
        :pricing.comparison.schnaq/analysis "Ergebnisanalyse"
        :pricing.comparison.schnaq/flatrate " Flatrate im Monat"
        :pricing.comparison.schnaq/person-20 "79 € für 20 Personen"
        :pricing.comparison.schnaq/person-50 "79 € für 50 Personen"
        :pricing.comparison.schnaq/person-100 "79 € für 100 Personen …"
        :pricing.comparison/compared-to [:span "Verglichen" [:br] "mit"]
        :pricing.comparison.miro/description "Brainstorming Software"
        :pricing.comparison.loomio/description "Kooperative Entscheidungsfindung"
        :pricing.comparison.confluence/description "Wissensdatenbank"
        :pricing.comparison.competitor/person-10 " im Monat für 10 Personen"
        :pricing.comparison.competitor/person-20 "247 € für 20 Personen"
        :pricing.comparison.competitor/person-50 "685 € für 50 Personen"
        :pricing.comparison.competitor/person-100 "1370 € für 100 Personen …"
        :pricing.faq/heading "Häufig gestellte Fragen zu schnaq Abos"
        :pricing.faq.terminate/heading "Kann ich jederzeit kündigen?"
        :pricing.faq.terminate/body
        [:<> [:span.text-primary "Ja! "] "Sie können" [:span.text-primary " jeden Monat"] " kündigen,
     wenn Sie die monatliche Zahlweise gewählt haben. Wenn Sie die jährliche Zahlweise
     wählen, können Sie zum Ablauf des Abonnementjahres kündigen."]
        :pricing.faq.extra-price/heading "Muss ich für mehr Leute extra bezahlen?"
        :pricing.faq.extra-price/body
        [:<> [:span.text-primary "Nein, "] "Sie können" [:span.text-primary " beliebig viele Personen "]
         " zu Ihrer Organisation hinzufügen. Jedes Unternehmen, Verein,
         Bildungseinrichtung, usw. braucht " [:span.text-primary "nur ein Abonnement."]]
        :pricing.faq.trial-time/heading "Verlängert sich der Testzeitraum automatisch?"
        :pricing.faq.trial-time/body
        [:<> [:span.text-primary "Nein, "] "wenn ihr Testzeitraum endet, können Sie" [:span.text-primary " aktiv entscheiden"]
         ", ob Sie Zahlungsdaten hinzufügen und weiter den Business-Tarif nutzen möchten.
         Der " [:span.text-primary "Starter Plan bleibt unbegrenzt kostenfrei"] ", auch nach dem Testzeitraum."]
        :pricing.faq.longer-trial/heading "Kann ich den Business-Tarif länger testen?"
        :pricing.faq.longer-trial/body
        [:<> [:span.text-primary "Ja! "] "Schreiben Sie uns einfach eine " [:span.text-primary " E-Mail"] " an "
         [:a {:href "mailto:info@schnaq.com"} "info@schnaq.com."]]
        :pricing.faq.privacy/heading "Wer hat Zugriff auf meine Daten?"
        :pricing.faq.privacy/body-1
        [:<> "Jede Person, die Sie Ihrem Unternehmen hinzufügen, kann potentiell auf die hinterlegten Daten zugreifen."
         "Technisch werden Ihre Daten vollständig sicher auf"
         [:span.text-primary " deutschen Servern und DSGVO konform"] " abgespeichert. Auf unserer "]
        :pricing.faq.privacy/body-2 "Seite zur Datensicherheit"
        :pricing.faq.privacy/body-3 " finden Sie mehr Informationen"
        :pricing/headline "Schnaq Abonnement"
        :pricing.newsletter/lead "Werden Sie sofort informiert, wenn das Abonnement live geht: "
        :pricing.newsletter/name "DisqTec Newsletter."

        ;; Route Link Texts
        :router.features/discussion "Diskussionsfeatures"
        :router.features/meetings "Meeting Features"
        :router/all-feedbacks "Alle Feedbacks"
        :router/all-meetings "Alle schnaqs"
        :router/analytics "Analyse-Dashboard"
        :router/continue-discussion "Führe Besprechung fort"
        :router/create-brainstorm "Brainstorm anlegen"
        :router/create-meeting "Schnaq anlegen"
        :router/graph-view "Graph View"
        :router/how-to "Wie benutze ich schnaq?"
        :router/invalid-link "Fehlerseite"
        :router/meeting-created "Zuletzt angelegter schnaq"
        :router/my-schnaqs "Meine schnaqs"
        :router/not-found-label "Not Found route redirect"
        :router/pricing "Preise"
        :router/privacy "Datenschutz"
        :router/show-single-meeting "Schnaq anzeigen"
        :router/start-discussion "Starte Besprechung"
        :router/startpage "Startseite"
        :router/true-404-view "404 Fehlerseite"}})

(defn labels
  "Get a localized resource for the requested key. Returns either a string or a hiccup
  element. Optionally tempura parameters can be passed."
  [resource-key & params]
  (let [default-lang :de]
    (tr {:dict translations} [@user-language default-lang] [resource-key] (vec params))))

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
     :privacy/made-in-germany "/imgs/privacy/shield.jpg"
     :schnaqqifant/original "/imgs/schnaqqifant.svg"
     :schnaqqifant/white "/imgs/schnaqqifant_white.svg"
     :startpage.features/meeting-organisation "/imgs/startpage/meeting_organisation_500px.png"
     :startpage.features/sample-discussion "/imgs/startpage/discussion_elearning.png"
     :startpage.features/discussion-graph "/imgs/startpage/discussion_graph_500px.png"
     :startpage.value-cards.discussion/image "/imgs/stock/discussion.jpeg"
     :startpage.value-cards.meetings/image "/imgs/stock/meeting.jpeg"
     :startpage.value-cards.knowledge/image "/imgs/stock/knowledge.jpeg"
     :pricing.others/miro "imgs/startpage/pricing/miro.png"
     :pricing.others/loomio "imgs/startpage/pricing/loomio.png"
     :pricing.others/confluence "imgs/startpage/pricing/confluence.jpeg"
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
     :check/double "fa-check-double"
     :check/normal "fa-check"
     :check/square "fa-check-square"
     :cog "fa-cogs"
     :comment "fa-comments"
     :comment-alt "fa-comment-alt"
     :cookie/bite "fa-cookie-bite"
     :cookie/complete "fa-cookie"
     :copy "fa-copy"
     :delete-icon "fa-times-circle"
     :edit "fa-edit"
     :eraser "fa-eraser"
     :file-download "fa-file-download"
     :flag "fa-flag"
     :flask "fa-flask"
     :heart "fa-heart"
     :language "fa-language"
     :laptop "fa-laptop-code"
     :newspaper "fa-newspaper"
     :shield "fa-shield-alt"
     :terminal "fa-terminal"
     :trash "fa-trash-alt"
     :user/group "fa-users"
     :user/lock "fa-user-lock"
     :user/shield "fa-user-shield"}))

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