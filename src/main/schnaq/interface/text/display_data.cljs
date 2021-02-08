(ns schnaq.interface.text.display-data
  "Texts used as labels in the whole application."
  (:require [schnaq.interface.config :refer [user-language]]
            [taoensso.tempura :refer [tr]]))

(def ^:private translations
  {:en {;; Common
        :common/language "Language"
        :common/history "History"
        :error/export-failed "Export failed. Please try again later."

        ;; navbar labels
        :nav/startpage "Home"
        :nav/schnaqs "schnaqs"
        :nav.schnaqs/show-all "All schnaqs"
        :nav.schnaqs/show-all-public "All public schnaqs"
        :nav.schnaqs/create-schnaq "Create schnaq"
        :nav.schnaqs/last-added "Last created schnaq"
        :nav/blog "Blog"

        ;; code of conduct
        :coc/heading "Code of Conduct"
        :coc/subheading "Do onto others as you would have them do unto you"

        :coc.users/lead "Behaviour towards other users"
        :coc.users/title "Respect and Non-Discrimination"
        :coc.users/body "A respectful behaviour is important and is the basis of each factual discussion. This applies not only offline but also online.\nIt is important to us that all users can express themselves without being discriminated against based on their person, origin or views. \nPosts that do not adhere to these guidelines will be deleted."

        :coc.content/lead "Content"
        :coc.content/title "We obey the law, please do that too"
        :coc.content/body "We comply with German law; this applies especially to data protection, equality and non-discrimination.\nContent that violates applicable law will be deleted."


        ;; Startpage
        :startpage/heading "Your personal discussion hub"
        :startpage/subheading "discuss, participate, decide"

        :startpage.usage/lead "What do I use schnaq for?"

        :startpage.call-to-action/discuss-spotlight-topics "Current schnaqs for you"

        :startpage.features/more-information "More information"

        :startpage.features.meeting-organisation/lead "Collaborate"
        :startpage.features.meeting-organisation/title "What do others think about?"
        :startpage.features.meeting-organisation/body "Your team is not on the same page? Does your club need to make a hard decision?
        Or do you just want to know what others think on a topic important to you? Create your discussion with schnaq and get on it."
        :startpage.features.discussion/lead "Structured Discussions"
        :startpage.features.discussion/title "The loudest voice isn't always right"
        :startpage.features.discussion/body "You know the drill. There is always someone stealing all the attention. With schnaq
        you can easily see the contributions of everyone. Easily address the things that you want to talk about and ignore the rest."
        :startpage.features.graph/lead "Clear Visualization"
        :startpage.features.graph/title "Never lose track"
        :startpage.features.graph/body "A mindmap is automatically created for every discussion.
        This way it's easy for you to keep track of things.
        Did something interesting catch your attention? Just double click to jump right in through the mindmap."

        :startpage.early-adopter/title "Gotten curious?"
        :startpage.early-adopter/body "Browse through public discussions"
        :startpage.early-adopter.buttons/join-schnaq "Public schnaqs"
        :startpage.early-adopter/or "or"

        :footer.buttons/about-us "About us"
        :footer.buttons/legal-note "Legal note"
        :footer.buttons/privacy "Privacy Notice"
        :footer.tagline/developed-with " Developed with "

        ;; Create schnaqs
        :schnaqs/create "Create schnaq"

        ;; Header image
        :schnaq.header-image.url/placeholder "Image url"
        :schnaq.header-image.url/button "Add as preview image"
        :schnaq.header-image.url/note "Images are restricted to content from pixabay.com"
        :schnaq.header-image.url/label "Add a preview image to your schnaq header"
        :schnaq.header-image.url/successful-set "Preview image successfully set"
        :schnaq.header-image.url/successful-set-body "The image will be featured in this schnaq's header."
        :schnaq.header-image.url/failed-setting-title "Error when adding image"
        :schnaq.header-image.url/failed-seting-body "The image will not be used as preview image."

        ;; Create schnaq
        :schnaq.create.input/placeholder "What should the name of your schnaq be?"
        :meeting/copy-link-tooltip "Click here to copy your link"
        :meeting/link-copied-heading "Link copied"
        :meeting/link-copied-success "The link was copied to your clipboard!"
        :schnaq/created-success-heading "Your schnaq was created!"
        :schnaq/created-success-subheading "Distribute your personal share-link or invite participants via email 🎉"
        :schnaqs/continue-with-schnaq-after-creation "Invited Everybody? Lets go!"
        :schnaqs/continue-to-schnaq-button "To the schnaq"

        :meeting.admin/addresses-label "Email addresses of the participants"
        :meeting.admin/addresses-placeholder "Email addresses separated by a newline or space."
        :meeting.admin/addresses-privacy "These addresses are only used to send the invitation emails and are deleted from our
        servers immediately afterwards."
        :meeting.admin/send-invites-button-text "Send invitations"
        :meeting.admin/send-invites-heading "Invite participants via email"
        :meeting.admin/delete-statements-heading "Delete the following statements"
        :meeting.admin/statements-label "Statement-IDs, that will be deleted"
        :meeting.admin/statement-id-placeholder "Statement IDs separated by a space or newline."
        :meeting.admin/delete-statements-button-text "Delete statements immediately"
        :meeting.admin.notifications/emails-successfully-sent-title "Mails sent!"
        :meeting.admin.notifications/emails-successfully-sent-body-text "Your invitations were sent successfully."
        :meeting.admin.notifications/sending-failed-title "Error during mail delivery!"
        :meeting.admin.notifications/sending-failed-lead "The following invitations could not be delivered: "
        :meeting.admin.notifications/statements-deleted-title "Statements deleted!"
        :meeting.admin.notifications/statements-deleted-lead "The statements you entered have been deleted."

        ;; Brainstorming time
        :schnaq.create/heading "Start schnaq"
        :brainstorm.buttons/start-now "Start your discussion"
        :schnaq.create.button/save "Start a new schnaq!"

        ;; Discussion Creation
        :discussion.create.public-checkbox/label "Make this discussion public"

        :discussion.privacy/public "Public Discussion"
        :discussion.privacy/private "Private Discussion"

        ;; Privacy Page
        :privacy/heading "What happens to your data?"
        :privacy/subheading "We lead you through it step by step!"
        :privacy.made-in-germany/lead "EU-regulation conformity"
        :privacy.made-in-germany/title "Data privacy is important to us!"
        :privacy.made-in-germany/body "The development team of schnaq consists of developers that are tired of misuse of private
        data. This is why we take special care to be GDPR compliant and to save all data securely on german servers.
        We do not exchange any data with other companies without absolute need and making it completely clear."
        :privacy.personal-data/lead "Which data is saved?"
        :privacy.personal-data/title "Personal Data"
        :privacy.personal-data/body
        [:<> [:p "Per default we only save data that is needed to operate the service. There is no analysis of personal data, and anonymous data of your behavior on our website is only collected, when you explicitly allow us to do so. "]
         [:p "If you want to support us and allow the analysis, we collect the data with Matomo and save it on our german servers. Matomo is a free and self-hosted alternative to commercial options for website analytics . We do not exchange this data with third parties."] [:p [:button.btn.btn-outline-primary {:on-click #(.show js/klaro)} "Check your settings"]]]
        :privacy.localstorage/lead "What data do I send to the server?"
        :privacy.localstorage/title "Data Exchange"
        :privacy.localstorage/body
        [:<> [:p "schnaq has no need for accounts. This way no personal data about you is saved on the server. Most of the interactions work through links. When you click on a link a part of it (the so called hash) is stored in your browser (in the localStorage). As soon as you go to schnaq.com again, your browser sends this hash back and you gain access to your created schnaqs. Alternatively you can send the links to yourself via email. This way you have all the data in your own hands."]
         [:p "In difference to website-cookies we use the localStorage, which only saves data that is needed for the application to function for you.
         You can see the data in your localStorage by clicking the following button."]]
        :privacy.localstorage/show-data "Show my data"
        :privacy.localstorage.notification/title "This data is saved by your browser"
        :privacy.localstorage.notification/body "Tip: \"Cryptic\" strings of characters are the codes that allow you to view schnaqs."
        :privacy.localstorage.notification/confirmation "Do you really want to delete the data?"
        :privacy.localstorage.notification/delete-button "Delete data"
        :privacy.link-to-privacy/lead "More information can be found in the comprehensive "
        :privacy.link-to-privacy/privacy "Privacy notice"

        ;; schnaqs not found
        :schnaqs.not-found/alert-lead "Unfortunately, no schnaqs were found."
        :schnaqs.not-found/alert-body "Invite people to your first schnaq after you created it."

        ;; Admin Center
        :meeting/educate-on-link-text "Share the link below with your coworkers."
        :meetings/educate-on-link-text-subtitle "Everybody with possession of the link can participate."
        :meeting/educate-on-edit "Want to change the name or description?"
        :meeting/educate-on-admin "Go back to the admin center at any time!"
        :schnaq.admin-center/heading "Admin-Center"
        :schnaq.admin-center/subheading "schnaq: \"%s\""
        :meeting.admin-center.edit.link/header "Entry to the admin-center"
        :meeting.admin-center.edit.link/primer "Administration takes work, let others help!"
        :meeting.admin-center.edit.link/admin "Entry to Admin-Center via Email"
        :meeting.admin-center.edit.link/admin-privileges "Edit and administer suggestions"
        :meeting.admin-center.edit.link.form/label "Email address of the administrators"
        :meeting.admin-center.edit.link.form/placeholder "Enter an email address"
        :meeting.admin-center.edit.link.form/submit-button "Send link"
        :meeting.admin-center.invite/via-link "Distribute Link"
        :meeting.admin-center.invite/via-mail "Invite via Email"
        :meeting.admin-center.edit/administrate "Administrate Discussion"
        :meeting/admin-center-export "Download discussion as a text-file"
        :meeting/admin-center-tooltip "Administrate schnaq"

        ;; Discussion Language
        :discussion/create-argument-action "Add Statement"
        :discussion/add-argument-conclusion-placeholder "I think that…"
        :discussion/add-premise-supporting "I want to support the statement"
        :discussion/add-premise-against "I disagree…"
        :discussion.add.button/support "Support"
        :discussion.add.button/attack "Attack"
        :discussion.badges/user-overview "All participants"
        :discussion.badges/delete-statement "Delete statement"
        :discussion.badges/delete-statement-confirmation "Do you really want to delete the statement?"
        :discussion.notification/new-content-title "New statement!"
        :discussion.notification/new-content-body "Your statement was added successfully!"

        ;; meetings overview
        :schnaqs/header "Overview of your schnaqs"
        :schnaqs/subheader "These are the schnaqs that you are part of"
        :schnaqs.all/header "Public schnaqs"

        ;; Feedbacks
        :feedbacks.overview/header "Feedbacks"
        :feedbacks.overview/subheader "All feedbacks"
        :feedbacks.overview/description "Description"
        :feedbacks.overview/contact-name "From"
        :feedbacks.overview/contact-mail "E-Mail"
        :feedbacks/button "Feedback"
        :feedbacks/screenshot "Screenshot"
        :feedbacks.modal/primer "Feedback is important! we are happy about any kind of feedback. 🥳
        Leave us a comment and thereby help to improve this software. Thank you and dankeschön!"
        :feedbacks.modal/contact-name "Your Name"
        :feedbacks.modal/contact-mail "Email Address"
        :feedbacks.modal/description "Your Feedback"
        :feedbacks.modal/optional "Optional"
        :feedbacks.modal/screenshot "Add screenshot?"
        :feedbacks.modal/disclaimer "Your data is only saved on our german servers and not exchanged with any third party."
        :feedbacks.notification/title "Thank you for your feedback!"
        :feedbacks.notification/body "Your feedback reached us safely 🎉"

        :feedbacks.survey/primer
        [:<> "We would be extremely happy if you could participate in a small survey.
        The survey is hosted through Google Forms. Find their  "
         [:a {:href "https://policies.google.com/privacy"} "privacy policy here."]
         " By participating, you accept their privacy policy."]
        :feedbacks.survey/checkbox "Yes, I want to participate in the survey."
        :feedbacks.survey/loading "The form is being loaded…"
        :feedbacks.survey/tab "Survey"

        ;; login
        :login/as "Hello, "
        :login/set-name "Enter your name"

        ;; analytics
        :analytics/heading "Analytics"
        :analytics/overall-meetings "schnaqs created"
        :analytics/user-numbers "Usernames created"
        :analytics/average-agendas-title "Average number of agendas / schnaq"
        :analytics/statements-num-title "# of statements"
        :analytics/active-users-num-title "Active users"
        :analytics/statement-lengths-title "Length of statements"
        :analytics/argument-types-title "Argument types"
        :analytics/last-meeting-created-title "Last meeting created at"
        :analytics/fetch-data-button "Retrieving data…"

        ;; Supporters
        :supporters/heading "Supported by the Ministry of Economics of the State of North Rhine-Westphalia"

        ;; Testimonials
        :testimonials/heading "Testimonials"
        :testimonials.doctronic/quote "We observe the development of schnaq with great interest for our own use and for the use of our customers."
        :testimonials.doctronic/author "Ingo Küper, Managing Director doctronic GmbH & Co. KG"

        ;; User related
        :user.button/set-name "Save name"
        :user.button/set-name-placeholder "Your name"
        :user.button/change-name "Change name"
        :user.button/success-body "Name saved successfully"
        :user.set-name/dialog-header "Hello 👋"
        :user.set-name/dialog-lead "Good to see you!"
        :user.set-name/dialog-body "To be able to participate in discussions, enter a name."
        :user.set-name/dialog-button "How do you want to be called?"
        :user.set-name.modal/header "Please, enter a name"
        :user.set-name.modal/primer "The name will be visible to other participants of the schnaq."
        :user/login "Login / Sign up"
        :user/logout "Logout"
        :user.profile/settings "Settings"
        :user.profile/star-tooltip "You're an admin!\n\"With great power comes great responsibility.\""

        ;; Errors
        :errors/navigate-to-startpage "Back to the home page"
        :errors/generic "An error occurred"

        :error.generic/contact-us
        [:<> "Did you end up here after clicking something on schnaq.com? Give us a hint at " [:a {:href "mailto:info@schnaq.com"} "info@schnaq.com"]]

        :error.404/heading "This site does not exist 🙉"
        :error.404/body "The URL that you followed does not exist. Maybe there is a typo."

        :error.403/heading "You do not have the rights to view this site 🧙‍♂️"
        :error.403/body "You either have insufficient rights to view this site, or a typo happened."

        ;; Graph Texts
        :graph/heading "Discussion Overview"
        :graph.button/text "Mindmap"
        :graph/download-png "Download mindmap as image"

        ;; Pricing Page
        :pricing.free-tier/description "For small teams and private parties. The starter plan is the perfect entry
         into structured idea generation."
        :pricing.free-tier/beta-notice "This plan will be still available after the beta-phase for teams of up to 5 members."
        :pricing.free-tier/call-to-action "Start free of charge"
        :pricing.business-tier/description "Whether 10 or 50 members – the price stays the same.
      Perfect for companies, clubs, educational institutions and everyone looking to structure their knowledge."
        :pricing.units/per-month "/ month"
        :pricing.notes/with-vat "incl. VAT"
        :pricing.notes/yearly-rebate "15% discount when paid yearly in advance"
        :pricing.business-tier/call-to-action "Available from 01.01.2021"
        :pricing.trial/call-to-action "Test business 30 days free of charge"
        :pricing.trial/description "No credit card needed! Cancel anytime."
        :pricing.trial.temporary/deactivation "Available from 01.01.2021"
        :pricing.features/heading "Schnaq subscription advantages"
        :pricing.features.user-numbers/heading "Unlimited member accounts"
        :pricing.features.user-numbers/content "Set no bounds for how many people can collaborate. *"
        :pricing.features.team-numbers/heading "Unlimited number of teams"
        :pricing.features.team-numbers/content "Create as many teams as are needed for your projects. *"
        :pricing.features.app-integration/heading "App integration"
        :pricing.features.app-integration/content "Connect schnaq easily to your Slack, MS Teams, Confluence …"
        :pricing.features.analysis/heading "Automatic Analyses"
        :pricing.features.analysis/content "All discussions are automatically analyzed and presented to the participants."
        :pricing.features.knowledge-db/heading "Knowledge Database"
        :pricing.features.knowledge-db/content "Collect all your knowledge in one spot."
        :pricing.features.mindmap/heading "Interactive mindmap"
        :pricing.features.mindmap/content "All statements are automatically structured and shown in an interactive mindmap."
        :pricing.features/disclaimer "* Applies only to business plan"
        :pricing.competitors/per-month-per-user " € per month per user"
        :pricing.comparison/heading "You continue growing – you continue saving!"
        :pricing.comparison/subheading "No matter how much your team grows, the price stays the same.
   Take a look how the price of schnaq compares to Miro + Loomio + Confluence together."
        :pricing.comparison.schnaq/price-point "79 € per month for your company"
        :pricing.comparison.schnaq/brainstorm "Brainstorming"
        :pricing.comparison.schnaq/decision-making "Decision making"
        :pricing.comparison.schnaq/knowledge-db "Knowledge database"
        :pricing.comparison.schnaq/async "Asynchronous communication"
        :pricing.comparison.schnaq/mindmap "Mindmapping"
        :pricing.comparison.schnaq/analysis "Result analysis"
        :pricing.comparison.schnaq/flatrate " Flat per Month"
        :pricing.comparison.schnaq/person-20 "79 € for 20 users"
        :pricing.comparison.schnaq/person-50 "79 € for 50 users"
        :pricing.comparison.schnaq/person-100 "79 € for 100 users …"
        :pricing.comparison/compared-to [:<> "Compared" [:br] "to"]
        :pricing.comparison.miro/description "Brainstorming software"
        :pricing.comparison.loomio/description "Cooperative decision making"
        :pricing.comparison.confluence/description "Knowledge database"
        :pricing.comparison.competitor/person-10 " per month for 10 users"
        :pricing.comparison.competitor/person-20 "247 € for 20 users"
        :pricing.comparison.competitor/person-50 "685 € for 50 users"
        :pricing.comparison.competitor/person-100 "1370 € for 100 users …"
        :pricing.faq/heading "Frequently asked questions regarding schnaq subscriptions"
        :pricing.faq.terminate/heading "Can I cancel anytime?"
        :pricing.faq.terminate/body
        [:<> [:span.text-primary "Yes! "] "You are able to cancel" [:span.text-primary " every month"] ",
     if you are paying monthly. If you are paying yearly, you can cancel at the end of the subscription year."]
        :pricing.faq.extra-price/heading "Do I need to pay extra for more seats?"
        :pricing.faq.extra-price/body
        [:<> [:span.text-primary "No, "] "you can add" [:span.text-primary " unlimited accounts "]
         " to your organization. Every company, club,
         educational institution, etc. only needs " [:span.text-primary "one subscription."]]
        :pricing.faq.trial-time/heading "Is my trial-subscription automatically converted into a paid one?"
        :pricing.faq.trial-time/body
        [:<> [:span.text-primary "No, "] "when your trial ends, you can" [:span.text-primary " actively decide"]
         ", whether you want to add payment data and continue using the business plan.
         The " [:span.text-primary "starter plan stays free of charge"] ", after a possible trial."]
        :pricing.faq.longer-trial/heading "Can I trial the business plan a little bit longer?"
        :pricing.faq.longer-trial/body
        [:<> [:span.text-primary "Sure! "] "Simply write us an " [:span.text-primary " Email"] " at "
         [:a {:href "mailto:info@schnaq.com"} "info@schnaq.com."]]
        :pricing.faq.privacy/heading "Who can access my data?"
        :pricing.faq.privacy/body-1
        [:<> "Any Person who is added to your organization and to corresponding teams can see their data."
         "From a technical standpoint the data is securely saved on"
         [:span.text-primary " german Servers in a GDPR compliant"] " manner. See the "]
        :pricing.faq.privacy/body-2 "Privacy notice page"
        :pricing.faq.privacy/body-3 " for more information."
        :pricing/headline "Schnaq subscription"
        :pricing.newsletter/lead "Subscribe to the newsletter and be informed as soon as the plans go live: "
        :pricing.newsletter/name "DisqTec newsletter."

        ;; feature list
        :feature/what "One step to start a discussion"
        :feature/share "Invite friends – they don't need accounts"
        :feature/participate "Find out what others think"
        :feature/graph "Automatically generated mindmaps"
        :feature/private-public "Decide who gets to see your discussion"
        :feature/secure "Your data is stored securely on EU servers"

        ;; tooltips
        :tooltip/history-statement "Back to statement made by "

        ;; History
        :history.home/text "Start"
        :history.home/tooltip "Back to the discussion's beginning"
        :history.statement/user "Post from "
        :history.all-schnaqs/text "Overview"
        :history.all-schnaqs/tooltip "Back to all schnaqs"
        :history.back/text "Back"
        :history.back/tooltip "Back to previous post"

        ;; Route Link Texts
        :router.features/discussion "Discussion features"
        :router/admin-center "Admin-Center"
        :router/all-feedbacks "All feedbacks"
        :router/all-meetings "All schnaqs"
        :router/analytics "Analytics dashboard"
        :router/continue-discussion "Continue Discussion"
        :router/create-schnaq "Create schnaq"
        :router/graph-view "Graph view"
        :router/how-to "How do I use schnaq?"
        :router/invalid-link "Error page"
        :router/meeting-created "Last created schnaq"
        :router/my-schnaqs "My schnaqs"
        :router/not-found-label "Not found route redirect"
        :router/pricing "Prices"
        :router/privacy "Privacy policy"
        :router/show-single-meeting "Show schnaq"
        :router/start-discussion "Start discussion"
        :router/startpage "Startpage"
        :router/true-404-view "404 error page"
        :router/public-discussions "Public schnaqs"

        :admin.center.start/title "Admin Center"
        :admin.center.start/heading "Admin Center"
        :admin.center.start/subheading "Administrate schnaqs as a superuser"
        :admin.center.delete/confirmation "Do you really want to delete this schnaq?"
        :admin.center.delete.public/label "Public schnaqs"
        :admin.center.delete.public/button "Delete schnaq"
        :admin.center.delete/heading "Deletion"
        :admin.center.delete.public/heading "Public schnaqs"
        :admin.center.delete.private/label "Share-hash"
        :admin.center.delete.private/heading "Private schnaqs"}
   :de {;; Common
        :common/language "Sprache"
        :common/history "Verlauf"
        :error/export-failed "Export hat nicht geklappt, versuchen Sie es später erneut."

        ;; navbar labels
        :nav/startpage "Home"
        :nav/schnaqs "schnaqs"
        :nav.schnaqs/show-all "Alle schnaqs"
        :nav.schnaqs/show-all-public "Alle öffentlichen schnaqs"
        :nav.schnaqs/create-schnaq "schnaq anlegen"
        :nav.schnaqs/last-added "Zuletzt angelegter schnaq"
        :nav/blog "Zum Blog"

        ;; code of conduct
        :coc/heading "Verhaltensregeln"
        :coc/subheading "Unsere Benimmregeln"

        :coc.users/lead "Verhalten gegenüber anderen Nutzer:innen"
        :coc.users/title "Respektvoller Umgang und Nichtdiskriminierung"
        :coc.users/body "Ein respektvoller Umgang ist wichtig, um miteinander leben zu können und bietet die Grundlage für sachliche Diskussionen. Dies gilt nicht nur offline sondern auch online. \nUns ist es wichtig, dass sich jede:r Nutzer:in ausdrücken kann, ohne aufgrund ihrer Person, Herkunft oder Ansichten diskriminiert zu werden. \nBeiträge, die sich nicht an diese Richtlinien halten, werden entfernt."

        :coc.content/lead "Inhalte"
        :coc.content/title "Wir halten uns an das Gesetz, bitte tut das auch"
        :coc.content/body "Wir halten das Deutsche Grundgesetz ein; dies gilt auch und insbesondere für Datenschutz, Gleichberechtigung und Nichtdiskriminierung.\nInhalte, die gegen geltendes Recht verstoßen, werden von uns gelöscht."

        ;; Startpage
        :startpage/heading "Deine Online-Diskussionsplattform"
        :startpage/subheading "Diskussionen, Online-Partizipationen, Entscheidungsfindungen"

        :startpage.call-to-action/discuss-spotlight-topics "Aktuelle schnaqs für dich"
        :startpage.usage/lead "Wofür kann ich schnaq verwenden?"
        :startpage.features/more-information "Mehr Informationen"

        :startpage.features.meeting-organisation/lead "Kollaborativer Meinungsaustausch"
        :startpage.features.meeting-organisation/title "Was denken andere?"
        :startpage.features.meeting-organisation/body "Es gibt Uneinigkeiten im Team? Dein Verein muss eine Entscheidung treffen?
        Oder möchtest du einfach wissen was andere Menschen zu Themen denken, die dir wichtig sind? Erstelle eine Diskussion mit schnaq
        und lege direkt los."
        :startpage.features.discussion/lead "Strukturierte Diskussionen"
        :startpage.features.discussion/title "Wer am lautesten schreit, hat nicht immer Recht"
        :startpage.features.discussion/body "Der Gruppenchat bimmelt seit 30 Minuten. Die Nachrichten werden immer mehr. Die Lust zu lesen immer weniger. Nur drei Nachrichten waren interessant.
        Mit schnaq siehst du einfach strukturiert die Beiträge aller Teilnehmer:innen.
        Gehe schnell auf die Beiträge ein, die dich interessieren."
        :startpage.features.graph/lead "Übersichtliche Darstellung"
        :startpage.features.graph/title "Verliere nie wieder den Überblick"
        :startpage.features.graph/body [:span "Zu jeder Diskussion wird automatisch eine Mindmap erstellt. So hast du jederzeit den Überblick. Dir ist etwas interessantes aufgefallen?" [:br] " Spring über die Mindmap per Doppelklick direkt in die Diskussion."]

        :startpage.early-adopter/title "Neugierig geworden?"
        :startpage.early-adopter/body "Stöbere durch öffentliche Diskussionen:"
        :startpage.early-adopter.buttons/join-schnaq "Öffentliche schnaqs"
        :startpage.early-adopter/or "oder"

        :footer.buttons/about-us "Über uns"
        :footer.buttons/legal-note "Impressum"
        :footer.buttons/privacy "Datenschutz"
        :footer.tagline/developed-with " Entwickelt mit "

        ;; Create schnaqs
        :schnaqs/create "schnaq anlegen"

        ;; Header image
        :schnaq.header-image.url/placeholder "Bild URL eingeben"
        :schnaq.header-image.url/button "Vorschaubild hinzufügen"
        :schnaq.header-image.url/note "Erlaubt werden nur Inhalte von pixabay.com"
        :schnaq.header-image.url/label "Fügen Sie Ihrem schnaq ein Vorschaubild hinzu"
        :schnaq.header-image.url/successful-set "Vorschaubild erfolgreich gesetzt"
        :schnaq.header-image.url/successful-set-body "Das Bild wird nun in der Übersicht dargestellt."
        :schnaq.header-image.url/failed-setting-title "Fehler beim Hinzufügen des Bildes"
        :schnaq.header-image.url/failed-seting-body "Das Bild wird nicht in der Vorschau genutzt."



        ;; Create schnaq
        :schnaq.create.input/placeholder "Wie soll dein schnaq heißen?"
        :meeting/copy-link-tooltip "Hier klicken, um Link zu kopieren"
        :meeting/link-copied-heading "Link kopiert"
        :meeting/link-copied-success "Der Link wurde in deine Zwischenablage kopiert!"
        :schnaq/created-success-heading "Dein schnaq wurde erstellt!"
        :schnaq/created-success-subheading "Nun kannst du den Zugangslink verteilen oder andere Personen per Mail einladen 🎉"
        :schnaqs/continue-with-schnaq-after-creation "Alle eingeladen? Los geht's!"
        :schnaqs/continue-to-schnaq-button "Zum schnaq"

        :meeting.admin/addresses-label "E-Mail Adressen der Teilnehmer:innen"
        :meeting.admin/addresses-placeholder "E-Mail Adressen getrennt mit Leerzeichen oder Zeilenumbruch eingeben."
        :meeting.admin/addresses-privacy "Diese Adressen werden ausschließlich zum Mailversand genutzt und danach sofort von unseren Servern gelöscht."
        :meeting.admin/send-invites-button-text "Einladungen versenden"
        :meeting.admin/send-invites-heading "Lade die Teilnehmer:innen per E-Mail ein"
        :meeting.admin/delete-statements-heading "Lösche folgende Beiträge"
        :meeting.admin/statements-label "Statement-IDs, die gelöscht werden"
        :meeting.admin/statement-id-placeholder "Statement IDs getrennt mit Leerzeichen oder Zeilenumbruch eingeben."
        :meeting.admin/delete-statements-button-text "Beiträge endgültig löschen"
        :meeting.admin.notifications/emails-successfully-sent-title "Mail(s) verschickt!"
        :meeting.admin.notifications/emails-successfully-sent-body-text "Deine Mail(s) wurden erfolgreich versendet."
        :meeting.admin.notifications/sending-failed-title "Fehler bei Zustellung!"
        :meeting.admin.notifications/sending-failed-lead "Die Einladung konnte an folgende Adressen nicht zugestellt werden: "
        :meeting.admin.notifications/statements-deleted-title "Nachrichten gelöscht!"
        :meeting.admin.notifications/statements-deleted-lead "Deine gewählten Nachrichten wurden erfolgreich gelöscht."

        ;; Brainstorming time
        :schnaq.create/heading "Schnaq starten"
        :brainstorm.buttons/start-now "Starte deine Diskussion"
        :schnaq.create.button/save "Schnaq starten!"

        ;; Discussion Creation
        :discussion.create.public-checkbox/label "Diese Diskussion öffentlich machen"

        :discussion.privacy/public "Öffentliche Diskussion"
        :discussion.privacy/private "Private Diskussion"

        ;; Privacy Page
        :privacy/heading "Was geschieht mit deinen Daten?"
        :privacy/subheading "Wir erklären es dir gerne!"
        :privacy.made-in-germany/lead "EU-Konformes Vorgehen"
        :privacy.made-in-germany/title "Datenschutz ist uns wichtig!"
        :privacy.made-in-germany/body "Das Entwicklerteam von schnaq besteht aus Informatiker:innen, die es Leid sind, dass mit Daten nicht sorgfältig umgegangen wird. Deshalb legen wir besonderen Wert darauf, DSGVO konform zu agieren und sämtliche Daten sicher auf deutschen Servern zu speichern. Kein Datenaustausch mit anderen Unternehmen, keine faulen Kompromisse!"
        :privacy.personal-data/lead "Welche Daten werden erhoben?"
        :privacy.personal-data/title "Persönliche Daten"
        :privacy.personal-data/body [:<> [:p "Standardmäßig werden nur technisch notwendige Daten erhoben. Es findet keine Auswertung über persönliche Daten statt und dein Verhalten auf unserer Website wird auch nur dann anonymisiert analysiert, wenn du dem zustimmst. "] [:p "Wenn du uns unterstützen möchtest und der anonymisierten Analyse zustimmst, werden diese Daten mit Matomo erfasst und auf unseren Servern in Deutschland gespeichert. Matomo ist eine freie und selbstgehostete Alternative zu kommerziellen Anbietern. Wir geben keine Daten an Dritte damit weiter."] [:p [:button.btn.btn-outline-primary {:on-click #(.show js/klaro)} "Einstellungen prüfen"]]]
        :privacy.localstorage/lead "Welche Daten schicke ich an die Server?"
        :privacy.localstorage/title "Datenaustausch"
        :privacy.localstorage/body [:<> [:p "schnaq kann ganz auf Accounts verzichten. Es werden so keine Daten von dir auf unseren Servern gespeichert. Die meiste Interaktion findet über geteilte Links statt. Klicke auf einen Link zu einem schnaq, wird ein Teil des Links (der Hash) in deinem Browser (im LocalStorage) abgespeichert. Besuchst du dann schnaq erneut, schickt dein Browser diesen Hash zurück an uns und erhält so erneut Zugang zum schnaq. Alternativ kannst du dir die Zugangslinks per E-Mail schicken lassen und hältst so alle für den Betrieb notwendigen Daten selbst in der Hand."]
                                    [:p "Im Unterschied zu herkömmlichen Cookies verwenden wir den LocalStorage, welcher naturgemäß nur die wirklich notwendigen Daten von dir an uns zurückschickt. Schaue selbst nach, welche Daten das genau sind, indem du auf den Button klickst."]]
        :privacy.localstorage/show-data "Deine Daten anzeigen"
        :privacy.localstorage.notification/title "Diese Daten hat dein Browser gespeichert"
        :privacy.localstorage.notification/body "Hinweis: \"Kryptische\" Zeichenketten sind die Zugangscodes zu den schnaqs."
        :privacy.localstorage.notification/confirmation "Möchtest du deine Daten wirklich löschen?"
        :privacy.localstorage.notification/delete-button "Daten löschen"
        :privacy.link-to-privacy/lead "Mehr Informationen findest du in unserer ausführlichen "
        :privacy.link-to-privacy/privacy "Datenschutzerklärung"

        ;; schnaqs not found
        :schnaqs.not-found/alert-lead "Leider wurden keine schnaqs gefunden, zu denen du Zugriff hast."
        :schnaqs.not-found/alert-body "Lade zu deinem ersten schnaq ein, indem du einen erstellst."

        ;; Admin Center
        :meeting/educate-on-link-text "Teile den untenstehenden Link mit deinen Kolleg:innen und Freund:innen."
        :meetings/educate-on-link-text-subtitle "Teilnahme ist für alle, die den Link kennen, möglich!"
        :meeting/educate-on-edit "Titel ändern oder Agendapunkte editieren?"
        :meeting/educate-on-admin "Später jederzeit zum Admin-Center zurückkehren!"
        :schnaq.admin-center/heading "Admin-Center"
        :schnaq.admin-center/subheading "schnaq: \"%s\""
        :meeting.admin-center.edit.link/header "Zugang zum Admin-Center"
        :meeting.admin-center.edit.link/primer "Administration ist Arbeit, lass' dir dabei helfen!"
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

        ;; Discussion Language
        :discussion/create-argument-action "Beitrag hinzufügen"
        :discussion/add-argument-conclusion-placeholder "Das denke ich darüber."
        :discussion/add-premise-supporting "Ich möchte die Aussage unterstützen"
        :discussion/add-premise-against "Ich habe einen Grund dagegen"
        :discussion.add.button/support "Dafür"
        :discussion.add.button/attack "Dagegen"
        :discussion.badges/user-overview "Alle Teilnehmer:innen"
        :discussion.badges/delete-statement "Beitrag löschen"
        :discussion.badges/delete-statement-confirmation "Möchtest du den Beitrag wirklich löschen?"
        :discussion.notification/new-content-title "Neuer Beitrag!"
        :discussion.notification/new-content-body "Dein Beitrag wurde erfolgreich gespeichert."

        ;; meetings overview
        :schnaqs/header "Übersicht deiner schnaqs"
        :schnaqs/subheader "Auf diese schnaqs hast du Zugriff"
        :schnaqs.all/header "Öffentliche schnaqs"

        ;; Feedbacks
        :feedbacks.overview/header "Rückmeldungen"
        :feedbacks.overview/subheader "Alle abgegebenen Rückmeldungen"
        :feedbacks.overview/description "Beschreibung"
        :feedbacks.overview/contact-name "Von"
        :feedbacks.overview/contact-mail "E-Mail"
        :feedbacks/button "Feedback"
        :feedbacks/screenshot "Screenshot"
        :feedbacks.modal/primer "Feedback ist wichtig! Wir freuen uns sehr über
     jede Art von Feedback, je ehrlicher desto besser 🥳 Hinterlasse uns
     gerne einen kleinen Kommentar und hilf uns damit diese Software
     weiter zu verbessern. Dankeschön!"
        :feedbacks.modal/contact-name "Dein Name"
        :feedbacks.modal/contact-mail "E-Mail Adresse"
        :feedbacks.modal/description "Deine Rückmeldung"
        :feedbacks.modal/optional "Optional"
        :feedbacks.modal/screenshot "Foto der Anwendung mit abschicken?"
        :feedbacks.modal/disclaimer "Deine Daten werden nur auf unseren Servern
     abgespeichert und keinen Dritten zugänglich gemacht."
        :feedbacks.notification/title "Vielen Dank für deine Rückmeldung!"
        :feedbacks.notification/body "Dein Feedback wurde erfolgreich an uns
     gesendet 🎉"

        :feedbacks.survey/primer
        [:<> "Wir würden uns freuen, wenn du bei einer
     kleinen Umfrage teilnehmen würdest. Diese wird bei Google Forms gehostet
     und unterliegt den "
         [:a {:href "https://policies.google.com/privacy"} "Datenschutzbestimmungen von Google"]
         ". Mit der Teilnahme an der Umfrage akzeptierst du diesen Datenschutzbestimmungen."]
        :feedbacks.survey/checkbox "Ja, ich möchte an der Umfrage teilnehmen"
        :feedbacks.survey/loading "Formular wird geladen..."
        :feedbacks.survey/tab "Umfrage"

        ;; login
        :login/as "Hallo, "
        :login/set-name "Gib deinen Namen ein"

        ;; analytics
        :analytics/heading "Analytics"
        :analytics/overall-meetings "Schnaqs erstellt"
        :analytics/user-numbers "Usernamen angelegt"
        :analytics/average-agendas-title "Durchschnittliche Zahl an Agendas pro Schnaq"
        :analytics/statements-num-title "Anzahl Statements"
        :analytics/active-users-num-title "Aktive User (min. 1 Beitrag)"
        :analytics/statement-lengths-title "Beitragslängen"
        :analytics/argument-types-title "Argumenttypen"
        :analytics/last-meeting-created-title "Letztes Meeting erstellt am"
        :analytics/fetch-data-button "Hole Daten"

        ;; Supporters
        :supporters/heading "Unterstützt vom Wirtschaftsministerium des Landes Nordrhein-Westfalen"

        ;; Testimonials
        :testimonials/heading "Was andere über uns denken:"
        :testimonials.doctronic/quote "Wir beobachten die Entwicklung von schnaq mit großem Interesse für den eigenen Einsatz und für den Einsatz bei unseren Kunden."
        :testimonials.doctronic/author "Ingo Küper, Geschäftsführer doctronic GmbH & Co. KG"

        ;; User related
        :user.button/set-name "Name speichern"
        :user.button/set-name-placeholder "Dein Name"
        :user.button/change-name "Namen ändern"
        :user.button/success-body "Name erfolgreich gespeichert"
        :user.set-name/dialog-header "Hallo 👋"
        :user.set-name/dialog-lead "Schön, dass du hier bist!"
        :user.set-name/dialog-body "Um an Diskussionen teilzunehmen ist es notwendig, dass du einen Namen eingibst."
        :user.set-name/dialog-button "Wie möchtest du genannt werden?"
        :user.set-name.modal/header "Gib einen Namen ein"
        :user.set-name.modal/primer "Der Name wird den anderen Teilnehmer:innen im schnaq angezeigt."
        :user/login "Login / Registrieren"
        :user/logout "Logout"
        :user.profile/settings "Einstellungen"
        :user.profile/star-tooltip "Du bist ein Admin!\n\"Aus großer Kraft folgt große Verantwortung.\""

        ;; Errors
        :errors/navigate-to-startpage "Zurück zur Startseite"
        :errors/generic "Es ist ein Fehler aufgetreten"

        :error.generic/contact-us [:span "Solltest du hier landen nachdem du etwas auf schnaq.com angeklickt hast, gib uns gerne Bescheid unter " [:a {:href "mailto:info@schnaq.com"} "info@schnaq.com"]]

        :error.404/heading "Diese Seite existiert nicht 🙉"
        :error.404/body "Die URL, der du gefolgt bist, existiert leider nicht. Möglicherweise hat sich ein Tippfehler
     oder ein Zeichen zu viel eingeschlichen."

        :error.403/heading "Du hast nicht die Berechtigung diese Seite aufzurufen 🧙‍♂️"
        :error.403/body "Dir fehlt die Berechtigung diese Seite aufzurufen oder es handelt sich um einen Tippfehler in deiner URL."

        ;; Graph Texts
        :graph/heading "Diskussionsübersicht"
        :graph.button/text "Mindmap"
        :graph/download-png "Mindmap als Bild herunterladen"

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
        :pricing.features.user-numbers/content "Lass so viele Mitarbeiter:innen, wie du möchtest, kooperieren. *"
        :pricing.features.team-numbers/heading "Unbegrenzte Teams"
        :pricing.features.team-numbers/content "Die Anzahl der Teams, die du erstellen kannst, ist unlimitiert. *"
        :pricing.features.app-integration/heading "App-Integration"
        :pricing.features.app-integration/content "Verknüpfe schnaq leicht mit deinem Slack, MS Teams, Confluence …"
        :pricing.features.analysis/heading "Automatische Analysen"
        :pricing.features.analysis/content "Die Beiträge werden automatisch analysiert und für alle Teilnehmer:innen aufbereitet."
        :pricing.features.knowledge-db/heading "Wissensdatenbank"
        :pricing.features.knowledge-db/content "Sammle erarbeitetes Wissen und Ideen an einem Ort."
        :pricing.features.mindmap/heading "Interaktive Mindmap"
        :pricing.features.mindmap/content "Alle Beiträge werden automatisch graphisch und interaktiv dargestellt."
        :pricing.features/disclaimer "* Gilt nur für Business-Abonnement"
        :pricing.competitors/per-month-per-user " € pro Monat pro Nutzer:in"
        :pricing.comparison/heading "Ihr wachst weiter – ihr spart mehr!"
        :pricing.comparison/subheading "Egal wie groß dein Team wird, der Preis bleibt der Gleiche.
   So schlägt sich der Preis von schnaq im Vergleich zu Miro + Loomio + Confluence im Paket."
        :pricing.comparison.schnaq/price-point "79 € pro Monat für dein Unternehmen"
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
        [:<> [:span.text-primary "Ja! "] " Du kannst" [:span.text-primary " jeden Monat"] " kündigen,
     wenn du die monatliche Zahlweise gewählt hast. Wenn du die jährliche Zahlweise
     wählst, kannst du zum Ablauf des Abonnementjahres kündigen."]
        :pricing.faq.extra-price/heading "Muss ich für mehr Leute extra bezahlen?"
        :pricing.faq.extra-price/body
        [:<> [:span.text-primary "Nein, "] "du kannst" [:span.text-primary " beliebig viele Personen "]
         " zu deiner Organisation hinzufügen. Jedes Unternehmen, Verein,
         Bildungseinrichtung, usw. braucht " [:span.text-primary "nur ein Abonnement."]]
        :pricing.faq.trial-time/heading "Verlängert sich der Testzeitraum automatisch?"
        :pricing.faq.trial-time/body
        [:<> [:span.text-primary "Nein, "] "wenn dein Testzeitraum endet, kannst du" [:span.text-primary " aktiv entscheiden"]
         ", ob du Zahlungsdaten hinzufügen und weiter den Business-Tarif nutzen möchtest.
         Der " [:span.text-primary "Starter Plan bleibt unbegrenzt kostenfrei"] ", auch nach dem Testzeitraum."]
        :pricing.faq.longer-trial/heading "Kann ich den Business-Tarif länger testen?"
        :pricing.faq.longer-trial/body
        [:<> [:span.text-primary "Ja! "] "Schreibe uns einfach eine " [:span.text-primary " E-Mail"] " an "
         [:a {:href "mailto:info@schnaq.com"} "info@schnaq.com."]]
        :pricing.faq.privacy/heading "Wer hat Zugriff auf meine Daten?"
        :pricing.faq.privacy/body-1
        [:<> "Jede Person, die du deinem Unternehmen hinzufügst, kann potentiell auf die hinterlegten Daten zugreifen."
         "Technisch werden deine Daten vollständig sicher auf"
         [:span.text-primary " deutschen Servern und DSGVO konform"] " abgespeichert. Auf unserer "]
        :pricing.faq.privacy/body-2 "Seite zur Datensicherheit"
        :pricing.faq.privacy/body-3 " findest du mehr Informationen"
        :pricing/headline "Schnaq Abonnement"
        :pricing.newsletter/lead "Werde sofort informiert, wenn das Abonnement live geht: "
        :pricing.newsletter/name "DisqTec Newsletter."

        ;; feature list
        :feature/what "Mit einem Schritt zur Diskussion"
        :feature/share "Lade deine Freunde ein – keine Accounts notwendig"
        :feature/participate "Finde heraus, was andere denken"
        :feature/graph "Automatisch generierte Mindmaps"
        :feature/private-public "Entscheide, wer deine Diskussion sehen darf"
        :feature/secure "Deine Daten sind sicher auf deutschen Servern"

        ;; tooltips
        :tooltip/history-statement "Zurück zum Beitrag von "

        ;; History
        :history.home/text "Diskussionsstart"
        :history.home/tooltip "Zurück zum Diskussionsanfang"
        :history.statement/user "Beitrag von "
        :history.all-schnaqs/text "Übersicht"
        :history.all-schnaqs/tooltip "Zurück zur Übersicht der schnaqs"
        :history.back/text "Zurück"
        :history.back/tooltip "Zurück zum vorherigen Beitrag"

        ;; Route Link Texts
        :router.features/discussion "Diskussionsfeatures"
        :router/admin-center "Admin-Center"
        :router/all-feedbacks "Alle Feedbacks"
        :router/all-meetings "Alle schnaqs"
        :router/analytics "Analyse-Dashboard"
        :router/continue-discussion "Führe Besprechung fort"
        :router/create-schnaq "Schnaq anlegen"
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
        :router/true-404-view "404 Fehlerseite"
        :router/public-discussions "Öffentliche schnaqs"

        :admin.center.start/title "Admin Center"
        :admin.center.start/heading "Admin Center"
        :admin.center.start/subheading "Administration von schnaqs als Superuser"
        :admin.center.delete/confirmation "Soll dieses schnaq wirklich gelöscht werden?"
        :admin.center.delete.public/label "Öffentliche schnaqs"
        :admin.center.delete.public/button "Schnaq löschen"
        :admin.center.delete/heading "Löschen"
        :admin.center.delete.public/heading "Öffentliche schnaqs"
        :admin.center.delete.private/label "Share-hash"
        :admin.center.delete.private/heading "Private schnaqs"}})

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
    {:feature.meetings/hero-image "/imgs/stock/meeting_landing_hero_500w.jpeg"
     :feature.meetings/schedule-meetings "/imgs/startpage/features/meeting-erstellen.png"
     :how-to/taskbar "/imgs/howto/taskbar.svg"
     :icon-add "/imgs/buttons/add-button.svg"
     :icon-community "/imgs/community.svg"
     :icon-crane "/imgs/crane.svg"
     :icon-graph "/imgs/graph/graph-icon.svg"
     :icon-reports "/imgs/reports.svg"
     :icon-robot "/imgs/robot.svg"
     :logo "/imgs/Schnaq-Logo.svg"
     :logo-white "/imgs/Schnaq-Logo-White.svg"
     :logos/digihub "/imgs/logos/digihub_logo.png"
     :logos/doctronic "/imgs/logos/doctronic_logo.png"
     :logos/ignition "/imgs/logos/ignition_logo.png"
     :pricing.others/confluence "imgs/startpage/pricing/confluence.jpeg"
     :pricing.others/loomio "imgs/startpage/pricing/loomio.png"
     :pricing.others/miro "imgs/startpage/pricing/miro.png"
     :privacy/made-in-germany "/imgs/privacy/shield.jpg"
     :schnaqqifant/admin "/imgs/elephants/admin.png"
     :schnaqqifant/erase "/imgs/elephants/erase.png"
     :schnaqqifant/hippie "https://s3.disqtec.com/schnaq-schnaqqifanten/schnaqqifant-hippie.png"
     :schnaqqifant/original "/imgs/schnaqqifant.svg"
     :schnaqqifant/police "https://s3.disqtec.com/schnaq-schnaqqifanten/schnaqqifant-polizei.png"
     :schnaqqifant/share "/imgs/elephants/share.png"
     :schnaqqifant/stop "/imgs/elephants/stop.png"
     :schnaqqifant/talk "/imgs/elephants/talk.png"
     :schnaqqifant/white "/imgs/schnaqqifant_white.svg"
     :spotlight/eco-brain "/imgs/spotlight/brain.jpg"
     :spotlight/home-office "/imgs/spotlight/covid-19.jpg"
     :spotlight/merkel "/imgs/spotlight/merkel.jpg"
     :startpage.features/admin-center "/imgs/startpage/features/admin-center.png"
     :startpage.features/discussion-graph "/imgs/startpage/sample_graph.png"
     :startpage.features/meeting-organisation "/imgs/startpage/meeting_organisation_500px.png"
     :startpage.features/sample-discussion "/imgs/startpage/discussion_vegan.jpg"}))

(defn video
  "Returns an video path"
  [identifier]
  (identifier
    {:animation-discussion/webm "/animations/animation_discussion.webm"
     :animation-discussion/mp4 "/animations/animation_discussion.mp4"
     :start-page.features.sample-discussion/webm "/animations/sample_discussion.webm"
     :start-page.features.sample-discussion/mp4 "/animations/sample_discussion.mp4"
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
     :clipboard "fa-clipboard-list"
     :clock "fa-clock"
     :circle "fa-circle"
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
     :graph "fa-project-diagram"
     :heart "fa-heart"
     :home "fa-home"
     :language "fa-language"
     :laptop "fa-laptop-code"
     :lock-open "fa-lock-open"
     :minus "fa-minus"
     :newspaper "fa-newspaper"
     :plane "fa-paper-plane"
     :plus "fa-plus"
     :share "fa-share-alt"
     :shield "fa-shield-alt"
     :site-map "fa-sitemap"
     :star "fa-star"
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
     :gray/dark "#343a40"
     :white "#ffffff"}))