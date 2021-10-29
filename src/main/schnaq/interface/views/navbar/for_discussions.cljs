(ns schnaq.interface.views.navbar.for-discussions
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.navbar :as navbar-components]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.navbar.collapse-content :as collapse-content]
            [schnaq.interface.views.navbar.elements :as nav-elements]
            [schnaq.interface.views.schnaq.admin :as admin]))

(defn- collapsable-nav-bar
  [brand-content collapse-content-id navbar-bg-class top-right-content]
  [:<>
   [:nav.navbar.navbar-expand-lg.navbar-light.schnaq-navbar-dynamic-padding
    {:class navbar-bg-class}
    [:navbar-brand.p-0 {:href "#"} brand-content]
    [:button.navbar-toggler.mx-2 {:type "button" :data-toggle "collapse"
                                  :data-target (str "#" collapse-content-id)
                                  :aria-controls collapse-content-id
                                  :aria-expanded "false"
                                  :aria-label "Toggle navigation"}
     [:span.navbar-toggler-icon]]
    [:div.ml-auto.d-none.d-lg-block
     top-right-content]]
   [collapse-content/navbar-external-content collapse-content-id]])

(defn- interaction-elements []
  [:div.d-flex.schnaq-navbar.align-items-center.px-3
   [nav-elements/progress-bar-hide-lg]
   [nav-elements/share-modal]
   [nav-elements/navbar-download]
   [nav-elements/navbar-settings]
   [nav-elements/language-toggle]
   [nav-elements/dropdown-views]
   [nav-elements/user-button]])

(defn header
  "Header for schnaq view overview"
  []
  (let [navbar-content-id "Navbar-Content"]
    [collapsable-nav-bar
     [nav-elements/title-and-infos]
     navbar-content-id
     "navbar-bg-transparent-sm-white"
     [interaction-elements]]))

(defn- qanda-interaction-elements []
  [:div.d-flex.align-items-center
   [nav-elements/statement-counter]
   [nav-elements/dropdown-views :icon-views-light "text-white"]
   [nav-elements/user-button "btn-outline-light btn-transparent"]])

(defn header-for-qanda-view
  "Header for schnaq Q&A View"
  []
  (let [navbar-content-id "Qanda-Content"]
    [collapsable-nav-bar
     [nav-elements/navbar-qanda-title]
     navbar-content-id
     "bg-transparent"
     [qanda-interaction-elements]]))

(defn embeddable-header []
  ;; The view breaks earlier, because the breakpoints heed the screen size, not the div size
  (let [{:discussion/keys [title share-hash] :as discussion} @(rf/subscribe [:schnaq/selected])
        admin-access-map @(rf/subscribe [:schnaqs/load-admin-access])
        edit-hash (get admin-access-map share-hash)
        meta-info (:meta-info discussion)
        statement-count (:all-statements meta-info)
        user-count (count (:authors meta-info))]
    [:div.d-flex.flex-row.flex-wrap.p-md-3
     [:div.d-flex.align-items-center.flex-row.schnaq-navbar-space.schnaq-navbar.mb-4.mr-3
      ;; schnaq logo
      [:div.mx-4.d-none.d-md-block
       [:small.text-primary (labels :discussion.navbar/posts)]
       [:h5.text-center statement-count]]
      [:div.mx-4.d-none.d-md-block
       [:small.text-primary (labels :discussion.navbar/members)]
       [:h5.text-center user-count]]
      [:a.schnaq-logo-container.d-flex.h-100.text-decoration-none
       {:href (reitfe/href :routes.schnaq/start)}
       [:small.text-white "powered by"]
       [:img.d-inline-block.align-middle.mr-2
        {:src (img-path :logo-white) :alt "schnaq logo"
         :style {:max-height "100%" :max-width "100%" :object-fit "contain"}}]]]
     [:div.d-flex.flex-row.schnaq-navbar-space.mb-4.flex-wrap.ml-xl-auto
      [:div.d-flex.align-items-center.schnaq-navbar.px-4
       [nav-elements/schnaq-progress-bar]
       [admin/txt-export share-hash title]
       (when edit-hash
         [admin/admin-center])
       [navbar-components/language-toggle-with-tooltip false {:class "btn-lg"}]]
      [:div.d-flex.align-items-center.mt-4.mt-md-0
       [:div.mx-2.embedded-nav-button [nav-elements/graph-button]]
       [:div.mr-2.embedded-nav-button [nav-elements/summary-button]]]]]))
