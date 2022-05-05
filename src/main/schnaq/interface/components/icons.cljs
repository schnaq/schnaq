(ns schnaq.interface.components.icons
  ;; For further information check: https://fontawesome.com/v5.15/how-to-use/on-the-web/using-with/react
  ;; For two styles of the same icon see here: https://fontawesome.com/v5.15/how-to-use/on-the-web/using-with/react#faqs
  (:require ["@fortawesome/free-brands-svg-icons" :refer [faFacebook faInstagram faLinkedin faTwitter faGithub faFontAwesomeFlag]]
            ["@fortawesome/free-regular-svg-icons" :refer [faCalendar faCommentAlt faEye faEnvelope faIdCard faHourglass]]
            ["@fortawesome/free-solid-svg-icons" :refer
             [faArchive faBackspace faArrowLeft faArrowRight faArrowDown faArrowUp faBell faBriefcase faBuilding faCalendarAlt faCamera faChalkboardTeacher faChartPie faCheck faCheckCircle
              faCloud faCog faComment faCookie faCopy faCrown faEnvelopeOpenText faTimes faTimesCircle faEllipsisH faEllipsisV faEdit
              faFileDownload faFlask faGem faGhost faGraduationCap faHandshake faProjectDiagram faInfoCircle faQuestionCircle faLanguage faLaptop
              faLocationArrow faLock faLockOpen faMagic faPalette faMapPin faChevronLeft faChevronRight faCircle
              faPaperPlane faPenSquare faPlayCircle faPlus faQrcode faQuestion faRocket faSearch faShareAlt faShieldAlt faSlidersH faStar faSun faTag faTerminal
              faTimes faTimesCircle faTrashAlt faUniversity faUsers faUserLock faUserNinja faUserPlus faAngleDown faAngleRight faMinus faStepBackward]]
            ["@fortawesome/react-fontawesome" :refer [FontAwesomeIcon]]))

(def ^:private icons
  {:archive faArchive
   :arrow-down faArrowDown
   :arrow-left faArrowLeft
   :arrow-right faArrowRight
   :arrow-up faArrowUp
   :backspace faBackspace
   :bell faBell
   :briefcase faBriefcase
   :building faBuilding
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
   :cog faCog
   :collapse-down faAngleDown
   :collapse-up faAngleRight
   :comment faComment
   :comment/alt faCommentAlt
   :cookie/complete faCookie
   :copy faCopy
   :cross faTimes
   :crown faCrown
   :delete-icon faTimesCircle
   :dots faEllipsisH
   :dots-v faEllipsisV
   :edit faEdit
   :eye faEye
   :envelope faEnvelope
   :envelope-open-text faEnvelopeOpenText
   :facebook faFacebook
   :file-download faFileDownload
   :flag faFontAwesomeFlag
   :flask faFlask
   :gem faGem
   :ghost faGhost
   :graduation-cap faGraduationCap
   :graph faProjectDiagram
   :github faGithub
   :handshake faHandshake
   :hourglass/empty faHourglass
   :id-card faIdCard
   :info faInfoCircle
   :info-question faQuestionCircle
   :instagram faInstagram
   :language faLanguage
   :laptop faLaptop
   :linkedin faLinkedin
   :location-arrow faLocationArrow
   :lock faLock
   :lock/open faLockOpen
   :magic faMagic
   :minus faMinus
   :palette faPalette
   :pen faPenSquare
   :pin faMapPin
   :plane faPaperPlane
   :play/circle faPlayCircle
   :plus faPlus
   :question faQuestion
   :qrcode faQrcode
   :reset faStepBackward
   :rocket faRocket
   :search faSearch
   :sliders-h faSlidersH
   :share faShareAlt
   :shield faShieldAlt
   :star faStar
   :sun faSun
   :tag faTag
   :terminal faTerminal
   :times faTimes
   :times-circle faTimesCircle
   :trash faTrashAlt
   :twitter faTwitter
   :university faUniversity
   :user/group faUsers
   :user/lock faUserLock
   :user/ninja faUserNinja
   :user/plus faUserPlus})

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

(defn icon-card
  "Wrap an icon into a panel to emphasize it. Takes same parameters as `icon`."
  ([identifier]
   [icon-card identifier ""])
  ([identifier classes]
   [icon-card identifier classes {}])
  ([identifier classes extras]
   [:span.icon-card
    [icon identifier classes extras]]))
