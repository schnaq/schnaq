(ns meetly.meeting.interface.views.base)



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

