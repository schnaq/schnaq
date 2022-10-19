(ns schnaq.interface.components.icons
  ;; For further information check: https://fontawesome.com/v5.15/how-to-use/on-the-web/using-with/react
  ;; For two styles of the same icon see here: https://fontawesome.com/v5.15/how-to-use/on-the-web/using-with/react#faqs
  (:require ["@fortawesome/free-brands-svg-icons" :refer [faFacebook
                                                          faFontAwesomeFlag faGithub
                                                          faInstagram faLinkedin faTwitter]]
            ["@fortawesome/free-regular-svg-icons" :refer [faCalendar faSmileBeam
                                                           faCommentAlt faEnvelope
                                                           faEye faEyeSlash faFileAlt faFileImage faFileVideo faHourglass faIdCard faImage]]
            ["@fortawesome/free-solid-svg-icons" :refer
             [faAngleDown faAngleRight faArchive faArrowDown faArrowLeft
              faArrowRight faArrowUp faBackspace faBell faBold faBriefcase
              faBullseye faCalendarAlt faCamera faChalkboardTeacher faChartPie faCheck faCheckCircle
              faChevronLeft faChevronRight faCircle faCloud faCookieBite faCode faCog faComment faCopy
              faEdit faEllipsisH faEllipsisV faExclamationTriangle faExternalLinkAlt faFileDownload faFlask faGhost
              faGraduationCap faInfinity faInfoCircle faItalic faLanguage faLaptop faList faListOl
              faLock faLockOpen faMagic faMapPin faMinus faPalette faPaperPlane faPenSquare faPencilRuler
              faPlayCircle faPlus faProjectDiagram faSquare faQrcode faQuestion faQuestionCircle
              faQuoteRight faRedo faRocket faSearch faShareAlt faShieldAlt faSlidersH faStar
              faStepBackward faStrikethrough faSun faTag faTerminal faTimes faTimes faTimesCircle
              faTrashAlt faUnderline faUndo faUniversity faUsers]]
            ["@fortawesome/react-fontawesome" :refer [FontAwesomeIcon]]
            [schnaq.interface.utils.tooltip :as tooltip]))

(def ^:private icons
  {:archive faArchive
   :arrow-down faArrowDown
   :arrow-left faArrowLeft
   :arrow-right faArrowRight
   :arrow-up faArrowUp
   :backspace faBackspace
   :bell faBell
   :bold faBold
   :briefcase faBriefcase
   :bullseye faBullseye
   :calendar faCalendar
   :calendar-alt faCalendarAlt
   :camera faCamera
   :chalkboard-teacher faChalkboardTeacher
   :chart-pie faChartPie
   :check/circle faCheckCircle
   :check/normal faCheck
   :chevron/left faChevronLeft
   :chevron/right faChevronRight
   :circle faCircle
   :cloud faCloud
   :cookie-bite faCookieBite
   :code faCode
   :cog faCog
   :collapse-down faAngleDown
   :collapse-up faAngleRight
   :comment faComment
   :comment/alt faCommentAlt
   :copy faCopy
   :cross faTimes
   :delete-icon faTimesCircle
   :dots faEllipsisH
   :dots-v faEllipsisV
   :edit faEdit
   :eye faEye
   :eye-slash faEyeSlash
   :envelope faEnvelope
   :exclamation-triangle faExclamationTriangle
   :external-link-alt faExternalLinkAlt
   :facebook faFacebook
   :file-alt faFileAlt
   :file-download faFileDownload
   :flag faFontAwesomeFlag
   :flask faFlask
   :ghost faGhost
   :graduation-cap faGraduationCap
   :graph faProjectDiagram
   :github faGithub
   :hourglass/empty faHourglass
   :id-card faIdCard
   :image faImage
   :image-file faFileImage
   :infinity faInfinity
   :info faInfoCircle
   :info-question faQuestionCircle
   :instagram faInstagram
   :italic faItalic
   :language faLanguage
   :laptop faLaptop
   :linkedin faLinkedin
   :list faList
   :list-ol faListOl
   :lock faLock
   :lock/open faLockOpen
   :magic faMagic
   :minus faMinus
   :palette faPalette
   :pen faPenSquare
   :pencil-ruler faPencilRuler
   :pin faMapPin
   :plane faPaperPlane
   :play/circle faPlayCircle
   :plus faPlus
   :question faQuestion
   :qrcode faQrcode
   :quote-right faQuoteRight
   :redo faRedo
   :reset faStepBackward
   :rocket faRocket
   :search faSearch
   :sliders-h faSlidersH
   :share faShareAlt
   :shield faShieldAlt
   :smile-beam faSmileBeam
   :square faSquare
   :star faStar
   :strike-through faStrikethrough
   :sun faSun
   :tag faTag
   :terminal faTerminal
   :times faTimes
   :trash faTrashAlt
   :twitter faTwitter
   :underline faUnderline
   :undo faUndo
   :university faUniversity
   :user/group faUsers
   :video-file faFileVideo})

(defn icon
  "The core icon building-block. Pass extra-attributes as a third parameter.
  e.g. `{:size \"lg\"
         :rotation 180}`"
  ([identifier]
   [icon identifier ""])
  ([identifier classes]
   [icon identifier classes {}])
  ([identifier classes extras]
   [:> FontAwesomeIcon
    (merge
     {:icon (get icons identifier)
      :className classes}
     extras)]))

(defn icon-with-tooltip
  "Add an icon with a tooltip on mouseover."
  [tooltip identifier classes extras]
  [tooltip/text
   tooltip
   [:span [icon identifier classes extras]]])

(defn icon-card
  "Wrap an icon into a panel to emphasize it. Takes same parameters as `icon`."
  ([identifier]
   [icon-card identifier ""])
  ([identifier classes]
   [icon-card identifier classes {}])
  ([identifier classes extras]
   [:span.icon-card
    [icon identifier classes extras]]))

(defn stacked-icon
  "Build a stacked icon."
  [& {:keys [props vertical? icon-key]}]
  [:div.fa-stack.small (if vertical?
                         (assoc props :className "d-block mx-auto")
                         props)
   [icon :square "fa-stack-2x text-white"]
   [icon icon-key "fa-stack-1x text-dark"]])
