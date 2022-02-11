(ns schnaq.interface.views.user.themes
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [com.fulcrologic.guardrails.core :refer [>defn >defn- =>]]
            [oops.core :refer [oget oget+]]
            [re-frame.core :as rf]
            [schnaq.interface.views.schnaq.activation :as activation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.discussion.conclusion-card :refer [selection-card]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.user.settings :as settings]
            [schnaq.interface.views.discussion.card-view :as discussion-card-view]
            [schnaq.interface.views.navbar.elements :as elements]))

(s/def ::hex-color (s/and string? #(.startsWith % "#")))
(s/def ::css-variable (s/and string? #(.startsWith % "--")))
(s/def :theme/field #{:primary :secondary :background :logo :activation})

(>defn- theme-field->css-variable
  "Convert between internally used keywords for addressing the currently
  configured field and the css-variable."
  [theme-field]
  [keyword? => ::css-variable]
  (str "--theming-" (name theme-field)))

(>defn- color-from-root
  "Read css color value."
  [theme-field]
  [:theme/field => ::hex-color]
  (str/trim
   (.getPropertyValue
    (js/getComputedStyle (.-body js/document))
    (theme-field->css-variable theme-field))))

(>defn- set-root-color
  "Set global root css definition to document."
  [css-variable color]
  [::css-variable ::hex-color => nil?]
  (.setProperty
   (.. js/document -documentElement -style)
   css-variable color))

(>defn- color-picker
  "Color picker for the theme colors."
  [theme-field label]
  [keyword? string? => :re-frame/component]
  (let [color @(rf/subscribe [:theme/field theme-field])
        color-picker-id (str (name theme-field) "-color-picker")]
    [:div.row
     [:div.col
      [:label.form-label {:for color-picker-id} label]]
     [:div.col
      [:input.form-control.form-control-color
       {:id color-picker-id
        :type :color
        :value (if color color (color-from-root theme-field))
        :name (str "color-" (name theme-field))
        :on-change (fn [e]
                     (let [color (oget e :target :value)]
                       (rf/dispatch [:theme/field theme-field color])))}]]]))

(defn- preview []
  [:<>
   [:h2 "Vorschau"]
   [:section.theming-enabled
    [:div.base-wrapper.p-3
     [elements/navbar-title
      [:h1.h6.fw-bold.my-auto.text-dark "Welcome to schnaq"]
      nil false]
     [activation/activation-card]
     [selection-card]]]])

(defn theming []
  [:section
   [:div.text-center
    [:p.lead.pb-3 "Stelle hier das Erscheinungsbild deiner schnaqs ein!"]]
   [:form
    {:on-submit (fn [e]
                  (js-wrap/prevent-default e)
                  (let [get-field #(oget+ e [:target :elements % :value])]
                    (prn {:colors {:primary (get-field :color-primary)
                                   :secondary (get-field :color-secondary)
                                   :background (get-field :color-background)}})))}
    [:div.form-floating.mb-3
     [:input.form-control
      {:id "theme-title"
       :placeholder "Give your theme a title"
       :required true
       :value "Title"
       :name "title"
       :on-change #(prn "change")}]
     [:label {:for "theme-title"} "Give your theme a title"]]
    [:div.row
     [:div.col-md-5
      [:strong "Logo"]
      [:p.text-muted "Coming Soon"]
      [:strong "Vorschaubild"]
      [:p.text-muted "Coming Soon"]]
     [:div.col-md-7
      [:strong "Farbeinstellungen"]
      [color-picker :primary "Primäre Farbe"]
      [color-picker :secondary "Sekundäre Farbe"]
      [color-picker :background "Hintergrundfarbe"]]]
    [:button.btn.btn-outline-primary {:type :submit} "Speichern"]]
   [:hr.my-5]
   [preview]])

(defn view []
  [settings/user-view
   :user.settings/themes
   [pages/settings-panel
    "Thema / Branding definieren"
    [theming]]])

;; -----------------------------------------------------------------------------

(rf/reg-fx
 :page.root/set-color
 (fn [[theme-field color]]
   (set-root-color (theme-field->css-variable theme-field) color)))

(rf/reg-event-fx
 :theme/field
 (fn [{:keys [db]} [_ field color]]
   {:db (assoc-in db [:theme :preview field] color)
    :fx [[:page.root/set-color [field color]]]}))

(rf/reg-sub
 :theme/field
 (fn [db [_ field]]
   (get-in db [:theme :preview field])))

(rf/reg-event-fx
 :theming/dummy
 ;; Add dummy data to the selected schnaq, e.g. for preview functions
 (fn [{:keys [db]}]
   (let [discussion #:discussion{:author {:user.registered/display-name "schnaqqi"}
                                 :title "Welcome to schnaq"}
         conclusion #:statement{:content "Welcome to schnaq"
                                :author {:user.registered/display-name "schnaqqi"}
                                :created-at nil}]
     {:db (-> db
              (assoc-in [:schnaq :selected] discussion)
              (assoc-in [:discussion :conclusion :selected] conclusion))
      :fx [[:dispatch [:schnaq.activation/temp-counter 0]]]})))
