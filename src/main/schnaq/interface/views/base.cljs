(ns schnaq.interface.views.base
  (:require [schnaq.interface.text.display-data :as data :refer [labels img-path]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]))

(defn wavy-curve
  "Define a wavy curve."
  ([]
   (wavy-curve ""))
  ([transformation]
   ;; bezier curves
   [:svg.wavy-curve
    {:xmlSpace "preserve"
     :overflow :auto
     :viewBox "0 0 1440 87"
     :style {:transform transformation
             :-webkit-transform transformation}
     :y "0px"
     :x "0px"}
    [:path {:d "M0,87h48c48,0,144,0,240-11.6c96-11.8,192-34.6,288-43.5c96-8.5,192-3.1,288,8.7c96,11.6,192,29,288,29 s192-17.4,240-26.1l48-8.7V0h-48c-48,0-144,0-240,0S960,0,864,0S672,0,576,0S384,0,288,0S96,0,48,0H0V87z"}]]))

(defn header
  "Build a header with a curly bottom for a page. Heading, subheading and more will be included in the header."
  [heading subheading & more]
  [:div.pb-2
   [:header.masthead.text-white
    [:div.container
     [:h1 heading]
     [:h4 subheading]
     more]]
   [wavy-curve]])

;; grid icons

(defn icon-in-grid
  "Create one icon in a grid"
  [icon heading subheading]
  [:div.col-lg-4
   [:div {:class "features-icons-item mx-auto mb-5 mb-lg-0 mb-lg-3"}
    [:div.features-icons-icon.d-flex
     [:i {:class (str "m-auto text-primary fas fa-4x " icon)}]]
    [:h3 heading]
    [:p.lead.mb-0 subheading]]])

(defn img-bullet-subtext
  "Create one icon in a grid"
  [path-to-img heading subheading]
  [:div.d-flex.flex-row.p-1
   [:div [:img {:src path-to-img}]]
   [:div
    [:p.h4 heading]
    [:p subheading]]])


(defn icon-bullet [path-to-img text]
  [:div.d-flex.flex-row.p-1
   [:div [:img {:src path-to-img}]]
   [:div [:span text]]])

;; name entry

(defn- name-input
  "An input, where the user can set their name. Happens automatically by typing."
  [username]
  [:form.form-inline
   {:on-submit (fn [e]
                 (js-wrap/prevent-default e)
                 (rf/dispatch [:user/set-display-name (oget e [:target :elements :name-input :value])]))}
   [:input#name-input.form-control.form-round-05.py-1.mr-sm-2
    {:type "text"
     :name "name-input"
     :autoFocus true
     :required true
     :defaultValue username
     :placeholder (labels :user.button/set-name-placeholder)}]
   [:input.btn.btn-outline-primary.mt-1.mt-sm-0
    {:type "submit"
     :value (labels :user.button/set-name)}]])


(defn show-input-button
  "A button triggering the showing of the name field."
  [username button-class]
  [:button.btn {:class button-class
                :on-click #(rf/dispatch [:user/show-display-name-input])} username])

(defn username-bar-view
  "A bar containing all user related utilities and information."
  ([]
   (username-bar-view "btn-outline-primary"))

  ([button-class]
   (let [username @(rf/subscribe [:user/display-name])
         show-input? @(rf/subscribe [:user/show-display-name-input?])]
     (if show-input?
       [name-input username]
       [show-input-button username button-class]))))

(defn username-bar-view-light
  []
  (username-bar-view "btn-outline-light"))


;; discussion loop header

(defn discussion-header
  "Non wavy header with an optional back button.
  'title-on-click-function' is triggered when header is clicked
  'on-click-back-function' is triggered when back button is clicked,when no on-click-back-function is provided the back button will not be displayed"
  ([title subtitle]
   [discussion-header title subtitle nil nil])

  ([title subtitle title-on-click-function]
   [discussion-header title subtitle title-on-click-function nil])

  ([title subtitle title-on-click-function on-click-back-function]
   ;; check if title is clickable and set properties accordingly
   [:div.meeting-header.header-custom.shadow-straight
    [:div.row
     [:div.col-1.back-arrow
      (when on-click-back-function
        [:p {:on-click on-click-back-function}              ;; the icon itself is not clickable
         [:i.arrow-icon {:class (str "m-auto fas " (data/fa :arrow-left))}]])]
     [:div.col-8.container
      [:div
       (when title-on-click-function
         {:on-click title-on-click-function
          :class "clickable-no-hover"})
       [:h2 title]
       [:h6 subtitle]]]]]))

;; nav header

(defn nav-header []
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
      [:div {:id "schnaq-navbar"
             :class "collapse navbar-collapse"}
       [:ul.navbar-nav.mr-auto
        ;; navigation items
        (when-not toolbelt/production?
          [:li.nav-item [:a.nav-link {:href (reitfe/href :routes/meetings)} (labels :nav-meeting)]])
        [:li.nav-item [:a.nav-link {:href (reitfe/href :routes.meeting/create)} (labels :nav-meeting-create)]]
        (when-not (empty? visited-hashes)
          [:li.nav-item [:a.nav-link {:href (reitfe/href :routes.meetings/my-schnaqs)} (labels :router/my-schnaqs)]])
        (when-not (nil? edit-hash)
          [:li.nav-item
           [:div.nav-link.clickable
            {:on-click #(rf/dispatch [:navigation/navigate
                                      :routes.meeting/admin-center
                                      {:share-hash share-hash :edit-hash edit-hash}])}
            (labels :router/meeting-created)]])]
       ;; name input
       [username-bar-view]]]]))

(defn meeting-header
  "Overview header for a meeting with its title as headline"
  [meeting]
  [:nav.navbar.navbar-expand-lg.py-3.navbar-dark.context-header

   [:div.container
    ;; hamburger
    [:button.navbar-toggler
     {:type "button" :data-toggle "collapse" :data-target "#schnaq-navbar"
      :aria-controls "schnaq-navbar" :aria-expanded "false" :aria-label "Toggle navigation"
      :data-html2canvas-ignore true}
     [:span.navbar-toggler-icon]]
    ;; menu items
    [:div.row {:id "schnaq-navbar"
               :class "collapse navbar-collapse"}
     ;; schnaq logo
     [:div.col.col-2
      [:a.navbar-brand.mx-2 {:href (reitfe/href :routes/startpage)}
       [:img.d-inline-block.align-middle.mr-2
        {:src (img-path :logo-white) :width "150" :alt "schnaq logo"}]]]
     ;; clickable title
     [:div.col-8.px-5.text-center
      [:div {:on-click
             (fn []
               (rf/dispatch [:navigation/navigate :routes.meeting/show
                             {:share-hash (:meeting/share-hash meeting)}])
               (rf/dispatch [:meeting/select-current meeting]))
             :class "clickable-no-hover"}
       [:h2.mx-5 (:meeting/title meeting)]]]
     ;; name input
     [:div.col-2.text-right
      [username-bar-view-light]]]]])

;; footer

(defn footer
  "Footer to display at the bottom the page."
  []
  [:footer
   [:div.container
    [:div.row
     [:div.col-md-4.col-12
      [:img.footer-schnaqqifant
       {:src (img-path :logo-white)}]
      [:div.lead.text-white.font-italic.pb-1
       (labels :startpage/heading)]
      [:small.text-white "\u00A9 DisqTec 2020"]]
     [:div.col-md-8.col-12.text-center.text-md-right.pt-3.pt-md-0
      [:ul.list-inline
       [:li.list-inline-item.btn.btn-outline-white
        [:a {:href "https://disqtec.com/ueber-uns"}
         (labels :footer.buttons/about-us)]]
       [:li.list-inline-item.btn.btn-outline-white
        [:a {:href "https://disqtec.com/impressum"}
         (labels :footer.buttons/legal-note)]]
       [:li.list-inline-item.btn.btn-outline-white
        [:a {:href "https://disqtec.com/datenschutz"}
         (labels :footer.buttons/privacy)]]]]]]])
