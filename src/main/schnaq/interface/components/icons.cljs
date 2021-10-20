(ns schnaq.interface.components.icons
  ;; For further information check: https://fontawesome.com/v5.15/how-to-use/on-the-web/using-with/react
  ;; For two styles of the same icon see here: https://fontawesome.com/v5.15/how-to-use/on-the-web/using-with/react#faqs
  (:require ["@fortawesome/free-brands-svg-icons" :refer [faFacebook faInstagram faLinkedin faTwitter faGithub]]
            ["@fortawesome/free-regular-svg-icons" :refer [faCalendar faCommentAlt faEye faEnvelope faHourglass]]
            ["@fortawesome/free-solid-svg-icons" :refer
             [faArrowLeft faArrowRight faArrowDown faArrowUp faBell faCalendarAlt faCamera faChalkboardTeacher faCheck faCircleNotch
              faCog faComment faComments faCookie faCopy faEnvelopeOpenText faTimes faTimesCircle faEllipsisH faEllipsisV faEdit
              faFileDownload faFlask faGem faGhost faHandshake faProjectDiagram faInfoCircle faQuestionCircle faLanguage faLocationArrow faPaperPlane
              faPlus faQrcode faQuestion faRocket faSearch faShareAlt faShieldAlt faSlidersH faStar faTag faTerminal faTimes faTimesCircle faTrashAlt
              faUsers faUserLock faUserNinja faUserPlus]]
            ["@fortawesome/react-fontawesome" :refer [FontAwesomeIcon]]))

(def ^:private icons
  {:arrow-down faArrowDown
   :arrow-left faArrowLeft
   :arrow-right faArrowRight
   :arrow-up faArrowUp
   :bell faBell
   :calendar faCalendar
   :calendar-alt faCalendarAlt
   :camera faCamera
   :chalkboard-teacher faChalkboardTeacher
   :check/normal faCheck
   :circle-notch faCircleNotch
   :cog faCog
   :comment faComment
   :comment/alt faCommentAlt
   :comments faComments
   :cookie/complete faCookie
   :copy faCopy
   :cross faTimes
   :delete-icon faTimesCircle
   :dots faEllipsisH
   :dots-v faEllipsisV
   :edit faEdit
   :eye faEye
   :envelope faEnvelope
   :envelope-open-text faEnvelopeOpenText
   :facebook faFacebook
   :file-download faFileDownload
   :flask faFlask
   :gem faGem
   :ghost faGhost
   :graph faProjectDiagram
   :github faGithub
   :handshake faHandshake
   :hourglass/empty faHourglass
   :info faInfoCircle
   :info-question faQuestionCircle
   :instagram faInstagram
   :language faLanguage
   :linkedin faLinkedin
   :location-arrow faLocationArrow
   :plane faPaperPlane
   :plus faPlus
   :question faQuestion
   :qrcode faQrcode
   :rocket faRocket
   :search faSearch
   :sliders-h faSlidersH
   :share faShareAlt
   :shield faShieldAlt
   :star faStar
   :tag faTag
   :terminal faTerminal
   :times faTimes
   :times-circle faTimesCircle
   :trash faTrashAlt
   :twitter faTwitter
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
