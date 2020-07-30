(ns meetly.meeting.interface.views.base
  (:require [oops.core :refer [oget]]
            [reitit.frontend.easy :as reitfe]
            [meetly.meeting.interface.text.display-data :as data]
            [re-frame.core :as rf]))


(defn- wavy-bottom []
  ;; bezier curves
  [:svg.wavy-bottom
   {:xmlSpace "preserve"
    :viewBox "0 0 1440 87"
    :y "0px"
    :x "0px"}
   [:path {
           :d "M0,87h48c48,0,144,0,240-11.6c96-11.8,192-34.6,288-43.5c96-8.5,192-3.1,288,8.7c96,11.6,192,29,288,29 s192-17.4,240-26.1l48-8.7V0h-48c-48,0-144,0-240,0S960,0,864,0S672,0,576,0S384,0,288,0S96,0,48,0H0V87z"
           }]])


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
     [:i {:class (str "m-auto text-primary fas " icon)}]]
    [:h3 heading]
    [:p.lead.mb-0 subheading]]])


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
                 (.preventDefault e)
                 (rf/dispatch [:set-username (oget e [:target :elements :name-input :value])])
                 (rf/dispatch [:hide-name-input]))}
   [:div.px-2 [:input#name-input.form-control.form-round-05.px-2.py-1
               {:type "text"
                :name "name-input"
                :autoFocus true
                :placeholder username}]]
   [:input.btn.btn-primary {:type "submit"
                            :value "Set Name"}]])


(defn show-input-button
  "A button triggering the showing of the name field."
  [username]
  [:button.btn.btn-primary {:on-click #(rf/dispatch [:show-name-input])} username])

(defn username-bar-view
  "A bar containing all user related utilities and information."
  []
  (let [username @(rf/subscribe [:username])
        show-input? @(rf/subscribe [:show-username-input?])]
    [:div.float-right
     (if show-input?
       [name-input username]
       [show-input-button username])]))

;; nav header

(defn nav-header []
  ;; collapsable navbar
  [:nav.navbar.navbar-expand-lg.py-3.navbar-light.bg-light
   ;; logo
   [:div.container
    [:a.navbar-brand {:href "#/startpage"}
     [:img.d-inline-block.align-middle.mr-2 {:src (data/img-path :logo) :width "150" :alt ""}]]
    ;; hamburger
    [:button.navbar-toggler
     {:type "button" :data-toggle "collapse" :data-target "#navbarSupportedContent"
      :aria-controls "navbarSupportedContent" :aria-expanded "false" :aria-label "Toggle navigation"}
     [:span.navbar-toggler-icon]]
    ;; menu items
    [:div
     {:id "navbarSupportedContent"
      :class "collapse navbar-collapse"}
     [:ul.navbar-nav.ml-auto
      ;; navigation items
      [:li.nav-item [:a.nav-link {:href (reitfe/href :routes/meetings)} (data/labels :nav-meeting)]]
      [:li.nav-item [:a.nav-link {:href (reitfe/href :routes/meetings.create)} (data/labels :nav-meeting-create)]]
      [:li.nav-item [:a.nav-link {:href (reitfe/href :routes/home)} (data/labels :nav-overview)]]]
     ;; name input
     [:div.px-2
      [username-bar-view]]]]])

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
