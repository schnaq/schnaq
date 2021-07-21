(ns schnaq.interface.views.navbar.for-pages
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [labels img-path fa]]
            [schnaq.interface.utils.language :as language]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.navbar.user-management :as um]))

;; -----------------------------------------------------------------------------
;; Navbar Elements

(defn- create-dropdown-item [href label-key]
  [:a.dropdown-item {:href href}
   (labels label-key)])

(defn- create-schnaq-link []
  (create-dropdown-item (reitfe/href :routes.schnaq/create)
                        :nav.schnaqs/create-schnaq))

(defn- last-added-schnaq-link [share-hash edit-hash]
  (when-not (nil? edit-hash)
    [:div.dropdown-item.clickable
     {:on-click #(rf/dispatch [:navigation/navigate
                               :routes.schnaq/admin-center
                               {:share-hash share-hash :edit-hash edit-hash}])}
     (labels :nav.schnaqs/last-added)]))

(defn- my-schnaqs-link [visited-hashes]
  (when-not (empty? visited-hashes)
    (create-dropdown-item (reitfe/href :routes.schnaqs/personal)
                          :router/visited-schnaqs)))

(defn- all-public-schnaqs-link []
  (create-dropdown-item (reitfe/href :routes.schnaqs/public)
                        :nav.schnaqs/show-all-public))

(defn- blog-link []
  [:ul.navbar-nav
   [:li.nav-item
    [:a.nav-link {:href "https://schnaq.com/blog/" :role "button"}
     (labels :nav/blog)]]])


;; -----------------------------------------------------------------------------

(defn navbar
  "Navbar definition for the default pages."
  []
  (let [{:discussion/keys [share-hash edit-hash]} @(rf/subscribe [:schnaq/last-added])
        visited-hashes @(rf/subscribe [:schnaqs.visited/all-hashes])
        current-language @(rf/subscribe [:current-language])]
    ;; collapsable navbar
    [:nav.navbar.navbar-expand-lg.py-3.navbar-light.bg-light
     ;; logo
     [:div.container
      [:a.navbar-brand {:href (reitfe/href :routes/startpage)}
       [:img.d-inline-block.align-middle.mr-2
        {:src (img-path :logo) :width "150" :alt "schnaq logo"}]]
      ;; hamburger
      [:button.navbar-toggler
       {:type "button" :data-toggle "collapse" :data-target "#schnaq-navbar"
        :aria-controls "schnaq-navbar" :aria-expanded "false" :aria-label "Toggle navigation"
        :data-html2canvas-ignore true}
       [:span.navbar-toggler-icon]]
      ;; menu items
      [:div#schnaq-navbar.collapse.navbar-collapse
       [:ul.navbar-nav.mr-auto
        ;; navigation items
        [toolbelt/desktop-mobile-switch
         ;; desktop view
         [:li.nav-item
          [:a.nav-link
           {:role "button"
            :href (reitfe/href
                    (if (zero? (count visited-hashes))
                      :routes.schnaqs/public
                      :routes.schnaqs/personal))}
           (labels :nav/schnaqs)]]
         ;; mobile view
         [:li.nav-item.dropdown
          [:a#schnaq-dropdown.nav-link.dropdown-toggle
           {:href "#" :role "button" :data-toggle "dropdown"
            :aria-haspopup "true" :aria-expanded "false"}
           (labels :nav/schnaqs)]
          [:div.dropdown-menu {:aria-labelledby "schnaq-dropdown"}
           [create-schnaq-link]
           [:div.dropdown-divider]
           [last-added-schnaq-link share-hash edit-hash]
           [my-schnaqs-link visited-hashes]
           [all-public-schnaqs-link]]]]
        [:li.nav-item
         [:a.nav-link {:role "button" :href (reitfe/href :routes/privacy)}
          (labels :router/privacy)]]]
       [:ul.navbar-nav.dropdown.ml-auto
        [:a#schnaq-dropdown.nav-link.dropdown-toggle
         {:href "#" :role "button" :data-toggle "dropdown"
          :aria-haspopup "true" :aria-expanded "false"}
         [:i {:class (str "fas " (fa :language))}] " " current-language]
        [:div.dropdown-menu {:aria-labelledby "schnaq-dropdown"}
         [:button.dropdown-item
          {:on-click #(language/set-language :en)} "English"]
         [:button.dropdown-item
          {:on-click #(language/set-language :de)} "Deutsch"]]]
       [blog-link]
       [um/admin-dropdown "btn-outline-secondary"]
       [um/user-handling-menu "btn-outline-primary"]]]]))

(defn navbar-transparent
  "Navbar definition for the default pages."
  []
  (let [visited-hashes @(rf/subscribe [:schnaqs.visited/all-hashes])
        current-language @(rf/subscribe [:current-language])]
    ;; collapsable navbar
    [:nav.navbar.navbar-expand-lg.py-3.navbar-transparent.bg-transparent.mb-4
     ;; logo
     [:div.container
      [:a.navbar-brand {:href (reitfe/href :routes/startpage)}
       [:img.d-inline-block.align-middle.mr-2
        {:src (img-path :logo-white) :width "150" :alt "schnaq logo"}]]
      ;; hamburger
      [:button.navbar-toggler
       {:type "button" :data-toggle "collapse" :data-target "#schnaq-navbar"
        :aria-controls "schnaq-navbar" :aria-expanded "false" :aria-label "Toggle navigation"
        :data-html2canvas-ignore true}
       [:span.navbar-toggler-icon]]
      ;; menu items
      [:div#schnaq-navbar.collapse.navbar-collapse
       [:ul.navbar-nav.ml-auto
        [:li.nav-item
         [:a.nav-link
          {:role "button"
           :href (reitfe/href
                   (if (zero? (count visited-hashes))
                     :routes.schnaqs/public
                     :routes.schnaqs/personal))}
          (labels :nav/schnaqs)]]
        [blog-link]
        [:li.nav-item
         [:a.nav-link {:role "button" :href (reitfe/href :routes/privacy)}
          (labels :router/privacy)]]
        [:li.nav-item.dropdown
         [:a#schnaq-dropdown.nav-link.dropdown-toggle
          {:href "#" :role "button" :data-toggle "dropdown"
           :aria-haspopup "true" :aria-expanded "false"} current-language]
         [:div.dropdown-menu {:aria-labelledby "schnaq-dropdown"}
          [:button.dropdown-item
           {:on-click #(language/set-language :en)} "English"]
          [:button.dropdown-item
           {:on-click #(language/set-language :de)} "Deutsch"]]]]
       [um/admin-dropdown "btn-outline-secondary"]
       [um/user-handling-menu "btn-outline-light"]]]]))