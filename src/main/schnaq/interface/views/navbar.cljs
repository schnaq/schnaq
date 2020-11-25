(ns schnaq.interface.views.navbar
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [labels img-path fa]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.language :as language]
            [schnaq.interface.utils.toolbelt :as toolbelt]))

(defn- name-input
  "An input, where the user can set their name. Happens automatically by typing."
  [username btn-class]
  [:form.form-inline
   {:on-submit
    (fn [e] (js-wrap/prevent-default e)
      (rf/dispatch [:user/set-display-name
                    (oget e [:target :elements :name-input :value])]))}
   [:input#name-input.form-control.form-round-05.py-1.mr-sm-2
    {:type "text"
     :name "name-input"
     :autoFocus true
     :required true
     :defaultValue username
     :placeholder (labels :user.button/set-name-placeholder)}]
   [:input.btn.mt-1.mt-sm-0
    {:class btn-class
     :type "submit"
     :value (labels :user.button/set-name)}]])

(defn show-input-button
  "A button triggering the showing of the name field."
  [username button-class]
  [:button.btn {:class button-class
                :on-click #(rf/dispatch [:user/show-display-name-input])}
   username])

(defn username-bar-view
  "A bar containing all user related utilities and information."
  ([]
   [username-bar-view "btn-outline-primary"])

  ([button-class]
   (let [username @(rf/subscribe [:user/display-name])
         show-input? @(rf/subscribe [:user/show-display-name-input?])]
     (if show-input?
       [name-input username button-class]
       [show-input-button username button-class]))))

(defn username-bar-view-light
  []
  [username-bar-view "btn-outline-light"])


;; -----------------------------------------------------------------------------
;; Navbar Elements

(defn- create-dropdown-item [href label-key]
  [:a.dropdown-item {:href href}
   (labels label-key)])

(defn- create-brainstorm-link []
  (create-dropdown-item (reitfe/href :routes.brainstorm/create)
                        :nav.schnaqs/create-brainstorm))

(defn- create-meeting-link []
  (create-dropdown-item (reitfe/href :routes.meeting/create)
                        :nav.schnaqs/create-meeting))

(defn- last-added-schnaq-link [share-hash edit-hash]
  (when-not (nil? edit-hash)
    [:div.dropdown-item.clickable
     {:on-click #(rf/dispatch [:navigation/navigate
                               :routes.meeting/admin-center
                               {:share-hash share-hash :edit-hash edit-hash}])}
     (labels :nav.schnaqs/last-added)]))

(defn- my-schnaqs-link [visited-hashes]
  (when-not (empty? visited-hashes)
    (create-dropdown-item (reitfe/href :routes.meetings/my-schnaqs)
                          :router/my-schnaqs)))

(defn- all-schnaqs-link []
  (when-not toolbelt/production?
    (create-dropdown-item (reitfe/href :routes/meetings)
                          :nav.schnaqs/show-all)))

(defn- blog-link []
  [:ul.navbar-nav
   [:li.nav-item.mx-lg-4
    [:a.nav-link {:href "https://schnaq.com/blog/" :role "button"}
     (labels :nav/blog)]]])


;; -----------------------------------------------------------------------------

(defn navbar
  "Navbar definition for the default pages."
  []
  (let [{:meeting/keys [share-hash edit-hash]} @(rf/subscribe [:meeting/last-added])
        visited-hashes @(rf/subscribe [:meetings.visited/all-hashes])]
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
        [:li.nav-item.dropdown
         [:a#schnaq-dropdown.nav-link.dropdown-toggle
          {:href "#" :role "button" :data-toggle "dropdown"
           :aria-haspopup "true" :aria-expanded "false"}
          (labels :nav/schnaqs)]
         [:div.dropdown-menu {:aria-labelledby "schnaq-dropdown"}
          [create-brainstorm-link]
          [create-meeting-link]
          [:div.dropdown-divider]
          [last-added-schnaq-link share-hash edit-hash]
          [my-schnaqs-link visited-hashes]
          [all-schnaqs-link]]]
        [:li.nav-item
         [:a.nav-link {:role "button" :href (reitfe/href :routes/privacy)}
          (labels :router/privacy)]]]
       [:ul.navbar-nav.dropdown.ml-auto
        [:a#schnaq-dropdown.nav-link.dropdown-toggle
         {:href "#" :role "button" :data-toggle "dropdown"
          :aria-haspopup "true" :aria-expanded "false"}
         [:i {:class (str "fas fa-2x " (fa :language))}]]
        [:div.dropdown-menu {:aria-labelledby "schnaq-dropdown"}
         [:a.dropdown-item
          {:on-click #(language/set-language :en)} "English"]
         [:a.dropdown-item
          {:on-click #(language/set-language :de)} "Deutsch"]]]
       [blog-link]
       [username-bar-view]]]]))