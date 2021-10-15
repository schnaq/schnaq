(ns schnaq.interface.components.icons
  ;; For further information check: https://fontawesome.com/v5.15/how-to-use/on-the-web/using-with/react
  ;; For two styles of the same icon see here: https://fontawesome.com/v5.15/how-to-use/on-the-web/using-with/react#faqs
  (:require ["@fortawesome/free-regular-svg-icons" :refer [faCalendar faCommentAlt faEye]]
            ["@fortawesome/free-solid-svg-icons" :refer
             [faArrowLeft faArrowRight faArrowDown faArrowUp faBell faCalendarAlt faCamera faCheck faCircleNotch
              faCog faComment faComments faCookie faCopy faTimes faTimesCircle faEllipsisH faEllipsisV faEdit
              faFileDownload faFlask faGhost faProjectDiagram faInfoCircle faQuestionCircle faLanguage faPaperPlane
              faPlus faQuestion faRocket faSearch faShareAlt faShieldAlt faStar faTag faTerminal faTimes faTrashAlt
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
   :file-download faFileDownload
   :flask faFlask
   :ghost faGhost
   :graph faProjectDiagram
   :info faInfoCircle
   :info-question faQuestionCircle
   :language faLanguage
   :plane faPaperPlane
   :plus faPlus
   :question faQuestion
   :rocket faRocket
   :search faSearch
   :share faShareAlt
   :shield faShieldAlt
   :star faStar
   :tag faTag
   :terminal faTerminal
   :times faTimes
   :trash faTrashAlt
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
