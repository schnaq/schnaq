(ns schnaq.interface.translations.english
  (:require [schnaq.interface.config :refer [marketing-num-schnaqs marketing-num-statements]]
            [schnaq.interface.utils.toolbelt :as toolbelt]))

(def labels
  {:error/export-failed "Export failed. Please try again later."

   :nav/schnaqs "schnaqs"
   :nav.schnaqs/show-all "All schnaqs"
   :nav.schnaqs/show-all-public "All public schnaqs"
   :nav.schnaqs/create-schnaq "Create schnaq"
   :nav.schnaqs/last-added "Last created schnaq"
   :nav/blog "Blog"
   :nav/admin "Admin"
   :nav.buttons/language-toggle "Change language"

   ;; Alphazulu Page
   :alphazulu.page/heading "Alphazulu"
   :alphazulu.page/subheading "Modern Work for Modern Companies"
   :alphazulu.introduction/title "New Work out of Germany"
   :alphazulu.introduction/body
   [:<>
    [:p "Secure, custom, fast: This is the power of ALPHAZULU."]
    [:p "Use the modules from "
     [:a {:href "/"} "schnaq"] ", " [:a {:href "https://wetog.de/"} "wetog"] ", "
     [:a {:href "https://xignsys.com/"} "XignSys"] ", " [:a {:href "https://www.cobago.de/"} "Cobago"] ", "
     [:a {:href "https://www.trustcerts.de/"} "TrustCerts"] " und " [:a {:href "https://ec3l.com/"} "EC3L"]
     " to find the perfect custom solution for your company."]
    [:p "All Alphazulu products can be combined and integrated with each other."]]
   :alphazulu.schnaq/title "Structured Knowledge"
   :alphazulu.schnaq/body
   [:<>
    [:p "Manage the digital transformation of your company. We help you with internal communication and knowledge transfer."]
    [:p "Use schnaq standalone or with e.g. Xign.Me to log-in without passwords. Or upgrade your Wetog subscription to include schnaq."]]
   :alphazulu.wetog/title "Secure Collaboration"
   :alphazulu.wetog/body
   [:<>
    [:p "Wetog uses the quantum-computer safe encryption LIQRYPT to secure all data, chats and video-conferences."]
    [:p "Already got a Wetog subscription? Upgrade it to include schnaq with one click. Or add another Alphazulu-partner module."]]
   :alphazulu.xignsys/title "Passwordless Log-Ins from Anywhere"
   :alphazulu.xignsys/body
   [:<>
    [:p "With the unique solution Xign.Me, you will be able to authenticate with your biometrics anywhere without a password."]
    [:p "Try Xign.Me while logging into schnaq. You can also log in with other Alphazulu partners without an insecure password."]]
   :alphazulu.cobago/title "Digital Assistant"
   :alphazulu.cobago/body
   [:<>
    [:p "Cobago helps your company automate forms and processes. You do not need to have any technical knowledge."]
    [:p "Want to use schnaq discussions inside the Cobago platform? Write us for access!"]]
   :alphazulu.trustcerts/title "Digital Blockchain Signatures"
   :alphazulu.trustcerts/body
   [:<>
    [:p "With the TrustCerts Solutions you can sign and verify important documents through a tamperproof blockchain."]
    [:p "You can sign important results from schnaq discussions, documents out of Wetog or forms build with Cobago Sixpad."]]
   :alphazulu.ec3l/title "Reliable Employee Training"
   :alphazulu.ec3l/body
   [:<>
    [:p "Modern, target-oriented training that works and elevates your company to another level."]
    [:p "Find schnaq modules inside EC3L trainings soon. Continue using what you already know."]]
   :alphazulu.activate/title "Personal Conversation"
   :alphazulu.activate/body [:p "If you want to know more about schnaq and Alphazulu, you can book a slot through our "
                             [:a {:href "https://calendly.com/schnaq/30min"} "Calendly."] " We are looking forward to a talk with you."]

   ;; Call to contribute
   :call-to-contribute/lead "There are currently no contributions"
   :call-to-contribute/body "Start with your first post"

   ;; code of conduct
   :coc/heading "Code of Conduct"
   :coc/subheading "Do onto others as you would have them do unto you"

   :coc.users/lead "Behaviour towards other users"
   :coc.users/title "Respect and Non-Discrimination"
   :coc.users/body "A respectful behaviour is important and is the basis of each factual discussion. This applies not only offline but also online.\nIt is important to us that all users can express themselves without being discriminated against based on their person, origin or views. \nPosts that do not adhere to these guidelines will be deleted."

   :coc.content/lead "Content"
   :coc.content/title "We obey the law, please do that too"
   :coc.content/body "We comply with German law; this applies especially to data protection, equality and non-discrimination.\nContent that violates applicable law will be deleted."

   ;; how-to
   :how-to/button "How to schnaq?"
   :how-to/title "How to schnaq?"
   :how-to.create/title "What would you like to schnaq about with your team??"
   :how-to.create/body
   [:<>
    [:p [:i "\"Which systems do we use and when?\", \"What price do we offer educational institutions?\", \"What is our vision?\""]]
    [:p "Give your schnaq a meaningful title and let the whole team discuss. All contributions are important and offer an individual perspective on the discussion."]]
   :how-to.why/title "Why should I schnaq?"
   :how-to.why/body "Fluffed your lines? Didn't get a chance to speak? Did everyone get everything? No clue why the discussion took so long?\nSave your time and store your knowledge in schnaq. Participate whenever you like. The mindmap of the discussion is on the house."
   :how-to.admin/title "Administer your schnaq or invite someone to help you"
   :how-to.admin/body "Invite participants via link or mail. If you need any help as admin just invite someone to lend you a hand."
   :how-to.schnaq/title "How do I schnaq?"
   :how-to.schnaq/body "Share your opinion! Others can react to your post or add a comment. The mindmap is automatically generated and updated whenever there is a new statement. Fancy a post? Just click on it and jump right at it."
   :how-to.pro-con/title "To Agree or Disagree ..."
   :how-to.pro-con/body "Share your basic attitude towards the current post. With a click on our agree/disagree-button you change your post's attitude. Pro statements are highlighted blue and contra statements are highlighted orange. You can add multiple pro statements as well as contra statements in response to the same statement."
   :how-to.call-to-action/title "Now you know!"
   :how-to.call-to-action/body "That's actually all there is to know about schnaq. Now use it!"
   :how-to/ask-question "Not sure how to schnaq?"
   :how-to/ask-question-2 "Still not sure how to use schnaq?"
   :how-to/answer-question "Let us show you how!"
   :how-to/question-dont-show-again "Got it?"
   :how-to/answer-dont-show-again "Don't show this tip anymore!"
   :how-to/back-to-start "Back to schnaq"

   ;; Startpage
   :startpage/heading "Collecting questions made simple"
   :startpage/subheading "Q&A made simple"
   :startpage/hook "Simply collect questions and provide answers in your hybrid courses - for more clarity and sustainable exchange"
   :startpage.social-proof/numbers [:span "schnaq helped lead over " [:b marketing-num-schnaqs]
                                    " discussions and Q&As which amounts to " [:b marketing-num-statements] " instances of shared knowledge."]
   :schnaq.startpage.cta/button "Schnaqq free of charge"

   :startpage.usage/lead "What do I use schnaq for?"

   :startpage.features/more-information "More information"

   :startpage.information.know-how/title "Collecting questions made easy"
   :startpage.information.know-how/body "Schnaq's Q&A functions and structured discussions help your course, workshop or training to exchange knowledge at lightning speed and to prepare it in a sustainable way. Only where knowledge flows can great things be created."

   :startpage.information.positioning/title "Understand what is being asked"
   :startpage.information.positioning/body "Identify the problems of your course at a glance. With the automatically generated mindmap and A.I. analyses, each question round is clearly presented and easy to understand."

   :startpage.information.anywhere/title "Use schnaq anywhere at any time"
   :startpage.information.anywhere/body "Schnaq runs as a web app on all major operating systems, browsers and devices. Whether it's a smartphone, tablet or computer."

   :startpage.information.meetings/title "Hybrid questioning"
   :startpage.information.meetings/body "Reach out through schnaq to people who can't be there. Everyone can be involved and questions can be asked anonymously. With smart Q&A, you can even stay up-to-date online!"

   :startpage.feature-box/heading "How does schnaq work?"
   :startpage.feature-box.know-how/title "Prepare yourself"
   :startpage.feature-box.know-how/body "Create a schnaq where questions can be collected. You will receive an access code, QR code and a link with which you can invite all participants."
   :startpage.feature-box.discussion/title "Include the Course"
   :startpage.feature-box.discussion/body "With smartphones, tablets and laptops, your audience can join the schnaq and ask their questions. They are automatically shown matching questions with answers that are similar in content."
   :startpage.feature-box.learnings/title "Share the Answers"
   :startpage.feature-box.learnings/body "Answered questions are immediately available to your course and can be viewed at any time. You can always add content afterwards or use the same question base again in the next course."

   :startpage.early-adopter/title "Gotten Curious?"
   :startpage.early-adopter/body "Be one of the first to use schnaq hubs"

   :startpage.newsletter/heading "Be the first to know when you can profit from new features!"
   :startpage.newsletter/button "Give me exclusive previews!"
   :startpage.newsletter/address-placeholder "E-Mail Address"
   :startpage.newsletter/consent "I want to subscribe to the schnaq newsletter, and regularly receive information about schnaq.com."
   :startpage.newsletter/more-info-clicker "Data Processing"
   :startpage.newsletter/policy-disclaimer "Schnaq saves, processes and saves the personal information you enter above to
        subscribe you to the newsletter. You can unsubscribe at any time, by clicking the appropriate link in the emails you will receive.
        Alternatively you can write us an email, and we will unsubscribe you personally."
   :startpage.newsletter/privacy-policy-lead "More information about the handling of your personal data can be found in our"

   :startpage.faq/title "Frequently Asked Questions"
   :startpage.faq/subtitle "(this is what it could look like for you)"
   :startpage.faq.data/question "What happens with my data?"
   :startpage.faq.data/answer-1 "We only store data on german servers, to provide the best possible data protection. All details are explained
        in an understandable manner in our"
   :startpage.faq.data/link-name "privacy policy"
   :startpage.faq.data/answer-2 "."
   :startpage.faq.integration/question "Can I integrate schnaq with the software I'm already using?"
   :startpage.faq.integration/answer "Currently, schnaq can be integrated into WETOG at the click of a mouse. We currently work on integrations for Slack, MS Teams and other popular communication software.
        If you want to be the first to know, when we launch the feature, subscribe to our"
   :startpage.faq.integration/link-name "newsletter."
   :startpage.faq.costs/question "Are there any hidden costs?"
   :startpage.faq.costs/answer "schnaq is currently in a test-phase and completely free. No hidden payments.
        Although we are always happy about your honest feedback as a form of payment."
   :startpage.faq.start/question "How can I start using schnaq?"
   :startpage.faq.start/answer "You can either use schnaq anonymously, or register to have the possibility to see and administrate your schnaqs
        and statements from anywhere. Give it a try and"
   :startpage.faq.start/link-name "create a schnaq."
   :startpage.faq.why/question "Why should I use schnaq?"
   :startpage.faq.why/answer "schnaq is for you, if you support a modern, open and equal work-culture.
        Our goal is to make communication and knowledge-sharing at the workplace as flexible and easy as possible.
        This way we heighten the potential of every person in the company, and thus of the company itself."

   :startpage.founders-note/title "A Note from the Founders"

   ;; Login page
   :page.login/heading "Please Login"
   :page.login/subheading "You need to login to access this resource"
   :page.login.alert/text-1 "Don't have an account yet? Create a free account here to create your first schnaq. Check out"
   :page.login.alert/button "this page"
   :page.login.alert/text-2 "to view your benefits as a registered:user."

   :page.beta/heading "Beta-Feature"
   :page.beta/subheading "This feature is currently only enabled for beta-testers. Please log in if you are one."

   :footer.buttons/about-us "About us"
   :footer.buttons/legal-note "Legal Note"
   :footer.buttons/privacy "Privacy Notice"
   :footer.buttons/press-kit "Press Kit"
   :footer.buttons/publications "Publications"
   :footer.tagline/developed-with "Developed with"
   :footer.sponsors/heading "Our servers are hosted by"
   :footer.registered/rights-reserved "All rights reserved"
   :footer.registered/is-registered "is a registered trademark"

   ;; Header image
   :schnaq.header-image.url/placeholder "Image url"
   :schnaq.header-image.url/button "Add as preview image"
   :schnaq.header-image.url/note "Images are restricted to content from pixabay.com"
   :schnaq.header-image.url/label "Add a preview image to your schnaq header"
   :schnaq.header-image.url/successful-set "Preview image successfully set"
   :schnaq.header-image.url/successful-set-body "The image will be featured in this schnaq's header."
   :schnaq.header-image.url/failed-setting-title "Error when adding image"
   :schnaq.header-image.url/failed-setting-body "The image will not be used as preview image."

   ;; Create schnaq
   :schnaq.create.dispatch/heading "What do you want to start?"
   :schnaq.create.dispatch/qanda "Questions and Answers"
   :schnaq.create.dispatch.qanda/explain "Collect questions during the course and answer them, when you have the time."
   :schnaq.create.dispatch.qanda/share "Invite participants via link or a code."
   :schnaq.create.dispatch/discussion "Discussion"
   :schnaq.create.dispatch.discussion/explain "Conduct AI assisted and persistent discussions with others."
   :schnaq.create.dispatch.discussion/share "Invite participants via link."

   :schnaq.create.input/title "What would you like to discuss?"
   :schnaq.create.qanda.input/title "What should the questions be about?"
   :schnaq.create.input/placeholder "Specify Subject"
   :schnaq.create.hub/help-text "Directly assign your schnaq to a hub."
   :schnaq/copy-link-tooltip "Click here to copy your link"
   :schnaq/link-copied-heading "Link copied"
   :schnaq/link-copied-success "The link was copied to your clipboard!"
   :schnaq/created-success-heading "Your schnaq was created!"
   :schnaq/created-success-subheading "Distribute your personal share-link or invite participants via email 🎉"
   :schnaqs/continue-with-schnaq-after-creation "Did you invite everybody? Lets go!"
   :schnaqs/continue-to-schnaq-button "To the schnaq"

   :schnaq.admin/addresses-label "Email addresses of the participants"
   :schnaq.admin/addresses-placeholder "Email addresses separated by a newline or space."
   :schnaq.admin/addresses-privacy "These addresses are only used to send the invitation emails and are deleted from our
        servers immediately afterwards."
   :schnaq.admin/send-invites-button-text "Send invitations"
   :schnaq.admin/send-invites-heading "Invite participants via email"
   :schnaq.admin.notifications/emails-successfully-sent-title "Mails sent!"
   :schnaq.admin.notifications/emails-successfully-sent-body-text "Your invitations were sent successfully."
   :schnaq.admin.notifications/sending-failed-title "Error during mail delivery!"
   :schnaq.admin.notifications/sending-failed-lead "The following invitations could not be delivered:"
   :schnaq.admin.notifications/statements-deleted-title "Statements deleted!"
   :schnaq.admin.notifications/statements-deleted-lead "The statements you entered have been deleted."
   :schnaq.admin.notifications/heading "Configuration"
   :schnaq.admin.configurations.read-only/checkbox "Set to read-only"
   :schnaq.admin.configurations.read-only/explanation "When checked, users can no longer add new posts to the discussion. Existing posts are still readable and can be analysed. You can change this option anytime."
   :schnaq.admin.configurations.disable-pro-con/label "Disable agree/disagree button"
   :schnaq.admin.configurations.disable-pro-con/explanation "When checked, users can no longer use the agree/disagree button. New posts will be handled as agreement. You can change this option anytime."
   :schnaq.admin.configurations.mods-mark-only/label "Only Moderators mark answers"
   :schnaq.admin.configurations.mods-mark-only/explanation "When checked, only moderators are allowed to mark an answer as the correct one. Otherwise, everybody is able to do this."
   :schnaq.admin.configurations.mods-mark-only/beta "Only beta-users are allowed to change this setting. Request access at hello@schnaq.com."

   :schnaq.access-code.clipboard/header "Access code copied"
   :schnaq.access-code.clipboard/body "The access code has been copied to your clipboard."

   :statement/reply "Reply"
   :statement.edit.send.failure/title "Edit could not be made"
   :statement.edit.send.failure/body "The edit could not be published. Please try again in a short while."
   :statement.edit/label "Edit statement"
   :statement.edit.button/submit "Submit"
   :statement.edit.button/cancel "Cancel"
   :schnaq.edit/label "Edit title"

   ;; schnaq creation
   :schnaq.create/title "Start schnaq"
   :schnaq.create/heading "Start your schnaq."
   :schnaq.create/subheading "With a schnaq, you can let your team discuss and secure sustainable decisions."
   :schnaq.create.qanda/subheading "With a schnaq, you can have optimized Q&As."
   :schnaq.create/info "Add a simple and comprehensible title to your discussion."
   :schnaq.create.button/save "Start a new schnaq"

   ;; schnaq value
   :schnaq.value/title "You're ready to go!"
   :schnaq.value/subtitle "A few tips to help you and your team:"
   :schnaq.value.security/title "Privacy"
   :schnaq.value.security/text "Privacy is important to us! Your data is safe on German servers in accordance with European law."
   :schnaq.value.respect/title "Discuss with respect"
   :schnaq.value.respect/text "Respectful interaction is important in order to be able to live with one another and is the basis for objective discussions."
   :schnaq.value.share/title "Share your discussion"
   :schnaq.value.share/text "Simply invite participants via link or email. No registration necessary!"
   :schnaq.value.private/title "Private by default"
   :schnaq.value.private/text "Your schnaqs can only be accessed by you and people you share them with."
   :schnaq.value.cards/title "Mindmap"
   :schnaq.value.cards/text "Our mind map is generated automatically and provides a quick overview."
   :schnaq.value.results/title "Results view"
   :schnaq.value.results/text "View summaries and analysis of your discussion (beta feature)."

   ;; Discussion Creation
   :discussion.create.hub-exclusive-checkbox/title "Add schnaq to a Hub"
   :discussion.create.hub-exclusive-checkbox/label "Add to Hub"

   ;; Discussion Dashboard
   :dashboard/posts "Posts"
   :dashboard/members "Members"
   :dashboard/summary "Summary"
   :dashboard/top-posts "Top Posts"

   :discussion.navbar/title "Title"
   :discussion.navbar/posts "Posts"
   :discussion.navbar/members "Members"
   :discussion.navbar/views "Views"
   :discussion.state/read-only-label "read-only"
   :discussion.state/read-only-warning "This discussion is read-only. You can read the statements, but not write anything."
   :discussion.navbar/settings "Settings"

   ;; schnaq progress bar related stuff
   :discussion.progress/days-left "%s days left"
   :discussion.progress/unlimited "Open unlimited"
   :discussion.progress/end "End of discussion"
   :discussion.progress/ends "Ends %s"
   :discussion.progress/ends-not "Does not end"
   :discussion.progress.creation/heading "Limit the duration of your discussion"
   :discussion.progress.creation/label "End in days"
   :discussion.progress.creation/button-limit "%s days"
   :discussion.progress.creation/button-unlimited "Unlimited"

   ;; Conversion-Edit-Funnel
   :discussion.anonymous-edit.modal/title "Please sign in to edit"
   :discussion.anonymous-edit.modal/explain [:<> "To prevent fraudulent behaviour with anonymous statements, you must " [:strong "sign in to edit a statement."]]
   :discussion.anonymous-edit.modal/persuade "Recent statements from you in this browser will automatically be converted to your logged in account."
   :discussion.anonymous-edit.modal/cta "Sign in / Sign up"

   :discussion.anonymous-labels.modal/title "Please sign in to edit labels"
   :discussion.anonymous-labels.modal/explain [:<> "To prevent fraudulent behaviour with anonymous statements, you must " [:strong "sign in to edit labels."]]
   :discussion.anonymous-labels.modal/cta "Sign in / Sign up"

   ;; Conversion-Delete-Funnel
   :discussion.anonymous-delete.modal/title "Please sign in to delete"
   :discussion.anonymous-delete.modal/explain [:<> "To prevent fraudulent behaviour with anonymous statements, you must " [:strong "sign in to delete a statement."]]
   :discussion.anonymous-delete.modal/persuade "Recent statements from you in this browser will automatically be converted to your logged in account."
   :discussion.anonymous-delete.modal/cta "Sign in / Sign up"

   ;; Beta Only Funnel
   :beta.modal/title "Beta-Feature"
   :beta.modal/explain [:<> "This feature is in a test-state and you need to " [:strong "be a beta-tester to use it."]]
   :beta.modal/persuade "You can write us an email if you would like to become a beta-tester."
   :beta.modal/cta "Request Access"
   :beta.modal.login/intro "Already beta tester"
   :beta.modal.login/button "Then login"
   :page.beta.modal/cta "If you are interested in joining our beta-tester programm, please write us an email at"

   ;; Press Kit
   :press-kit/heading "Press & Media"
   :press-kit/subheading "We are happy to be available for interviews and articles!"
   :press-kit.intro/heading "Thank you for your interest in schnaq!"
   :press-kit.intro/lead "Please take a moment to read our brand guidelines. If you have any press enquiries or would like to write about us, please email presse@schnaq.com. We would love to talk to you!"
   :press-kit.spelling/heading "Correct Spelling and Pronunciation"
   :press-kit.spelling/content-1 "Our product is called"
   :press-kit.spelling/content-2 "(spoken: [ˈʃnak]) and is written with a \"q\". It is pronounced with a soft \"sch\", analogous to the North German \"schnacken\". Except at the beginning of sentences, schnaq should be written in lower case."
   :press-kit.not-to-do/heading "Please note the following points"
   :press-kit.not-to-do/bullet-1 "Do not use any other images, illustrations, content or other assets from this domain without permission."
   :press-kit.not-to-do/bullet-2 "Avoid displaying these graphics in a way that implies a relationship, affiliation or endorsement by schnaq. If you are unsure, please feel free to contact us."
   :press-kit.not-to-do/bullet-3 "Do not use these graphics as part of the name of your own product, business or service."
   :press-kit.not-to-do/bullet-4 "Please avoid altering these graphics in any way or combining them with other graphics without our written consent."
   :press-kit.materials/heading "Assets"
   :press-kit.materials/fact-sheet "Fact-Sheet"
   :press-kit.materials/logos "Logos"
   :press-kit.materials/product "Product Images"
   :press-kit.materials/team "Team Pictures"
   :press-kit.materials/download "Download"
   :press-kit.about-us/heading "Further Information"
   :press-kit.about-us/body "Further information on our founders, scientific publications and other appearances in newspapers and media, can be found on the following pages:"

   ;; Publications
   :publications/heading "Publications and Articles"
   :publications/subheading "The Science behind schnaq"
   :publications.primer/heading "From Science into Practice"
   :publications.primer/body "The software we develop is based not only on experience, but also on many years of research in the fields of discussion and communication. Here you will find scientific articles, newspaper articles and other publications that originate from our team or have been produced in cooperation with our team."

   :publications.perspective-daily/summary "An article about our research in Perspective Daily. Focus on structured discussion."
   :publications.salto/summary "An interview with our founders Dr. Christian Meter and Dr. Alexander Schneider about discussions on the internet, trolls and how to fight them."
   :publications.dissertation-alex/summary "Dr. Alexander Schneider's dissertation deals with the question of whether structured discussions can be carried out on the internet via decentralised systems."
   :publications.dissertation-christian/summary "In Dr. Christian Meter's dissertation, several novel procedures and approaches are highlighted to be able to conduct structured discussions on the internet."
   :publications.structure-or-content/summary "This paper analyses whether Pagerank as an algorithm can make reliable statements about argument relevance and how its performance compares to newer algorithms."
   :publications.overview-paper/summary "A presentation of a wide variety of methods that make it possible to improve real discussions on the internet."
   :publications.dbas/summary "The description of a formal prototype for dialogue-based online argumentation including evaluation."
   :publications.dbas-politics/summary "A presentation of the concept of dialogue-based online discussions for lay people."
   :publications.eden/summary "The presentation of a software package that allows the operation of decentralised servers that give users access to online discussion systems."
   :publications.jebediah/summary "The paper demonstrates a social bot based on Google's Dialogflow Engine. The bot is able to communicate with its users in social networks based on dialogue."
   :publications.dbas-experiment/summary "In a field experiment with over 100 test persons, we will investigate how well a dialogue-based argumentation system can be used by laypersons."
   :publications.reusable-statements/summary "The authors explore the idea of making online arguments and their interrelationships usable and reusable as a resource."
   :publications.discuss/summary "If structured discussions are possible via software, is it also possible to let these discussions take place in any web context? The authors explore this question."
   :publications.kind/article "Article"
   :publications.kind/dissertation "Dissertation (english)"
   :publications.kind/interview "Interview"
   :publications.kind/newspaper-article "Newspaper Article"
   :publications.kind/paper "Paper (english)"
   :publications.kind/short-paper "Shortpaper (english)"

   ;; Privacy Page
   :privacy/heading "What happens to your data?"
   :privacy/subheading "We lead you through it step by step!"
   :privacy/open-settings "Open Settings"
   :privacy.made-in-germany/lead "EU-regulation conformity"
   :privacy.made-in-germany/title "Data privacy is important to us!"
   :privacy.made-in-germany/body
   [:<>
    [:p "The development team of schnaq consists of developers that are tired of misuse of private data. This is why we take special care to be GDPR compliant and to save all data securely on german servers provided by Hetzner. We do not exchange any data with other companies without absolute need and making it completely clear."]
    [:p "If you are still unclear about how we handle your data, please feel free to contact us! We really care about transparency and clarity with personal data and we explain to you to the last bit what happens with the data."]]
   :privacy.personal-data/lead "Which data is saved?"
   :privacy.personal-data/title "Personal Data"
   :privacy.personal-data/body
   [:<>
    [:p "By default, only technically necessary data is collected. No evaluation of personal data takes place and your behaviour on our website is also only analysed anonymously."]
    [:p "Your user behaviour is recorded with Matomo and stored on our servers in Germany. Matomo is a free and self-hosted alternative to commercial providers. We do not pass on any data to third parties with it."]]
   :privacy.localstorage/lead "What data do I send to the server?"
   :privacy.localstorage/title "Data Exchange"
   :privacy.localstorage/body
   [:<>
    [:p "schnaq has no need for accounts. This way no personal data about you is saved on the server. Most of the interactions work through links. When you click on a link a part of it (the so called hash) is stored in your browser (in the localStorage). As soon as you go to schnaq.com again, your browser sends this hash back and you gain access to your created schnaqs. Alternatively you can send the links to yourself via email. This way you have all the data in your own hands."]
    [:p "In difference to website-cookies we use the localStorage, which only saves data that is needed for the application to function for you.
         You can see the data in your localStorage by clicking the following button."]]
   :privacy.localstorage/show-data "Show my data"
   :privacy.localstorage.notification/title "This data is saved by your browser"
   :privacy.localstorage.notification/body "Tip: \"Cryptic\" strings of characters are the codes that allow you to view schnaqs."
   :privacy.localstorage.notification/confirmation "Do you really want to delete the data?"
   :privacy.localstorage.notification/delete-button "Delete data"

   :privacy.data-processing.anonymous/lead "What happens to your posts?"
   :privacy.data-processing.anonymous/title "Data Processing for Anonymous Users"
   :privacy.data-processing.anonymous/body [:<> [:p "Your posts and chosen username will be stored on our own servers and are not passed on to other companies. If you do not provide any username, the author of your posts will be displayed as \"Anonymous\". Since even we do not remember who the contributions come from, it is not possible to edit the contribution. We do not store any other personal data, e.g. user-agent or ip address, to your posts."]
                                            [:p "Posts in public schnaqs can be viewed by anyone. Posts in private schnaqs will only be visible to those with access to the link. Administrators of a schnaq are able to delete a schnaq’s posts."]]
   :privacy.data-processing.registered/lead "And if I am logged in now?"
   :privacy.data-processing.registered/title "Data Processing for Registered Users"
   :privacy.data-processing.registered/body
   [:<> [:p "If you decide to register, your email address and name will be saved. This allows us to personalize your schnaq experience and display your name when you save a post. The email address is among other things necessary for notifications so that you are informed when there are new posts for you."]
    [:p "When you log in via an external provider, such as LinkedIn, LinkedIn receives a request from you to transmit the displayed information to us, which we then store. If you log in again, LinkedIn will also receive another request. If you want to avoid this, simply create an account with us directly."]
    [:p "In addition, we store in your account the hubs and schnaqs to which you have access. So you can also log in on your smartphone or other device and have access to all your schnaqs."]
    [:p "Now it is also possible to use advanced features, like edit posts, since you now have an identity on our platform 👍"]
    [:p "At any time you can contact us and request to view or delete your data."]]

   :privacy.link-to-privacy/lead "More information can be found in the comprehensive"
   :privacy/note "Privacy notice"

   :privacy.extended/heading "Privacy"
   :privacy.extended/subheading "We are compliant to GDPR"
   :privacy.extended.intro/title "General information on data processing"
   :privacy.extended.intro/body
   [:<>
    [:p "As a matter of principle, we only process personal data insofar as this is necessary to provide a functional website and our content. Personal data is regularly processed only with the consent of the user."]
    [:p "Insofar as consent is required for processing operations of personal data, Art. 6 (1) lit. a EU General Data Protection Regulation (GDPR) serves as the legal basis.\nIf the processing is necessary to protect a legitimate interest on our part or on the part of a third party and your interests, fundamental rights and freedoms do not override the former interest, Art. 6 (1) lit. f GDPR serves as the legal basis for the processing."]
    [:p "Personal data will be deleted as soon as the purpose of storage ceases to apply. Storage may also take place if this has been provided for by the European or national legislator in Union regulations, laws or other provisions to which we are subject. Data will also be deleted if a storage period prescribed by the aforementioned standards expires."]]
   :privacy.extended.logfiles/title "Provision of the website and creation of log files"
   :privacy.extended.logfiles/body
   [:<>
    [:p "Each time our website is accessed, our system automatically collects data and information (browser type / version used, operating system, IP address, date and time of access, websites from which our website was accessed, websites accessed via our website) from the computer system of the accessing computer. The data is stored in the log files of our system. This data is not stored together with other personal data of the user. The legal basis for the temporary storage of the data and the log files is Art. 6 para. 1 lit. f GDPR."]
    [:p "The temporary storage of the IP address by the system is necessary to enable delivery of the website to the user's computer. For this purpose, the IP address must remain stored for the duration of the session. The storage in log files is done to ensure the functionality of the website. In addition, we use the data to optimize the website and to ensure the security of our information technology systems. These purposes are also our legitimate interest in data processing according to Art. 6 para. 1 lit. f GDPR."]
    [:p "The data is deleted as soon as it is no longer required to achieve the purpose for which it was collected. In the case of the collection of data for the provision of the website, this is the case when the respective session has ended. In the case of storage of data in log files, this is the case after seven days at the latest. Storage beyond this period is possible. In this case, the IP addresses of the users are deleted or anonymized."]
    [:p "The collection of data for the provision of the website and the storage of the data in log files is mandatory for the operation of the website. Consequently, there is no possibility to object."]]
   :privacy.extended.cookies/title "Cookies"
   :privacy.extended.cookies/body
   [:<>
    [:p "We use so-called cookies on our homepage. Cookies are data packages that your browser stores in your terminal device at our instigation. A distinction is made between two types of cookies: temporary, so-called session cookies, and persistent cookies."]
    [:p "Session cookies are automatically deleted when you close the browser. These store a so-called session ID, with which various requests of your browser can be assigned to the common session. This allows your computer to be recognized when you return to our website. The use of session cookies is necessary so that we can provide you with the website. The legal basis for the processing of your personal data using session cookies is Art. 6 para. 1 lit. f GDPR."]
    [:p "Persistent cookies are automatically deleted after a predefined period of time, which may differ depending on the cookie. These cookies remain on your end device for a predefined time and are usually used to recognize you when you visit our homepage again. The use of persistent cookies on our homepage is based on the legal basis of Art. 6 para. 1 lit. f GDPR."]
    [:p "You can set your internet browser so that our cookies cannot be stored on your end device or so that cookies that have already been stored are deleted. If you do not accept cookies, this may lead to restrictions in the function of the Internet pages."]
    [:p "Specifically, we have these types of cookies:"]
    [:ul
     [:li "CSRF token (session cookie), which secures the contact form against unobserved content submission. This is a random arrangement of characters, which is used only for sending the form. This cookie is deleted after you leave our website. This protection mechanism complies with common security standards and can, for example "
      [:a {:href "https://en.wikipedia.org/wiki/Cross-site_request_forgery"}
       "here"]
      " be researched further."]
     [:li "Login cookie (persistent cookie), which recognises you as the user you logged in with. After 14 days, your cookie expires and is deleted. If you delete this cookie, you will have to log in again the next time you visit the site. You can find our authentication server here: https://auth.schnaq.com"]]
    [:p "All cookies we use generate random strings that are used to match corresponding strings on our server."]]

   :privacy.extended.personal-data/title "Personal data"
   :privacy.extended.personal-data/body
   [:<>
    [:h4 "Use of schnaq without user accounts"]
    [:p "If you use schnaq without registering, you are so called \"Anonymous User\". Here, in addition to the data necessary for server operation, only your contribution and an optional self-selected name will be saved. When saving the contribution, this string is then loosely saved with the contribution. An allocation to an identity does not take place thereby. If someone with the same name participates in any schnaq, the contributions appear to the outside as if they came from the same person."]
    [:p "By sending your contribution you agree to the storage. Since we can later no longer trace who made the contribution, you have no right to delete this contribution, because there is no proof of authorship."]
    [:h4 "Use of schnaq as registered user"]
    [:p "When you register, your mail address and your first and last name are stored. These are necessary for the operation of schnaq, the collection is thus made according to Art. 6 para. 1 lit. f GDPR. Registration is optional for the normal operation of schnaq. With the mail address, automatic notifications of new contributions are enabled. With the names, your contributions are displayed together on the interface of schnaq. Further affiliations, for example to the hubs or other schnaqs, are also visually displayed with it."]
    [:p "This data is stored on our own servers and is not passed on to third parties."]
    [:p "There are options to expand your own user profile. This includes, for example, uploading your own optional profile picture. This profile picture is then displayed as your avatar and is presented whenever your user account appears, for example, when people look at your posts."]
    [:h4 "Text contributions"]
    [:p "The text contributions must originate from you and may not violate any copyrights. The text contributions will not be passed on to third parties. Internally, your contributions can be used for further scientific evaluations and the training of our own neural networks. You will never lose your authorship of these contributions. This is used, for example, to automatically calculate machine-generated summaries or statistics. These summaries and statistics are intended for the evaluation of your schnaq and will not be passed on to any third party."]]
   :privacy.extended.matomo/title "Web analysis by Matomo (formerly PIWIK)"
   :privacy.extended.matomo/body
   [:<>
    [:h4 "Description and scope of data processing"]
    [:p "We use the open source software tool Matomo (formerly PIWIK) on our website to analyse the use of our internet presence. For example, we are interested in which pages are accessed how often and whether smartphones, tablets or computers with large screens are used. The software does not set a cookie and does not create a profile of visitors. If individual pages of our website are accessed, the following data is stored:"]
    [:ol
     [:li "Two bytes of the IP address of the calling system"]
     [:li "The accessed web page"]
     [:li "The website through which our website was accessed (referrer)"]
     [:li "The subpages that are called from the called web page"]
     [:li "The time spent on the website"]
     [:li "The frequency of access to the website"]]
    [:p "Matomo is set so that the IP addresses are not stored in full, but two bytes of the IP address are masked (example: 192.168.xxx.xxx). In this way, an assignment of the shortened IP address to the calling computer is no longer possible."]
    [:p "Matomo is used exclusively on schnaq servers. Personal data of the users is only stored there. The data is not passed on to third parties."]
    [:h4 "Purpose of data processing"]
    [:p "The processing of anonymized user data enables us to analyze the use of our website. By evaluating the data obtained, we are able to compile information about the use of the individual components of our website. This helps us to continuously improve our services and their user-friendliness. By anonymizing the IP address, the interest of the user in the protection of his personal data is sufficiently taken into account."]
    [:p "No profiles are created that would give us a deeper insight into the usage behavior of individual users. The evaluation is exclusively anonymized and aggregated so that no conclusion can be drawn about individual persons."]
    [:p "The use of Matomo on our homepage is based on the legal basis of Art. 6 para. 1 lit. f GDPR."]]
   :privacy.extended.rights-of-the-affected/title "Rights of the data subjects"
   :privacy.extended.rights-of-the-affected/body
   [:<>
    [:p "If personal data is processed by you, you are a data subject in the sense of the GDPR and you are entitled to the rights described below. Please address your request, preferably by e-mail, to the above-mentioned data controller."]
    [:p [:strong "Information:"]
     " You have the right to receive from us at any time free information and confirmation of the personal data stored about you and a copy of this information."]
    [:p [:strong "Correction:"]
     " You have the right to rectification and/or completion if the personal data processed concerning you is inaccurate or incomplete."]
    [:p [:strong "Restriction of processing:"]
     " You have the right to request the restriction of processing if one of the following conditions is met:"]
    [:ul
     [:li "You dispute the accuracy of the personal data for a period of time that allows us to verify the accuracy of the personal data."]
     [:li "The processing is unlawful, you object to the erasure of the personal data and request instead the restriction of the use of the personal data."]
     [:li "We no longer need the personal data for the purposes of processing, but you need it to assert, exercise or defend legal claims."]
     [:li "You have objected to the processing pursuant to Art. 21 (1) GDPR and it is not yet clear whether our legitimate grounds outweigh yours."]]
    [:p [:strong "Deletion:"]
     " You have the right to have the personal data concerning you erased without delay, provided that one of the following reasons applies and insofar as the processing is not necessary:"]
    [:ul
     [:li "The personal data were collected or otherwise processed for such purposes for which they are no longer necessary."]
     [:li "You withdraw your consent on which the processing was based and there is no other legal basis for the processing."]
     [:li "You object to the processing pursuant to Article 21(1) of the GDPR and there are no overriding legitimate grounds for the processing, or you object to the processing pursuant to Article 21(2) of the GDPR. "]
     [:li "The personal data have been processed unlawfully."]
     [:li "The deletion of the personal data is necessary for compliance with a legal obligation under Union or Member State law to which we are subject."]
     [:li "The personal data was collected in relation to information society services offered pursuant to Art. 8 (1) GDPR."]]
    [:p [:strong "Data portability:"]
     " You have the right to receive the personal data concerning you that you have provided to the controller in a structured, common and machine-readable format. You also have the right to transfer this data to another controller without hindrance from the controller to whom the personal data was provided. In exercising this right, you also have the right to obtain that the personal data concerning you be transferred directly from us to another controller, insofar as this is technically feasible. The freedoms and rights of other persons must not be affected by this."]
    [:p [:strong "Opposition:"]
     " You have the right to object at any time to the processing of personal data concerning you that is carried out on the basis of Art. 6 (1) lit. f GDPR. We will no longer process the personal data in the event of the objection, unless we can demonstrate compelling legitimate grounds for the processing which override your interests, rights and freedoms, or the processing serves the assertion, exercise or defense of legal claims."]
    [:p [:strong "Revocation of consent:"]
     " You have the right to revoke your declaration of consent under data protection law at any time. The revocation of consent does not affect the lawfulness of the processing carried out on the basis of the consent until the revocation."]]
   :privacy.extended.right-to-complain/title "Right to complain to a supervisory authority"
   :privacy.extended.right-to-complain/body
   [:<>
    [:p "Without prejudice to any other administrative or judicial remedy, you have the right to lodge a complaint with a supervisory authority, in particular in the Member State of your residence, if you consider that the processing of personal data concerning you infringes the GDPR.\nThe data protection supervisory authority responsible for the operator of this site is:"]
    [:p "The State Commissioner for Data Protection and Freedom of Information of North Rhine-Westphalia, Kavalleriestr. 2-4, 40102 Düsseldorf, Tel.: +49211/38424-0, e-mail: poststelle{at}ldi.nrw.de"]]
   :privacy.extended.hosting/title "Hosting der Webseite"
   :privacy.extended.hosting/body
   [:<>
    [:p "The schnaq website is hosted on servers of Hetzner Online GmbH in Germany. For further information, please refer to the websites of Hetzner Online GmbH."]
    [:h4 "Conclusion of a commissioned data processing contract (AV contract)"]
    [:p "We have concluded an AV contract with Hetzner Online GmbH, which protects our customers and obliges Hetzner not to pass on the collected data to third parties."]]
   :privacy.extended.responsible/title "Responsible Person"
   :privacy.extended.responsible/body
   [:<>
    [:p
     "schnaq GmbH i.Gr." [:br]
     "represented by Christian Meter, Alexander Schneider and Michael Birkhoff" [:br]
     "Speditionsstraße 15A" [:br]
     "40221 Düsseldorf" [:br]
     "Germany" [:br]
     (toolbelt/obfuscate-mail "info@schnaq.com")]
    [:p "Legally binding is the German version of this page."]]

   ;; About us
   :about-us.unity/title "The Unit schnaq"
   :about-us.unity/body [:<> [:p "schnaq brings digital discussions into the future. We offer companies the opportunity to conduct transparent decision-making processes in which the entire team can be heard, so that equal-opportunity and comprehensible discourse takes place. Our analytics help you understand which team member has not been heard enough and should be included. By sharing knowledge through discussions on our platform, we prevent knowledge silos and tacit company knowledge by making company knowledge available to all, be it written or later spoken communication."]
                         [:p "Our team is committed to ensuring that every voice can be heard!"]]

   :about-us.value/title "Our Values"
   :about-us.value/subtitle "We follow values that define our actions and our products."
   :about-us.honesty/title "Honesty"
   :about-us.honesty/body "We focus on presenting our products and their capabilities honestly and without exaggeration. We firmly believe that our products can stand for themselves without any exaggeration."
   :about-us.collaborate/title "Will to Collaborate"
   :about-us.collaborate/body "We firmly believe that we can achieve more together than alone. That's why we like to cultivate a culture of collaboration. Whether it's among ourselves as a team or with our customers and cooperation partners. Together we can create great things."
   :about-us.action/title "Drive"
   :about-us.action/body "We don't make decisions out of the blue, but based on all the data we have available. But once a decision has been made after discussions, we stand behind it together and pull together to move forward efficiently."
   :about-us.quality/title "Quality"
   :about-us.quality/body "We are proud of our work and what we create. We like our work, we see it as a part of us and we enjoy connecting people all over the world. That's why we care that our products are of the highest possible quality."
   :about-us.diversity/title "Diversity"
   :about-us.diversity/body "Every person brings their own unique perspective on the world. And precisely because we bring people into contact with each other, we want as many of these perspectives as possible to flow into our work."

   :about-us.numbers/title "schnaq in Numbers"
   :about-us.numbers/research "Years of Research"
   :about-us.numbers/users "Users"
   :about-us.numbers/statements "Statements Structured"
   :about-us.numbers/loc "Lines of Code"

   :about-us.team/title "Focus on the Team"
   :about-us.team/alexander "Co-Founder - Operational Management"
   :about-us.team/christian "Co-Founder - Technical Management"
   :about-us.team/mike "Co-Founder - Product Design Management"

   :about-us.page/heading "About Us"
   :about-us.page/subheading "Information about us"

   ;; Legal Note
   :legal-note.page/heading "Legal Note"
   :legal-note.page/disclaimer "Disclaimer"

   :legal-note.contents/title "Liability for Contents"
   :legal-note.contents/body "As a service provider, we are responsible for our own content on these pages in accordance with general legislation pursuant to Section 7 (1) of the German Telemedia Act (TMG). According to §§ 8 to 10 TMG, however, we are not obligated as a service provider to monitor transmitted or stored third-party information or to investigate circumstances that indicate illegal activity. Obligations to remove or block the use of information under the general laws remain unaffected. However, liability in this regard is only possible from the point in time at which a concrete infringement of the law becomes known. If we become aware of such infringements, we will remove this content immediately."
   :legal-note.links/title "Liability for Links"
   :legal-note.links/body "Our offer contains links to external websites of third parties, on whose contents we have no influence. Therefore, we cannot assume any liability for these external contents. The respective provider or operator of the pages is always responsible for the content of the linked pages. The linked pages were checked for possible legal violations at the time of linking. Illegal contents were not recognizable at the time of linking. However, a permanent control of the contents of the linked pages is not reasonable without concrete evidence of a violation of the law. If we become aware of any infringements, we will remove such links immediately."
   :legal-note.copyright/title "Copyright"
   :legal-note.copyright/body "The content and works created by the site operators on these pages are subject to German copyright law. The reproduction, editing, distribution and any kind of exploitation outside the limits of copyright require the written consent of the respective author or creator. Downloads and copies of this site are only permitted for private, non-commercial use. Insofar as the content on this site was not created by the operator, the copyrights of third parties are respected. In particular, third-party content is identified as such. Should you nevertheless become aware of a copyright infringement, please inform us accordingly. If we become aware of any infringements, we will remove such content immediately."
   :legal-note.privacy/title "Privacy Policy"
   :legal-note.privacy/body "You can find our privacy policy here."

   ;; Celebrations
   :celebrations.schnaq-filled/title "🎉 Congratulations 🎉"
   :celebrations.schnaq-filled/lead "You have filled a new schnaq with a first statement. This is the first milestone towards a successful discussion 💪"
   :celebrations.schnaq-filled/share-now "Now share the schnaq with your team!"
   :celebrations.schnaq-filled/button "Sharing Options"
   :celebrations.first-schnaq-created/title "You have created your first schnaq 🎈"
   :celebrations.first-schnaq-created/lead "Do you want to connect your schnaq to an account? Then register with a few clicks 🚀"

   ;; schnaqs not found
   :schnaqs.not-found/alert-lead "No schnaqs found"
   :schnaqs.not-found/alert-body "Create a schnaq or let yourself be invited"

   ;; Admin Center
   :schnaq/educate-on-link-text "Share the link below with your coworkers."
   :schnaq/educate-on-link-text-subtitle "Everybody with possession of the link can participate."
   :schnaq.admin/heading "Admin-Center"
   :schnaq.admin/subheading "schnaq: \"%s\""
   :schnaq.admin.edit.link/header "Entry to the admin-center"
   :schnaq.admin.edit.link/primer "Administration takes work, let others help!"
   :schnaq.admin.edit.link/admin "Entry to Admin-Center via Email"
   :schnaq.admin.edit.link/admin-privileges "Edit and administer suggestions"
   :schnaq.admin.edit.link.form/label "Email address of the administrators"
   :schnaq.admin.edit.link.form/placeholder "Enter an email address"
   :schnaq.admin.edit.link.form/submit-button "Send link"
   :schnaq.admin.invite/via-link "Distribute Link"
   :schnaq.admin.invite/via-mail "Invite via Email"
   :schnaq.admin.edit/administrate "Administrate schnaq"
   :schnaq.export/as-text "Download schnaq as a text-file"
   :schnaq.admin/tooltip "Administrate schnaq"
   :share-link/copy "Copy share-link"
   :share-link/via "By link"
   :share-access-code/via "By code"
   :share-access-code/title-1 "Visit"
   :share-access-code/title-2 "and enter the following access code:"
   :share-qr-code/via "By QR Code"

   :sharing/tooltip "Share your schnaq"
   :sharing.modal/title "Share your schnaq"
   :sharing.modal/lead "Invite your whole team to fill this schnaq with knowledge."
   :sharing.modal/schnaqqi-help "Fill the schnaq with your ideas. Your colleagues will find it easier to get started."
   :sharing.modal/qanda-help "Participants can ask questions via the Q&A view. Either directly by link or by code on www.schnaq.app!"

   ;; Discussion Language
   :discussion/create-argument-action "Add Statement"
   :discussion/add-argument-conclusion-placeholder "I think that…"
   :discussion/add-premise-supporting "I want to support the statement"
   :discussion/add-premise-against "I disagree…"
   :discussion/add-premise-neutral "I want to add something"
   :discussion.add.button/support "Support"
   :discussion.add.button/attack "Attack"
   :discussion.add.button/neutral "Neutral"
   :discussion.add.statement/new "New post from you"
   :discussion.badges/user-overview "All participants"
   :discussion.badges/delete-statement "Delete statement"
   :discussion.badges/posts "Posts"
   :discussion.badges/delete-statement-confirmation "Do you really want to delete the statement?"
   :discussion.notification/new-content-title "New statement!"
   :discussion.notification/new-content-body "Your statement was added successfully!"
   :discussion.badges/edit-statement "edit"
   :discussion.badges/statement-by "by"
   :discussion.badges/new "New"
   :discussion.button/text "Overview"

   ;; Q & A
   :qanda/add-question-label "State your question"
   :qanda/add-question "Type in your question here …"
   :qanda.button/text "Q&A"
   :qanda.button/submit "Ask question"
   :qanda.state/read-only-warning "This schnaq is read-only, you cannot ask any questions at the moment."
   :qanda.call-to-action/display-code "Participation code:"
   :qanda.call-to-action/intro-1 "Invite more people by navigating to"
   :qanda.call-to-action/intro-2 "and entering the code there."
   :qanda.call-to-action/help "All options for sharing your schnaq can be found in the top right navigation bar"
   :qanda.search/similar-results "Similar Questions"
   :qanda.search/similar-results-explanation-1 "Similar questions that have already been asked appear here. You can mark them as relevant for you with "
   :qanda.search/similar-results-explanation-2 "."
   :qanda.button.mark/as-answer "Mark as answer"
   :qanda.button.mark/as-unanswered "Unmark as answer"

   ;; meetings overview
   :schnaqs/header "Overview of your schnaqs"
   :schnaqs/subheader "These are the schnaqs that you are part of"
   :schnaqs/author "Author"
   :schnaqs/schnaq "schnaq"

   ;; Feedback
   :feedbacks.overview/header "Feedbacks"
   :feedbacks.overview/subheader "All feedbacks"
   :feedbacks.overview/description "Description"
   :feedbacks.overview/table-header "We have %s feedbacks 🥳!"
   :feedbacks.overview/when? "When?"
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

   ;; analytics
   :analytics/heading "Analytics"
   :analytics/overall-discussions "schnaqs created"
   :analytics/user-numbers "Usernames created"
   :analytics/registered-users-numbers "Registered Users"
   :analytics/average-statements-title "Average number of statements / schnaq"
   :analytics/statements-num-title "# of statements"
   :analytics/active-users-num-title "Active users"
   :analytics/statement-lengths-title "Length of statements"
   :analytics/statement-types-title "Argument types"
   :analytics/fetch-data-button "Retrieving data…"

   ;; Supporters
   :supporters/heading "Supported by the Ministry of Economics of the State of North Rhine-Westphalia"

   ;; Testimonials
   :testimonials/heading "Testimonials"
   :testimonials.doctronic/company "doctronic GmbH & Co. KG"
   :testimonials.doctronic/quote "We observe the development of schnaq with great interest for our own use and for the use of our customers."
   :testimonials.doctronic/author "Ingo Küper, Managing Director"

   :testimonials.leetdesk/company "Leetdesk – ODYN GmbH"
   :testimonials.leetdesk/quote "Even with our quite small team, it is helpful to collect our thoughts in order to be able to properly drive a discussion. This has been made possible for us very well by schnaq. More efficient meetings were the result."
   :testimonials.leetdesk/author "Meiko Tse, Managing Director"

   :testimonials.hhu/company "Heinrich Heine University Düsseldorf"
   :testimonials.bjorn/quote "For internal coordination and agreement we used schnaq so that all participants could write down their thoughts and put them into context. Finally, concrete tasks were derived and we could go into the work phase in a structured way."
   :testimonials.bjorn/author "Björn Ebbinghaus, Research assistant"

   :testimonials.lokay/company "Mediator and Conflict Resolution Advisor"
   :testimonials.lokay/quote "I had the honour of giving feedback to the colleagues in the initial phase and I am impressed by their spirited values and practical orientation."
   :testimonials.lokay/author "Oliver Lokay, Mediator and Conflict Resolution Advisor"

   :testimonials.hck/company "Chief Digital Officer"
   :testimonials.hck/quote "As an expert in digital transformation of companies, I quickly recognised the potential of schnaq and have been available to the team as a mentor ever since. A strong idea and a competent founding team that we will for sure be hearing more from!"
   :testimonials.hck/author "Hans-Christoph Kaiser, CDO"

   :testimonials.franky/company "FoxBase GmbH"
   :testimonials.franky/quote "Schnaq's backend is rocket science while its frontend is as simple as riding a tricycle."
   :testimonials.franky/author "Frank Stampa, Head of Sales"

   :testimonials.metro/company "Metro Digital"
   :testimonials.metro/quote "As an asyncronous work evangelist, I really appreciate schnaq for breaking down information silos and making it available to all employees in a transparent and clear way."
   :testimonials.metro/author "Dr. Tobias Schröder, Product Manager"

   :testimonials.eugenbialon/company "EugenBialonArchitekt GmbH"
   :testimonials.eugenbialon/quote "In an architect's office, there are several parallel projects with a large number of players involved. Schnaq supports us in cross-project information management, whether in the office, in the home office or on the construction site!"
   :testimonials.eugenbialon/author "Dipl.-Ing. Eugen Bialon, Managing Partner and Architect, EugenBialonArchitekt GmbH"

   :testimonials.bialon/quote "With schnaq, I am able to process the mass of information regarding the digitisation of a university in a structured and clear way. This allows me to act quickly in any project context."
   :testimonials.bialon/author "Raphael Bialon, Personal Assistant to the Prorector for Digitisation, Heinrich Heine University Düsseldorf"

   :testimonials.sensor/company "Enterprise company in the field of sensor and measurement technology"
   :testimonials.sensor/quote "As part of the familiarisation process with new service products, we used schnaq to collect our ideas and open questions centrally throughout the team. This enabled us to go into exchange meetings well prepared and address specific points.
Now we write down questions and discuss them and can still understand what we have decided three weeks later."
   :testimonials.sensor/author "Florian Clever, Customer Consultant for Service Processes Automation"

   :testimonials.bib/company "Research Assistant"
   :testimonials.bib/quote "We were also able to stimulate discussion and exchange between students at online events through schnaq, which had a significant impact on the success of the events."
   :testimonials.bib/author "Frauke Kling, Research assistant"

   ;; User related
   :user.button/set-name "Save name"
   :user.button/set-name-placeholder "Your name"
   :user.button/change-name "Change name"
   :user.button/success-body "Name saved successfully"
   :user.set-name.modal/header "Please, enter a name"
   :user.set-name.modal/primer "The name will be visible to other participants of the schnaq."
   :user/login "Sign In"
   :user/logout "Logout"
   :user/register "Sign In / Sign Up"
   :user.profile/settings "Settings"
   :user.action/link-copied "Link copied!"
   :user.action/link-copied-body "Share the link with others, to give them access to the schnaq."
   :user/edit-account "Manage Account Information"
   :user/edit-notifications "Manage Notifications"
   :user/edit-hubs "Manage Hubs"
   :user.settings "Settings"
   :user.keycloak-settings "Advanced Settings"
   :user.settings/header "Manage User Data"
   :user.settings/info "User infos"
   :user.settings/notifications "Notifications"
   :user.settings/hubs "Hubs"
   :user.settings/change-name "Change name and profile picture"
   :user.settings.button/change-account-information "Save changes"
   :user.settings.profile-picture-title/success "Profile picture successfully uploaded"
   :user.settings.profile-picture-body/success "Your new profile picture was successfully set. You may have to reload the page to see it."
   :user.settings.profile-picture-title/error "Error while uploading profile picture"
   :user.settings.profile-picture-too-large/error "Your profile picture size is %d bytes, it exceeds the maximum allowed size of %d bytes. Please upload a smaller picture."
   :user.settings.profile-picture.errors/scaling "Your profile picture could not be converted. Maybe the image is corrupt. Please try a different image or contact us."
   :user.settings.profile-picture.errors/invalid-file-type "The image you provided has the wrong file type. Allowed file types: %s"
   :user.settings.profile-picture.errors/default "Something went wrong with the picture you're uploaded. Please try again."

   ;; notification settings
   :user.notifications/header "Manage Notifications"
   :user.notifications/mails "E-Mail Notifications"
   :user.notifications/info "You will only receive notifications when there are new posts in your visited schnaqs."
   :user.notifications.set-all-to-read/button "Mark everything as read"
   :user.notifications.set-all-to-read/info "You still receive notifications from old discussions? No problem, just mark everything as read and only receive notifications from new discussions."

   ; mail interval
   :notification-mail-interval/daily "Daily"
   :notification-mail-interval/weekly "Weekly"
   :notification-mail-interval/never "Never"

   ;; Errors
   :errors/generic "An error occurred"

   :error.generic/contact-us
   [:<> "Did you end up here after clicking something on schnaq.com? Give us a hint at " [:a {:href "mailto:info@schnaq.com"} "info@schnaq.com"]]

   :error.404/heading "This site does not exist 🙉"
   :error.404/body "The URL that you followed does not exist. Maybe there is a typo."

   :error.403/heading "You do not have the rights to view this site 🧙‍♂️"
   :error.403/body "You either have insufficient rights to view this site, or a typo happened."

   :error.beta/heading "You do not have the rights to view this site 🧙‍♂️"
   :error.beta/body "Only schnaq beta-testers can access this page. If you are one, please log in. If you would like to be a beta-tester, write us an email at hello@schnaq.com."

   ;; Graph Texts
   :graph.button/text "Mindmap"
   :graph.download/as-png "Download mindmap as image"
   :graph.settings/title "Settings for your Mindmap"
   :graph.settings/description "Here are some settings for your Mindmap! Play around with the sliders and let the magic happen."
   :graph.settings.gravity/label "Adjust the gravity between your nodes."
   :graph.settings/stabilize "Stabilize Mindmap"

   ;; Pricing Page
   :pricing.intro/heading "Starting soon!"
   :pricing.intro/lead [:span "You will soon be able to book your plan here. Would you like to be part of our beta and test the business plan exclusively and free of charge? Then please contact us at " [:a {:href "mailto:hello@schnaq.com"} "hello@schnaq.com!"]]
   :pricing.free-tier/title "Free"
   :pricing.free-tier/subtitle "Forever"
   :pricing.free-tier/description "For efficient makers and small teams, that don't need a lot. Create discussions and Q&As with two clicks."
   :pricing.free-tier/beta-notice "This plan stays free forever. Need more features? Upgrade easily with a few clicks."
   :pricing.free-tier/call-to-action "Start Free of Charge"
   :pricing.free-tier/for-free "Free forever"
   :pricing.pro-tier/title "Pro"
   :pricing.pro-tier/subtitle "Activate your potential"
   :pricing.pro-tier/description "Gain full control over your discussions and Q&As and completley understand your participants."
   :pricing.pro-tier/call-to-action "Test Business now"
   :pricing.enterprise-tier/title "Enterprise"
   :pricing.enterprise-tier/subtitle "Big plans?"
   :pricing.enterprise-tier/description "Optimize the communication of your company. Special wishes and requirements can be accomodated for easily."
   :pricing.enterprise-tier/call-to-action "Send Inquiry"
   :pricing.enterprise-tier/on-request "On Request"
   :pricing.features/implemented "Already implemented"
   :pricing.features/to-be-implemented "Soon available"
   :pricing.features/starter ["Hosted in Germany" "Unlimited schnaqs" "Unlimited participants" "Create Discussions" "Hold Q&As" "Automatic Mindmap" "Shareable by Link, QR Code and digit code" "Text and Image Export"]
   :pricing.features/business ["Analysis Dashboard" "A.I. Summaries" "Personal Spaces" "Moderation Options"]
   :pricing.features/enterprise ["Embedding in existing systems" "SSO Login (OpenID, LDAP, ...)" "Whitelabelling" "On-Premise"]
   :pricing.features/upcoming ["A.I. Sentiment Analysis" "Integrations"]
   :pricing.units/per-month "/ month"
   :pricing.notes/with-vat "plus VAT"
   :pricing.trial/call-to-action "Test the pro plan 30 days free of charge"
   :pricing.trial/description "No credit card needed! Cancel anytime."
   :pricing.trial.temporary/deactivation "Available from 22.11.2021"
   :pricing.features/heading "Schnaq subscription advantages"
   :pricing.features.user-numbers/heading "Unlimited member accounts"
   :pricing.features.user-numbers/content "Set no bounds for how many people can collaborate."
   :pricing.features.team-numbers/heading "Unlimited number of teams"
   :pricing.features.team-numbers/content "Create as many teams as are needed for your projects. *"
   :pricing.features.app-integration/heading "App integration"
   :pricing.features.app-integration/content "Connect schnaq easily to other software you use."
   :pricing.features.analysis/heading "Automatic Analyses"
   :pricing.features.analysis/content "All discussions are automatically analyzed and presented to the participants. *"
   :pricing.features.knowledge-db/heading "Knowledge Database"
   :pricing.features.knowledge-db/content "Collect all your knowledge in one spot."
   :pricing.features.mindmap/heading "Interactive mindmap"
   :pricing.features.mindmap/content "All statements are automatically structured and shown in an interactive mindmap."
   :pricing.features/disclaimer "* Applies only to pro plan"
   :pricing.competitors/per-month-per-user " € per month per user"
   :pricing/headline "Subscriptions"
   :pricing.newsletter/lead "Subscribe to the newsletter and be informed as soon as the plans go live:"
   :pricing.newsletter/name "schnaq newsletter."

   ;; tooltips
   :tooltip/history-statement "Back to statement made by"
   :tooltip/history-statement-current "Current statement"

   ;; History
   :history/title "History"
   :history.home/text "Start"
   :history.home/tooltip "Back to the discussion's beginning"
   :history.statement/user "Post from"
   :history.all-schnaqs/tooltip "Back to all schnaqs"
   :history.back/tooltip "Back to previous post"

   ;; Route Link Texts
   :router/admin-center "Admin-Center"
   :router/all-feedbacks "All feedbacks"
   :router/analytics "Analytics dashboard"
   :router/create-schnaq "Create schnaq"
   :router/value "Value of schnaq"
   :router/dashboard "schnaq dashboard"
   :router/graph-view "Graph view"
   :router/how-to "How do I use schnaq?"
   :router/last-added-schnaq "Last created schnaq"
   :router/visited-schnaqs "Visited schnaqs"
   :router/not-found-label "Not found route redirect"
   :router/pricing "Pricing"
   :router/privacy "Privacy Policy"
   :router/qanda "Q&A"
   :router/start-discussion "Start discussion"
   :router/startpage "Startpage"
   :router/true-404-view "404 error page"
   :router/code-of-conduct "Code of Conduct"
   :router/summaries "Summaries"
   :router/alphazulu "ALPHAZULU"

   :admin.center.start/title "Admin Center"
   :admin.center.start/heading "Admin Center"
   :admin.center.start/subheading "Administrate schnaqs as a superuser"
   :admin.center.delete/confirmation "Do you really want to delete this schnaq?"
   :admin.center.delete.public/button "Delete schnaq"
   :admin.center.delete/heading "Deletion"
   :admin.center.delete.private/label "Share-hash"
   :admin.center.delete.private/heading "Private schnaqs"

   :badges.filters/label "Display"
   :badges/sort "Sorting"
   :badges.sort/newest "Newest"
   :badges.sort/popular "Popular"
   :badges.sort/alphabetical "Alphabetical"
   :badges.filters/button "Filters"

   :filters.label/filter-for "Filter for"
   :filters.add/button "Add Filter"
   :filters.option.labels/text "labels"
   :filters.option.labels/includes "includes"
   :filters.option.labels/excludes "excludes"
   :filters.option.type/text "statement type"
   :filters.option.type/is "is"
   :filters.option.type/is-not "is not"
   :filters.option.votes/text "votes"
   :filters.option.vote/bigger "more than"
   :filters.option.vote/equal "equal"
   :filters.option.vote/less "less than"
   :filters.option.answered/all "All Statements"
   :filters.option.answered/answered "Answered"
   :filters.option.answered/unanswered "Unanswered"
   :filters.buttons/clear "Clear all filters"
   :filters.heading/active "Active Filters"

   ;; Labels for programattically created text in label overview
   :filters.labels.type/labels "Labels"
   :filters.labels.type/type "Statement type"
   :filters.labels.type/votes "Votes"
   :filters.labels.criteria/includes "include"
   :filters.labels.criteria/excludes "exclude"
   :filters.labels.criteria/is "is"
   :filters.labels.criteria/is-not "is not"
   :filters.labels.criteria/> "are more than"
   :filters.labels.criteria/= "equal"
   :filters.labels.criteria/< "are less than"
   :filters.stype/neutral "neutral"
   :filters.stype/attack "attack"
   :filters.stype/support "support"

   :filters.discussion.option.state/label "Schnaq state"
   :filters.discussion.option.state/closed "closed"
   :filters.discussion.option.state/read-only "read only"
   :filters.discussion.option.numbers/label "Number of statements"
   :filters.discussion.option.author/label "Own participation"
   :filters.discussion.option.author/prelude "I am"
   :filters.discussion.option.author/included "participating"
   :filters.discussion.option.author/excluded "not participating"
   ;; Auto-generation of pretty-labels
   :filters.labels.criteria/included "participating"
   :filters.labels.criteria/excluded "not participating"
   :filters.labels.type/state "Schnaq state"
   :filters.labels.type/numbers "Statement-number"
   :filters.labels.type/author "You are"

   :loading.placeholder/lead "Loading..."
   :loading.placeholder/takes-too-long "This takes longer than expected. Maybe something went wrong. Try to reload the page or repeat the process again. If you still have problems, please contact us!"

   :hubs/heading "Hubs"
   :hub/heading "Personal %s Hub"
   :hub/settings "Administration"
   :hub.settings/change-name "Change hub's name"
   :hub.settings.name/updated-title "Change name of hub"
   :hub.settings.name/updated-body "The name of the hub was successfully updated!"
   :hub.settings.update-logo-title/success "The logo of the hub was successfully updated!"
   :hub.settings.update-logo-body/success "Your new hub logo was successfully set. You may have to reload the page to see it."
   :hub.settings/save "Save Settings"
   :hub.add.schnaq.success/title "Schnaq added!"
   :hub.add.schnaq.success/body "The schnaq has been added to your hub successfully."
   :hub.add.schnaq.error/title "schnaq was not added!"
   :hub.add.schnaq.error/body "The schnaq could not be added or found. Please check your input and try again."
   :hub.add.schnaq.input/button "Add schnaq"
   :hub.add.schnaq.input/placeholder "schnaq-URL e.g. https://schnaq.com/schnaq/… or share-code"
   :hub.remove.schnaq.success/title "schnaq removed!"
   :hub.remove.schnaq.success/body "The schnaq has been removed from your hub."
   :hub.remove.schnaq.error/title "Removal failed!"
   :hub.remove.schnaq.error/body "Something went wrong. We were unable to remove the schnaq. Please try again."
   :hub.remove.schnaq/prompt "Do you really want to remove the schnaq from the hub?"
   :hub.remove.schnaq/tooltip "Remove the schnaq from hub"
   :hub.members/heading "Members"

   :hub.members.add.result.success/title "Success"
   :hub.members.add.result.success/body "User was added to the Hub"
   :hub.members.add.result.error/title "Error"
   :hub.members.add.result.error/unregistered-user "There seems to be no user with the email address you entered"
   :hub.members.add.result.error/generic-error "Something went wrong. Please check the email you entered and try again"
   :hub.members.add.form/title "Add Members"
   :hub.members.add.form/button "Add user now!"

   :schnaq.search/heading "Search results"
   :schnaq.search/results "results"
   :schnaq.search/input "Search for…"
   :schnaq.search/new-search-title "No results"

   :lead-magnet.privacy/consent "I want to receive a checklist of tools which comply with german privacy laws as .pdf file in German via mail and register for the newsletter as well. The newsletter is currently available in German only."
   :lead-magnet.form/button "Send me a copy!"
   :lead-magnet/heading "German laws take privacy very seriously and so do we"
   :lead-magnet/subheading "Our checklist for working remotely according to german privacy laws (in German only)"
   :lead-magnet.cover/alt-text "cover of the checklist for working remotely according to german privacy laws"
   :lead-magnet.form/label "We will send a download link via mail"
   :lead-magnet.requested/part-1 "First step done to comply with german privacy laws when working remotely!"
   :lead-magnet.requested/part-2 "You should receive the download link within a few minutes. Please check your spam folder in case you did not receive any mail."
   :lead-magnet.cta/button "Take me to the checklist!"
   :lead-magnet.explain.what/heading "What is the checklist for privacy sensitive remote work?"
   :lead-magnet.explain.what/text "We checked current software for conformity with european GDPR regulations.
        The results are summarized in this checklist. We included all categories of software that are helpful while working remotely.
        This way you can take privacy into account while working from home, the office or in a hybrid fashion."
   :lead-magnet.explain.how/heading "How does the checklist work?"
   :lead-magnet.explain.how/text "The checklist is structured in categories. Every category, like e.g. chat-software, contains several alternatives.
        Every alternative is marked with a color. Green shows that there are no privacy concerns. While red warns about
        several major concerns. Orange signals a few minor problems, but an overall okay result.
        Besides the color ranking we've included a few keywords which can be used for further research."

   :summary.link.button/text "Dashboard"
   :summary.user.request-succeeded/label "Summary requested, please wait."
   :summary.user/computation-time "The creation of the summary can take a few minutes."
   :summary.user.requested/label "Requesting summary"
   :summary.user.not-requested/label "Request summary"
   :summary.user.abort/confirm "The calculation can take several minutes. Do you really want to cancel?"
   :summary.user.abort/label "Problems with the calculation?"
   :summary.user.abort/button "Cancel"
   :summary.user/privacy-warning "For improvement, schnaq employees will be able to view and review summary content confidentially."
   :summary.user/label "Summary:"
   :summary.user/last-updated "Last updated:"
   :summary.user/heading "Summaries"
   :summary.user/subheading "See the discussion in a few sentences"
   :summary.admin/open-summaries "Open Summaries: %s"
   :summary.admin/closed-summaries "Closed Summaries: %s"
   :summary.admin/discussion "Discussion"
   :summary.admin/requester "Requester"
   :summary.admin/requested-at "Requested at"
   :summary.admin/summary "Summary"
   :summary.admin/submit "Submit"
   :summary.admin/closed-at "Closed at"})
