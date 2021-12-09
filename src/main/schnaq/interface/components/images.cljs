(ns schnaq.interface.components.images)

(defn img-path
  "Returns an image path as String for a given identifier"
  [identifier]
  (identifier
   {:alphazulu/logo "https://s3.schnaq.com/alphazulu/alphazulu_logo.png"
    :alphazulu.cobago/logo "https://s3.schnaq.com/alphazulu/cobago_logo.png"
    :alphazulu.ec3l/logo "https://s3.schnaq.com/alphazulu/ec3l_logo.png"
    :alphazulu.trustcerts/logo "https://s3.schnaq.com/alphazulu/trustcerts_logo.png"
    :alphazulu.wetog/logo "https://s3.schnaq.com/alphazulu/wetog_logo.png"
    :alphazulu.xignsys/logo "https://s3.schnaq.com/alphazulu/xignsys_logo.png"
    :how-to/taskbar "https://s3.schnaq.com/schnaq-common/howto/taskbar.svg"
    :icon-add "https://s3.schnaq.com/schnaq-common/buttons/add-button.svg"
    :icon-cards-dark "https://s3.schnaq.com/schnaq-common/icons/squares_dark.svg"
    :icon-community "https://s3.schnaq.com/schnaq-common/community.svg"
    :icon-crane "https://s3.schnaq.com/schnaq-common/icons/crane.svg"
    :icon-graph "https://s3.schnaq.com/schnaq-common/icons/mind_map_circle.svg"
    :icon-graph-dark "https://s3.schnaq.com/schnaq-common/icons/mind_map_circle_dark.svg"
    :icon-posts "https://s3.schnaq.com/schnaq-common/icons/dashboard/posts.svg"
    :icon-qanda-dark "https://s3.schnaq.com/schnaq-common/icons/qanda_dark.svg"
    :icon-reports "https://s3.schnaq.com/schnaq-common/icons/reports.svg"
    :icon-robot "https://s3.schnaq.com/schnaq-common/icons/robot.svg"
    :icon-search "https://s3.schnaq.com/schnaq-common/icons/dashboard/search.svg"
    :icon-summary "https://s3.schnaq.com/schnaq-common/icons/layers.svg"
    :icon-summary-dark "https://s3.schnaq.com/schnaq-common/icons/layers_dark.svg"
    :icon-users "https://s3.schnaq.com/schnaq-common/icons/dashboard/users.svg"
    :icon-views-dark "https://s3.schnaq.com/schnaq-common/icons/views_dark.svg"
    :icon-views-light "https://s3.schnaq.com/schnaq-common/icons/views_light.svg"
    :logo "https://s3.schnaq.com/schnaq-common/logos/schnaq.svg"
    :logo-white "https://s3.schnaq.com/schnaq-common/logos/schnaq_white.png"
    :logo.square.schnaqqi/blue "https://s3.schnaq.com/schnaq-common/logos/schnaqqi-qr.png"
    :logos/bialon "https://s3.schnaq.com/schnaq-common/testimonials/bialon_logo.png"
    :logos/digihub "https://s3.schnaq.com/schnaq-common/logos/digihub_logo.png"
    :logos/doctronic "https://s3.schnaq.com/schnaq-common/testimonials/doctronic_logo.png"
    :logos/franky "https://s3.schnaq.com/schnaq-common/testimonials/foxbase_logo.svg"
    :logos/frauke "https://s3.schnaq.com/schnaq-common/testimonials/library_logo.jpg"
    :logos/hck "https://s3.schnaq.com/schnaq-common/testimonials/hck.jpg"
    :logos/hetzner "https://s3.schnaq.com/schnaq-common/logos/logo-hetzner.svg"
    :logos/hhu "https://s3.schnaq.com/schnaq-common/testimonials/hhu_logo.png"
    :logos/ignition "https://s3.schnaq.com/schnaq-common/logos/ignition_logo.png"
    :logos/leetdesk "https://s3.schnaq.com/schnaq-common/testimonials/leetdesk_logo.png"
    :logos/lokay "https://s3.schnaq.com/schnaq-common/testimonials/lokay_logo.jpg"
    :logos/metro "https://s3.schnaq.com/schnaq-common/testimonials/metro_logo.svg"
    :logos/sensor "https://s3.schnaq.com/schnaq-common/testimonials/sensor.jpg"
    :press-kit/fact-sheet "https://s3.schnaq.com/schnaq-presskit/factsheet_preview.png"
    :press-kit/logo "https://s3.schnaq.com/schnaq-presskit/logo_card.png"
    :press-kit/product "https://s3.schnaq.com/schnaq-presskit/schnaq_liste_auf_tablet.jpg"
    :press-kit/team "https://s3.schnaq.com/schnaq-presskit/team.jpg"
    :pricing.others/confluence "https://s3.schnaq.com/schnaq-common/startpage/pricing/confluence.jpeg"
    :pricing.others/loomio "https://s3.schnaq.com/schnaq-common/startpage/pricing/loomio.png"
    :pricing.others/miro "https://s3.schnaq.com/schnaq-common/startpage/pricing/miro.png"
    :schnaqqifant.300w/talk "https://s3.schnaq.com/schnaq-schnaqqifanten/talk300w.png"
    :schnaqqifant/admin "https://s3.schnaq.com/schnaq-schnaqqifanten/admin.png"
    :schnaqqifant/erase "https://s3.schnaq.com/schnaq-schnaqqifanten/erase.png"
    :schnaqqifant/flat "https://s3.schnaq.com/schnaq-schnaqqifanten/schnaqqi_flat_front.png"
    :schnaqqifant/hippie "https://s3.schnaq.com/schnaq-schnaqqifanten/schnaqqifant-hippie.png"
    :schnaqqifant/mail "https://s3.schnaq.com/schnaq-schnaqqifanten/schnaqqi_newsletter.png"
    :schnaqqifant/original "https://s3.schnaq.com/schnaq-common/logos/schnaqqifant.svg"
    :schnaqqifant/police "https://s3.schnaq.com/schnaq-schnaqqifanten/schnaqqifant-polizei.png"
    :schnaqqifant/share "https://s3.schnaq.com/schnaq-schnaqqifanten/share.png"
    :schnaqqifant/stop "https://s3.schnaq.com/schnaq-schnaqqifanten/stop.png"
    :schnaqqifant/talk "https://s3.schnaq.com/schnaq-schnaqqifanten/talk.png"
    :schnaqqifant/three-d-head "https://s3.schnaq.com/schnaq-schnaqqifanten/schnaqqi-3d-head.png"
    :schnaqqifant/three-d-left "https://s3.schnaq.com/schnaq-schnaqqifanten/schnaqqi-3d-head-left.png"
    :schnaqqifant/white "https://s3.schnaq.com/schnaq-common/logos/schnaqqifant_white.png"
    :startpage.alternatives.e-learning/alex "https://s3.schnaq.com/startpage/alex.jpeg"
    :startpage.alternatives.e-learning/christian "https://s3.schnaq.com/startpage/christian.jpg"
    :startpage.alternatives.e-learning/david "https://s3.schnaq.com/startpage/david.jpeg"
    :startpage.alternatives.e-learning/header "https://s3.schnaq.com/startpage/schuelerin_laptop_in_hand.png"
    :startpage.alternatives.e-learning/mike "https://s3.schnaq.com/startpage/mike.jpg"
    :startpage.alternatives.e-learning/oma "https://s3.schnaq.com/startpage/old_woman_smartphone_with_schnaq.jpg"
    :startpage.alternatives.e-learning/student-smartphone "https://s3.schnaq.com/startpage/student_smartphone.jpg"
    :startpage.example/dashboard "https://s3.schnaq.com/schnaq-common/startpage/screenshots/example_dashboard.jpg"
    :startpage.example/discussion "https://s3.schnaq.com/schnaq-common/startpage/screenshots/example_discussion.jpg"
    :startpage.example/statements "https://s3.schnaq.com/schnaq-common/startpage/screenshots/all_statements.png"
    :startpage.features/admin-center "https://s3.schnaq.com/startpage/features/admin-center.png"
    :startpage.information/anywhere "https://s3.schnaq.com/startpage/startpage_mobile_work.jpeg"
    :startpage.information/meeting "https://s3.schnaq.com/startpage/startpage-meeting.jpeg"
    :startpage.screenshots/qanda "https://s3.schnaq.com/startpage/screenshots/qanda.png"
    :startpage/answer-schnaq "https://s3.schnaq.com/startpage/answer-schnaq.png"
    :startpage/create-schnaq "https://s3.schnaq.com/startpage/create-schnaq.png"
    :startpage/share-schnaq "https://s3.schnaq.com/startpage/share-schnaq.png"
    :startpage/team-schnaq "https://s3.schnaq.com/startpage/schnaq-team.png"
    :stock/team "https://s3.schnaq.com/startpage/team.jpeg"
    :team/alexander "https://s3.schnaq.com/team/alexanderschneider.jpg"
    :team/at-table-with-laptop "https://s3.schnaq.com/team/team_hinter_laptop_am_tisch.jpg"
    :team/christian "https://s3.schnaq.com/team/christianmeter.jpg"
    :team/mike "https://s3.schnaq.com/team/michaelbirkhoff.jpg"
    :team/sitting-on-couches "https://s3.schnaq.com/team/team_auf_couches.jpg"
    :team/vision-mindmap-team "https://s3.schnaq.com/team/vision_mindmap_team.jpg"
    :testimonial-picture/bjorn "https://s3.schnaq.com/schnaq-common/testimonials/bjorn_picture.jpg"
    :testimonial-picture/eugen-bialon "https://s3.schnaq.com/schnaq-common/testimonials/eugen_bialon_picture.jpg"
    :testimonial-picture/florian-clever "https://s3.schnaq.com/schnaq-common/testimonials/florian_picture.jpg"
    :testimonial-picture/frank-stampa "https://s3.schnaq.com/schnaq-common/testimonials/frank_stampa_picture.jpg"
    :testimonial-picture/frauke-kling "https://s3.schnaq.com/schnaq-common/testimonials/frauke_picture.jpg"
    :testimonial-picture/hck "https://s3.schnaq.com/schnaq-common/testimonials/hck_picture.jpg"
    :testimonial-picture/ingo-kupers "https://s3.schnaq.com/schnaq-common/testimonials/ingo_kupers_picture.jpg"
    :testimonial-picture/lokay "https://s3.schnaq.com/schnaq-common/testimonials/lokay_picture.jpeg"
    :testimonial-picture/meiko-tse "https://s3.schnaq.com/schnaq-common/testimonials/meiko_picture.jpg"
    :testimonial-picture/raphael-bialon "https://s3.schnaq.com/schnaq-common/testimonials/bialon_picture.jpg"
    :testimonial-picture/tobias-schroeder "https://s3.schnaq.com/schnaq-common/testimonials/tobias_picture.jpg"
    :value/book "https://s3.schnaq.com/schnaq-common/icons/value/book.svg"
    :value/bubble "https://s3.schnaq.com/schnaq-common/icons/value/bubble.svg"
    :value/cards "https://s3.schnaq.com/schnaq-common/icons/value/cards.svg"
    :value/share "https://s3.schnaq.com/schnaq-common/icons/value/share.svg"
    :value/shield "https://s3.schnaq.com/schnaq-common/icons/value/shield.svg"
    :value/private "https://s3.schnaq.com/schnaq-common/icons/value/eye.svg"
    :lead-magnet/cover "https://s3.schnaq.com/downloads/checkliste_cover.png"}))
