(ns schnaq.interface.views.navbar.for-discussions
  (:require [schnaq.interface.components.navbar :as navbar-components]
            [schnaq.interface.views.discussion.share :as share]
            [schnaq.interface.views.navbar.collapse-content :as collapse-content]
            [schnaq.interface.views.navbar.elements :as nav-elements]
            [schnaq.interface.views.navbar.user-management :as user-management]))

(defn- interaction-elements []
  [:div.d-flex.schnaq-navbar.align-items-center.px-3
   [share/share-schnaq-button]
   [nav-elements/navbar-download]
   [nav-elements/navbar-settings]
   [nav-elements/language-toggle]
   [nav-elements/dropdown-views]
   [nav-elements/navbar-upgrade-button true]
   [nav-elements/user-button true]])

(defn header
  "Header for schnaq view overview"
  []
  (let [navbar-content-id "Navbar-Content"]
    [navbar-components/collapsible-nav-bar
     [nav-elements/discussion-title]
     navbar-content-id
     "navbar-bg-transparent-sm-white"
     [interaction-elements]
     [collapse-content/navbar-external-content navbar-content-id]]))

(defn- qanda-interaction-elements []
  [:div.d-flex.align-items-center
   [nav-elements/statement-counter]
   [nav-elements/dropdown-views :icon-views-light "text-white"]
   [nav-elements/navbar-upgrade-button]
   [nav-elements/user-button]])

(defn qanda-header
  "Header for schnaq Q&A View"
  []
  (let [navbar-content-id "Qanda-Content"]
    [navbar-components/collapsible-nav-bar
     [nav-elements/navbar-qanda-title]
     navbar-content-id
     "bg-transparent"
     [qanda-interaction-elements]
     [collapse-content/navbar-external-content navbar-content-id]]))
