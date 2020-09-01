(ns schnaq.interface.views.base
  (:require [schnaq.interface.text.display-data :as data :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.utils.localstorage :as ls]))

(defn- wavy-bottom []
  ;; bezier curves
  [:svg.wavy-bottom
   {:xmlSpace "preserve"
    :viewBox "0 0 1440 87"
    :y "0px"
    :x "0px"}
   [:path {:d "M0,87h48c48,0,144,0,240-11.6c96-11.8,192-34.6,288-43.5c96-8.5,192-3.1,288,8.7c96,11.6,192,29,288,29 s192-17.4,240-26.1l48-8.7V0h-48c-48,0-144,0-240,0S960,0,864,0S672,0,576,0S384,0,288,0S96,0,48,0H0V87z"}]])

(defn header
  "Build a header with a curly bottom for a page. Heading, subheading and more will be included in the header."
  [heading subheading & more]
  [:div.pb-2
   [:header.masthead.text-white
    [:div.container
     [:h1 heading]
     [:h4 subheading]
     more]]
   [wavy-bottom]])

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
    [:div [:span [:h4 heading]]]
    [:div [:p subheading]]]])


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
  [username]
  [:button.btn.btn-outline-primary {:on-click #(rf/dispatch [:user/show-display-name-input])} username])

(defn username-bar-view
  "A bar containing all user related utilities and information."
  []
  (let [username @(rf/subscribe [:user/display-name])
        show-input? @(rf/subscribe [:user/show-display-name-input?])]
    (if show-input?
      [name-input username]
      [show-input-button username])))

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
   [:div.meeting-header.header-custom.shadow-custom
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
  (let [{:meeting/keys [share-hash edit-hash]} @(rf/subscribe [:meeting/last-added])]
    ;; collapsable navbar
    [:nav.navbar.navbar-expand-lg.py-3.navbar-light.bg-light
     ;; logo
     [:div.container
      [:a.navbar-brand {:href (reitfe/href :routes/startpage)}
       [:img.d-inline-block.align-middle.mr-2 {:src (data/img-path :logo) :width "150" :alt ""}]]
      ;; hamburger
      [:button.navbar-toggler
       {:type "button" :data-toggle "collapse" :data-target "#schnaq-navbar"
        :aria-controls "schnaq-navbar" :aria-expanded "false" :aria-label "Toggle navigation"}
       [:span.navbar-toggler-icon]]
      ;; menu items
      [:div {:id "schnaq-navbar"
             :class "collapse navbar-collapse"}
       [:ul.navbar-nav.mr-auto
        ;; navigation items
        (when-not toolbelt/production?
          [:li.nav-item [:a.nav-link {:href (reitfe/href :routes/meetings)} (labels :nav-meeting)]])
        [:li.nav-item [:a.nav-link {:href (reitfe/href :routes.meeting/create)} (labels :nav-meeting-create)]]
        (when (ls/get-item :meeting.last-added/edit-hash)
          [:li.nav-item
           [:div.nav-link.clickable
            {:on-click #(rf/dispatch [:navigation/navigate
                                      :routes.meeting/created
                                      {:share-hash share-hash :admin-hash edit-hash}])}
            (labels :nav-meeting-last-added)]])]

       ;; name input
       [username-bar-view]]]]))

;; footer

(defn footer
  "footer to display at the bottom the page"
  []
  [:footer.footer.bg-light
   [:div.container
    [:div.row
     [:div {:class "col-lg-6 h-100 text-center text-lg-left my-auto"}
      [:ul {:class "list-inline mb-2"}
       [:li.list-inline-item.btn.btn-link
        [:a {:href "https://dialogo.io/impressum"} "Impressum"]]
       [:li.list-inline-item.btn.btn-link
        [:a {:href "https://dialogo.io/datenschutz"} "Datenschutz"]]]
      [:p {:class "text-muted small mb-4 mb-lg-0"} "\u00A9 Dialogo 2020"]]
     ;; twitter icon
     [:div {:class "col-lg-6 h-100 text-center text-lg-right my-auto"}
      [:ul {:class "list-inline mb-0"}
       [:li {:class "list-inline-item mr-3"}
        [:a {:href "https://twitter.com/dialogoIO"}
         [:i {:class "fab fa-twitter-square fa-2x fa-fw"}]]]]]]]])
