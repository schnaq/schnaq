(ns schnaq.interface.components.icons)

(defn icon
  "Returns a fontawesome icon component for a given identifier."
  ([identifier] [icon identifier nil])
  ([identifier classes]
   (identifier
     {:arrow-down [:i.fas.fa-arrow-down {:class classes}]
      :arrow-left [:i.fa.fa-arrow-left {:class classes}]
      :arrow-right [:i.fas.fa-arrow-right {:class classes}]
      :arrow-up [:i.fas.fa-arrow-up {:class classes}]
      :bell [:i.fas.fa-bell {:class classes}]
      :calendar [:i.far.fa-calendar {:class classes}]
      :calendar-alt [:i.fas.fa-calendar-alt {:class classes}]
      :camera [:i.fas.fa-camera {:class classes}]
      :check/normal [:i.fas.fa-check {:class classes}]
      :circle-notch [:i.fas.fa-circle-notch {:class classes}]
      :cog [:i.fas.fa-cog {:class classes}]
      :comment [:i.fas.fa-comment {:class classes}]
      :comments [:i.fas.fa-comments {:class classes}]
      :cookie/complete [:i.fas.fa-cookie {:class classes}]
      :copy [:i.far.fa-copy {:class classes}]
      :cross [:i.fas.fa-times {:class classes}]
      :delete-icon [:i.fas.fa-times-circle {:class classes}]
      :dots [:i.fas.fa-ellipsis-h {:class classes}]
      :edit [:i.fas.fa-edit {:class classes}]
      :eye [:i.far.fa-eye {:class classes}]
      :file-download [:i.fas.fa-file-download {:class classes}]
      :flask [:i.fas.fa-flask {:class classes}]
      :ghost [:i.fas.fa-ghost {:class classes}]
      :graph [:i.fas.fa-project-diagram {:class classes}]
      :info [:i.fas.fa-info-circle {:class classes}]
      :info-question [:i.fas.fa-question-circle {:class classes}]
      :language [:i.fas.fa-language {:class classes}]
      :plane [:i.fas.fa-paper-plane {:class classes}]
      :plus [:i.fas.fa-plus {:class classes}]
      :question [:i.fas.fa-question {:class classes}]
      :rocket [:i.fas.fa-rocket {:class classes}]
      :search [:i.fas.fa-search {:class classes}]
      :share [:i.fas.fa-share-alt {:class classes}]
      :shield [:i.fas.fa-shield-alt {:class classes}]
      :star [:i.fas.fa-star {:class classes}]
      :tag [:i.fas.fa-tag {:class classes}]
      :terminal [:i.fas.fa-terminal {:class classes}]
      :trash [:i.fas.fa-trash-alt {:class classes}]
      :user/group [:i.fas.fa-users {:class classes}]
      :user/lock [:i.fas.fa-user-lock {:class classes}]
      :user/ninja [:i.fas.fa-user-ninja {:class classes}]
      :user/plus [:i.fas.fa-user-plus {:class classes}]})))

(defn icon-lg
  "A bigger version of the icon"
  ([identifier]
   [icon-lg identifier nil])
  ([identifier classes]
   [icon identifier (if classes (str "fa-lg " classes) "fa-lg")]))
