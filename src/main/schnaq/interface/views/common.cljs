(ns schnaq.interface.views.common
  (:require ["framer-motion" :refer [motion AnimatePresence]]
            ["jdenticon" :as jdenticon]
            [cljs.core.async :refer [go <! timeout]]
            [cljs.spec.alpha :as s]
            [ghostwheel.core :refer [>defn]]
            [goog.string :as gstring]
            [oops.core :refer [oset!]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [schnaq.interface.text.display-data :refer [img-path]]
            [schnaq.interface.utils.toolbelt :as toolbelt]))

(def ^:private default-identicon-background-color
  "#fafafa")

(defn- generate-identicon
  "Generate an identicon. Returns xml-styled SVG."
  ([display-name size]
   (generate-identicon display-name size default-identicon-background-color))
  ([display-name size background-color]
   (jdenticon/toSvg display-name size (clj->js {:backColor background-color}))))

(defn- set-fallback-identicon
  "If image loading fails, set an identicon."
  [display-name size]
  (fn [image]
    (oset! image [:target :src]
           (str "data:image/svg+xml;base64,"
                (js/btoa (generate-identicon display-name size))))))

(>defn identicon
  "Generate unique identicon component."
  ([display-name]
   [string? :ret vector?]
   [identicon display-name])
  ([display-name size]
   [string? number? :ret vector?]
   [:span.shadow-sm {:title display-name
                     :dangerouslySetInnerHTML
                     {:__html (generate-identicon display-name size)}}]))

(>defn avatar
  "Get a user's avatar."
  ([size]
   [nat-int? :ret vector?]
   (let [user @(rf/subscribe [:user/current])
         user-registered #:user.registered{:profile-picture (get-in user [:profile-picture :display])
                                           :display-name (get-in user [:names :display])}]
     [avatar user-registered size]))
  ([{:user.registered/keys [profile-picture display-name] :as user} size]
   [map? nat-int? :ret vector?]
   (let [display-name (or display-name (:user/nickname user))]
     [:div.avatar-image.p-0
      (if profile-picture
        [:div.profile-pic-fill
         {:style {:max-height (str size "px") :max-width (str size "px")}}
         [:img.profile-pic-image {:src profile-picture
                                  :alt (str "Profile Picture of " display-name)
                                  :on-error (set-fallback-identicon display-name 50)}]]
        [identicon display-name size])])))

(>defn avatar-with-nickname-right
  "Create an image based on the nickname and also print the nickname."
  [{:user.registered/keys [display-name] :as user} size]
  [map? number? :ret vector?]
  [:div.row
   [:div.mr-4 [avatar user size]]
   [:h4.my-auto display-name]])

(defn inline-avatar
  "Creates an inline image and name."
  [{:user.registered/keys [display-name] :as user} size]
  [:<>
   [:div.d-inline-block.pr-1
    [avatar user size]]
   [:p.d-inline-block display-name]])

(>defn add-namespace-to-keyword
  "Prepend a namespace to a keyword. Replaces existing namespace with new
  namespace."
  [prepend-namespace to-keyword]
  [(s/or :keyword keyword? :string string?) keyword? :ret keyword?]
  (keyword (str (name prepend-namespace)) (str (name to-keyword))))

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

(>defn set-website-title!
  "Set a document's website title."
  [title]
  [string? :ret nil?]
  (let [new-title (gstring/format "schnaq - %s" title)]
    (oset! js/document [:title] new-title)))


;; -----------------------------------------------------------------------------
;; schnaqqi speak

(defn- schnaqqi-speech-bubble-builder
  "Build a schnaqqi-speech composition."
  [schnaqqi-size bubble-content css-classes image-key]
  [number? vector? string? keyword?]
  [:section.d-flex
   [:div.speech-bubble.text-center {:class css-classes} bubble-content]
   [:img {:style {:width schnaqqi-size}
          :alt "schnaqqi speaking"
          :src (img-path image-key)}]])

(defn schnaqqi-speech-bubble-white
  "Create a speech bubble left of a white schnaqqi and let him speak to the audience."
  [schnaqqi-size bubble-content]
  [schnaqqi-speech-bubble-builder
   schnaqqi-size bubble-content nil :schnaqqifant/white])

(defn schnaqqi-speech-bubble-blue
  "Create a speech bubble left of a blue schnaqqi and let him speak to the audience."
  [schnaqqi-size bubble-content]
  [schnaqqi-speech-bubble-builder
   schnaqqi-size bubble-content "speech-bubble-bordered" :schnaqqifant/original])


;; -----------------------------------------------------------------------------
;; Form-related

(defn form-input
  "The input form for the display name."
  [{:keys [id placeholder default-value] :as properties}]
  [:input.form-control.form-border-bottom.mb-2
   (merge {:key (str id placeholder default-value)
           :type "text"
           :autoComplete "off"
           :required true}
          properties)])


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

(defn move-in
  "Add animation to component, which fades the component in and out."
  [from-direction component]
  (let [direction (case from-direction
                    :top {:y "-200%"}
                    :bottom {:y "-200%"}
                    :left {:x "-200%"}
                    {:x "200%"})]
    [:> (.-div motion)
     {:initial direction
      :animate {:x 0 :y 0}
      :exit direction
      :transition {:ease "easeOut" :duration 0.5}}
     component]))
