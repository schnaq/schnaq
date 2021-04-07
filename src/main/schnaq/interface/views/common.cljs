(ns schnaq.interface.views.common
  (:require ["framer-motion" :refer [motion AnimatePresence]]
            ["jdenticon" :as jdenticon]
            [cljs.core.async :refer [go <! timeout]]
            [cljs.spec.alpha :as s]
            [ghostwheel.core :refer [>defn]]
            [goog.string :as gstring]
            [oops.core :refer [oget oset!]]
            [reagent.core :as reagent]
            [reitit.frontend.easy :as reitfe]))

(>defn identicon
  "Generate unique identicon."
  ([display-name]
   [string? :ret vector?]
   (identicon display-name))
  ([display-name size]
   [string? number? :ret vector?]
   [:span {:title display-name
           :dangerouslySetInnerHTML
           {:__html (jdenticon/toSvg display-name size (clj->js {:backColor "#fff"}))}}]))

(>defn avatar
  "Get a user's avatar."
  [{:user.registered/keys [profile-picture display-name] :as user} size]
  [map? number? :ret vector?]
  (let [display-name (or display-name (:user/nickname user))]
    [:div.avatar-image.m-auto.p-0
     (if profile-picture
       [:div.profile-pic-fill
        {:style {:max-height (str size "px") :max-width (str size "px")}}
        [:img.profile-pic-image {:src profile-picture}]]
       [identicon display-name size])]))

(>defn avatar-with-nickname
  "Create an image based on the nickname and also print the nickname."
  [{:user.registered/keys [display-name] :as user} size]
  [map? number? :ret vector?]
  [:div.text-center
   [avatar user size]
   [:p.small.mt-1 display-name]])

(>defn avatar-with-nickname-right
  "Create an image based on the nickname and also print the nickname."
  [{:user.registered/keys [display-name] :as user} size]
  [map? number? :ret vector?]
  [:div.row
   [:div.mr-4 [avatar user size]]
   [:h4.my-auto display-name]])

(>defn add-namespace-to-keyword
  "Prepend a namespace to a keyword. Replaces existing namespace with new
  namespace."
  [prepend-namespace to-keyword]
  [(s/or :keyword keyword? :string string?) keyword? :ret keyword?]
  (keyword (str (name prepend-namespace) "/" (name to-keyword))))

(defn tab-builder
  "Create a tabbed view. Prefix must be unique on this page."
  ([tab-prefix first-tab second-tab]
   (tab-builder tab-prefix first-tab second-tab nil nil))
  ([tab-prefix first-tab second-tab third-tab fourth-tab]
   (let [tab-prefix# (str "#" tab-prefix)]
     [:<>
      [:nav.nav-justified
       [:div.nav.nav-tabs {:role "tablist"}
        [:a.nav-item.nav-link.active {:data-toggle "tab"
                                      :href (str tab-prefix# "-home")
                                      :role "tab"
                                      :aria-controls (str tab-prefix "-home")
                                      :aria-selected "true"}
         (:link first-tab)]
        [:a.nav-item.nav-link {:data-toggle "tab"
                               :href (str tab-prefix# "-link")
                               :role "tab"
                               :aria-controls (str tab-prefix "-link")
                               :aria-selected "false"}
         (:link second-tab)]
        (when third-tab
          [:a.nav-item.nav-link {:data-toggle "tab"
                                 :href (str tab-prefix# "-link-3")
                                 :role "tab"
                                 :aria-controls (str tab-prefix "-link-3")
                                 :aria-selected "false"}
           (:link third-tab)])
        (when fourth-tab
          [:a.nav-item.nav-link {:data-toggle "tab"
                                 :href (str tab-prefix# "-link-4")
                                 :role "tab"
                                 :aria-controls (str tab-prefix "-link-4")
                                 :aria-selected "false"}
           (:link fourth-tab)])]]
      [:div.tab-content.mt-5
       [:div.tab-pane.fade.show.active
        {:id (str tab-prefix "-home")
         :role "tabpanel" :aria-labelledby (str tab-prefix "-home-tab")}
        (:view first-tab)]
       [:div.tab-pane.fade
        {:id (str tab-prefix "-link")
         :role "tabpanel" :aria-labelledby (str tab-prefix "-link-tab")}
        (:view second-tab)]
       (when third-tab
         [:div.tab-pane.fade
          {:id (str tab-prefix "-link-3")
           :role "tabpanel" :aria-labelledby (str tab-prefix "-link-tab-3")}
          (:view third-tab)])
       (when fourth-tab
         [:div.tab-pane.fade
          {:id (str tab-prefix "-link-4")
           :role "tabpanel" :aria-labelledby (str tab-prefix "-link-tab-4")}
          (:view fourth-tab)])]])))

(>defn get-share-link
  [share-hash]
  [map? :ret string?]
  (let [path (reitfe/href :routes.schnaq/start {:share-hash share-hash})
        location (oget js/window :location)]
    (gstring/format "%s//%s%s" (oget location :protocol) (oget location :host) path)))

(>defn get-admin-center-link
  "Building the current URL with validated path, and without extra-stuff, like
  internal hashtag-routing."
  [current-route]
  [map? :ret string?]
  (let [{:keys [share-hash edit-hash]} (:path-params current-route)
        path (reitfe/href :routes.schnaq/admin-center {:share-hash share-hash
                                                       :edit-hash edit-hash})
        location (oget js/window :location)]
    (gstring/format "%s//%s%s" (oget location :protocol) (oget location :host) path)))

(>defn set-website-title!
  "Set a document's website title."
  [title]
  [string? :ret nil?]
  (let [new-title (gstring/format "schnaq - %s" title)]
    (oset! js/document [:title] new-title)))


;; -----------------------------------------------------------------------------
;; Form-related

(defn form-input
  "The input form for the display name."
  [{:keys [id placeholder default-value css]}]
  [:input.form-control.form-border-bottom.mb-2
   {:id id
    :class css
    :type "text"
    :autoComplete "off"
    :defaultValue default-value
    :placeholder placeholder
    :required true}])


;; -----------------------------------------------------------------------------
;; Higher Order Components

(defn- delay-render
  "Wrap a component in this component to wait for a certain amount of
  milliseconds, until the provided component is rendered."
  [_component _delay]
  (let [ready? (reagent/atom false)]
    (reagent/create-class
      {:component-did-mount
       (fn [comp]
         (let [[_ _component delay-in-milliseconds] (reagent/argv comp)]
           (go (<! (timeout delay-in-milliseconds))
               (reset! ready? true))))
       :display-name "Delay Rendering of wrapped component"
       :reagent-render
       (fn [component _delay]
         (when @ready? [:> AnimatePresence component]))})))

(defn fade-in-and-out
  "Add animation to component, which fades the component in and out."
  [component]
  [:> (.-div motion)
   {:initial {:opacity 0}
    :animate {:opacity 1}
    :exit {:opacity 0}}
   component])

(defn delayed-fade-in
  "Takes a component and applies a delay and a fade-in-and-out animation.
  Optionally takes a `delay` in milliseconds."
  ([component]
   [delayed-fade-in component 500])
  ([component delay]
   [delay-render [fade-in-and-out component] delay]))
