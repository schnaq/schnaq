(ns schnaq.interface.views.base
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :as data :refer [labels img-path fa]]
            [schnaq.interface.views.brainstorm.tools :as btools]
            [schnaq.interface.views.meeting.admin-buttons :as admin-buttons]
            [schnaq.interface.views.navbar :as navbar]))

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

(defn meeting-header
  "Overview header for a meeting with its title as headline"
  [{:meeting/keys [title share-hash] :as meeting}]
  (let [admin-access-map @(rf/subscribe [:meetings/load-admin-access])
        edit-hash (get admin-access-map share-hash)]
    [:nav.navbar.navbar-expand-lg.py-3.navbar-dark.context-header
     [:div.container
      ;; schnaq logo
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
      [:div {:id "schnaq-navbar" :class "collapse navbar-collapse"}
       ;; clickable title
       [:div.mr-auto.clickable-no-hover
        {:on-click
         (fn []
           (rf/dispatch [:navigation/navigate :routes.meeting/show
                         {:share-hash share-hash}])
           (rf/dispatch [:meeting/select-current meeting]))}
        [:h3.mx-5 title]]
       (when (and edit-hash (btools/is-brainstorm? meeting))
         [admin-buttons/admin-center share-hash edit-hash])
       ;; name input
       [navbar/username-bar-view-light]]]]))

;; footer

(defn footer
  "Footer to display at the bottom the page."
  []
  [:footer
   [:div.container
    [:div.row
     [:div.col-md-6.col-12
      [:img.footer-schnaqqifant
       {:src (img-path :logo-white)}]
      [:div.lead.font-italic.pb-1
       (labels :startpage/heading)]]
     [:div.col-md-6.col-12.text-md-right.pt-3.pt-md-0
      [:ul.list-inline
       [:li.list-inline-item.btn.btn-outline-white
        [:a {:href "https://disqtec.com/ueber-uns"}
         (labels :footer.buttons/about-us)]]
       [:li.list-inline-item.btn.btn-outline-white
        [:a {:href "https://disqtec.com/impressum"}
         (labels :footer.buttons/legal-note)]]
       [:li.list-inline-item.btn.btn-outline-white
        [:a {:href "https://disqtec.com/datenschutz"}
         (labels :footer.buttons/privacy)]]]]]
    [:p.pt-3
     [:i {:class (str "fas " (fa :terminal))}]
     " Entwickelt mit "
     [:i {:class (str "m-auto fas " (fa :flask))}]
     " in Düsseldorf, Germany. "
     "© " [:a {:href "https://disqtec.com"
               :target :_blank} "DisqTec"]
     " "
     (.getFullYear (js/Date.))]]])
