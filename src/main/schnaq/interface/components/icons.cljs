(ns schnaq.interface.components.icons
  ;; For further information check: https://fontawesome.com/v5.15/how-to-use/on-the-web/using-with/react
  ;; For two styles of the same icon see here: https://fontawesome.com/v5.15/how-to-use/on-the-web/using-with/react#faqs
  (:require ["@fortawesome/free-brands-svg-icons" :refer [faFontAwesomeFlag faGithub faLinkedin]]
            ["@fortawesome/free-regular-svg-icons" :refer [faCalendar faSmileBeam
                                                           faCommentAlt faEnvelope faNewspaper
                                                           faEye faEyeSlash faFileAlt faFileImage faFileVideo faHourglass faIdCard faImage]]
            ["@fortawesome/free-solid-svg-icons" :refer
             [faAngleDown faAngleRight faArchive faArrowDown faArrowDownWideShort faArrowLeft faAward
              faArrowRight faArrowUp faBackspace faBell faBold faBriefcase
              faBalanceScale faBalanceScaleRight faBullseye faCalendarAlt faCamera faChalkboardTeacher faChartPie faCheck faCheckCircle
              faChevronLeft faChevronRight faCircle faCloud faCookieBite faCode faCog faComment faComments faCopy
              faEdit faEllipsisH faEllipsisV faExclamationTriangle faExternalLinkAlt faFileDownload faFileExport faFilter faFlask faGhost
              faGraduationCap faInfinity faInfoCircle faItalic faLanguage faLayerGroup faLaptop faList faListOl
              faLock faLockOpen faMagic faMapPin faMinus faPalette faPaperPlane faPenSquare faPencilAlt faPencilRuler
              faPlayCircle faPlus faProjectDiagram faQrcode faQuestion faQuestionCircle
              faQuoteRight faRedo faRocket faSearch faShareAlt faShieldAlt faSignInAlt faSlidersH
              faStepBackward faStrikethrough faSun faTableCellsLarge faTag faTerminal faThumbsDown faThumbsUp faTimes faTimesCircle
              faTrashAlt faUnderline faUndo faUniversity faUsers faUserPlus]]
            ["@fortawesome/react-fontawesome" :refer [FontAwesomeIcon]]
            [schnaq.interface.utils.tooltip :as tooltip]))

(def ^:private icons
  {:archive faArchive
   :arrow-down faArrowDown
   :arrow-down-wide-short faArrowDownWideShort
   :arrow-left faArrowLeft
   :arrow-right faArrowRight
   :arrow-up faArrowUp
   :award faAward
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
   :comments faComments
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
   :feedback faBalanceScaleRight
   :file-alt faFileAlt
   :file-export faFileExport
   :file-download faFileDownload
   :filter faFilter
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
   :italic faItalic
   :language faLanguage
   :layer-group faLayerGroup
   :laptop faLaptop
   :linkedin faLinkedin
   :list faList
   :list-ol faListOl
   :lock faLock
   :lock/open faLockOpen
   :magic faMagic
   :minus faMinus
   :newspaper faNewspaper
   :palette faPalette
   :pen faPenSquare
   :pencil faPencilAlt
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
   :sign-in faSignInAlt
   :sliders-h faSlidersH
   :share faShareAlt
   :shield faShieldAlt
   :smile-beam faSmileBeam
   :strike-through faStrikethrough
   :scale faBalanceScale
   :sun faSun
   :table-cells-large faTableCellsLarge
   :thumbs-down faThumbsDown
   :thumbs-up faThumbsUp
   :tag faTag
   :terminal faTerminal
   :times faTimes
   :trash faTrashAlt
   :underline faUnderline
   :undo faUndo
   :university faUniversity
   :user/group faUsers
   :user-plus faUserPlus
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
