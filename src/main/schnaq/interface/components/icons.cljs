(ns schnaq.interface.components.icons)

(defn fa
  "Returns a fontawesome icon component for a given identifier."
  ([identifier] [fa identifier nil])
  ([identifier classes]
   (identifier
     {:arrow-down [:i.fas.fa-arrow-down {:class classes}]
      :arrow-left [:i.fa.fa-arrow-left {:class classes}]
      :arrow-right [:i.fas.fa-arrow-right {:class classes}]
      :arrow-up [:i.fas.fa-arrow-up {:class classes}]
      :bell [:i.fas.fa-bell {:class classes}]
      :calendar [:i.far.fa-calendar {:class classes}]
      :camera [:i.fas.fa-camera {:class classes}]
      :check/normal [:i.fas.fa-check {:class classes}]
      :check/square "fa-check-square"
      :circle "fas fa-circle"
      :circle-notch "fa-circle-notch"
      :clipboard "fa-clipboard-list"
      :clock "fa-clock"
      :cog "fa-cog"
      :cogs "fa-cogs"
      :comment-alt "fa-comment-alt"
      :comments "fa-comments"
      :cookie/bite "fa-cookie-bite"
      :cookie/complete "fa-cookie"
      :copy "fa-copy"
      :cross "fa-times"
      :delete-icon "fa-times-circle"
      :dots "fa-ellipsis-h"
      :edit "fa-edit"
      :eraser "fa-eraser"
      :file-download "fa-file-download"
      :flag "fa-flag"
      :flask "fa-flask"
      :graph "fa-project-diagram"
      :heart "fa-heart"
      :home "fa-home"
      :info "fa-info-circle"
      :info-question "fa-question-circle"
      :language "fa-language"
      :laptop "fa-laptop-code"
      :lightbulb "fa-lightbulb"
      :lock-closed "fa-lock"
      :lock-open "fa-lock-open"
      :minus "fa-minus"
      :newspaper "fa-newspaper"
      :paragraph "fa-paragraph"
      :plane "fa-paper-plane"
      :plus "fas fa-plus"
      :project/diagram "fa-project-diagram"
      :puzzle "fa-puzzle-piece"
      :reply "fa-comment-alt"
      :rocket "fas fa-rocket"
      :search "fa-search"
      :server "fa-server"
      :share "fa-share-alt"
      :shield "fa-shield-alt"
      :site-map "fa-sitemap"
      :spinner "fa-spinner"
      :star "fas fa-star"
      :tag "fas fa-tag"
      :terminal "fa-terminal"
      :trash "fa-trash-alt"
      :user/edit "fa-user-edit"
      :user/group "fa-users"
      :user/group-edit "fa-users-cog"
      :user/lock "fa-user-lock"
      :user/ninja "fa-user-ninja"
      :user/plus "fa-user-plus"
      :user/shield "fa-user-shield"})))

(defn icon-lg
  "A bigger version of the icon"
  ([identifier]
   [icon-lg identifier nil])
  ([identifier classes]
   [fa identifier (if classes (str "fa-lg " classes) "fa-lg")]))

;; TODO check out labels
