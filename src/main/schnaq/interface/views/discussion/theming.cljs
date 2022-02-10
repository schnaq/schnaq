(ns schnaq.interface.views.discussion.theming
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [com.fulcrologic.guardrails.core :refer [>defn >defn- =>]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]))

(s/def ::hex-color (s/and string? #(.startsWith % "#")))
(s/def ::css-variable (s/and string? #(.startsWith % "--")))

(s/def :theme/field #{:primary :secondary :background :logo :activation})

(>defn- color-from-root
  "Read css color value."
  [css-variable]
  [::css-variable => ::hex-color]
  (str/trim
   (.getPropertyValue
    (js/getComputedStyle (.-body js/document))
    css-variable)))

(>defn- set-root-color [css-variable color]
  [::css-variable ::hex-color => nil?]
  (.setProperty
   (.. js/document -documentElement -style)
   css-variable color))

(>defn- theme-field->css-variable
  "Convert between internally used keywords for addressing the currently
  configured field and the css-variable."
  [theme-field]
  [keyword? => ::css-variable]
  (str "--theming-" (name theme-field)))

(>defn- color-picker [theme-field]
  [keyword? => :re-frame/component]
  (let [color @(rf/subscribe [:theme/color theme-field])
        color-picker-id (str (name theme-field) "-color-picker")]
    [:div.row
     [:div.col
      [:label.form-label {:for color-picker-id} "Primäre Akzentfarbe"]]
     [:div.col
      [:input.form-control.form-control-color
       {:id color-picker-id
        :type :color
        :value color
        :on-change (fn [e]
                     (let [color (oget e :target :value)]
                       (rf/dispatch [:theme/color theme-field color])))}]]]))

(defn theming []
  [:section
   [:div.text-center
    [:h1 "Theming"]
    [:p.lead "Stelle hier das Erscheinungsbild deines schnaqs ein!"]]
   [:div.row
    [:div.col-md-3
     [:h2.h4 "Logo"]]
    [:div.col-md-4
     [:h2.h4 "Vorschaubild"]]
    [:div.col-md-5
     [:h2.h4 "Farbeinstellungen"]
     [color-picker :primary]
     [color-picker :secondary]
     [color-picker :background]]]])

(rf/reg-fx
 :page.root/set-color
 (fn [[theme-field color]]
   (set-root-color (theme-field->css-variable theme-field) color)))

(rf/reg-event-fx
 :theme/color
 (fn [{:keys [db]} [_ field color]]
   {:db (assoc-in db [:theme :new field] color)
    :fx [[:page.root/set-color [field color]]]}))

(rf/reg-sub
 :theme/color
 (fn [db [_ field]]
   (get-in db [:theme :new field])))