(ns schnaq.interface.views.user.themes
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [com.fulcrologic.guardrails.core :refer [>defn- =>]]
            [oops.core :refer [oget oget+]]
            [re-frame.core :as rf]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.discussion.conclusion-card :refer [selection-card]]
            [schnaq.interface.views.navbar.elements :as elements]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.schnaq.activation :as activation]
            [schnaq.interface.views.user.settings :as settings]))

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

(>defn- remove-root-color
  "Remove a global definition of a css var from the document."
  [css-variable]
  [::css-variable => nil?]
  (.removeProperty
   (.. js/document -documentElement -style)
   css-variable))

(>defn- color-picker
  "Color picker for the theme colors."
  [theme-field label]
  [keyword? string? => :re-frame/component]
  (let [color (get @(rf/subscribe [:theme/selected]) theme-field)
        color-picker-id (str (name theme-field) "-color-picker")]
    [:div.row
     [:div.col
      [:label.form-label {:for color-picker-id} label]]
     [:div.col
      [:input.form-control.form-control-color
       {:id color-picker-id
        :type :color
        :value (if color color (color-from-root (keyword (name theme-field))))
        :name (str "color-" (name theme-field))
        :on-change (fn [e]
                     (let [color (oget e :target :value)]
                       (rf/dispatch [:theme.selected/color theme-field color])))}]]]))

(defn- preview []
  (when @(rf/subscribe [:theme/selected])
    [:<>
     [:hr.my-5]
     [:h2 "Vorschau"]
     [:section.theming-enabled
      [:div.base-wrapper.p-3
       [elements/navbar-title
        [:h1.h6.fw-bold.my-auto.text-dark "Welcome to schnaq"]
        nil false]
       [activation/activation-card]
       [:div.d-flex.flex-row
        [buttons/button "primary button"]
        [buttons/button "secondary button" (constantly "#") "btn-secondary ms-2"]
        [buttons/button "primary outlined button" (constantly "#") "btn-outline-primary ms-2"]
        [buttons/button "secondary outlined button" (constantly "#") "btn-outline-secondary ms-2"]]
       [selection-card]]]]))

;; -----------------------------------------------------------------------------

(defn list-personal-themes
  "Show all configured themes."
  [dispatch-event]
  (let [themes @(rf/subscribe [:themes/personal])
        selected @(rf/subscribe [:theme/selected])]
    [:div.row.row-cols-2.row-cols-xl-4.g-2.g-lg-3
     (for [theme themes]
       (with-meta
         [:div.col
          [buttons/button
           (toolbelt/truncate-to-n-chars-string (:theme/title theme) 24)
           #(rf/dispatch [dispatch-event theme])
           (if (= (:db/id selected) (:db/id theme))
             "btn-secondary w-100 h-100"
             "btn-outline-dark w-100 h-100")]]
         {:key (str "theme-" (:db/id theme))}))]))

(defn- loaded-themes
  "Display all available themes."
  []
  (let [user-name @(rf/subscribe [:user/display-name])]
    [:section.pb-5
     [:h4 "Deine Themen"]
     [:p.lead "Wähle ein bestehendes Thema aus oder erzeuge ein neues."]
     [list-personal-themes :theme/select]
     [:div.pt-3
      [buttons/button
       "Neu erstellen"
       (fn []
         (rf/dispatch [:theme/reset])
         (rf/dispatch [:theme.selected/update :theme/title (str user-name "'s first theme")]))
       "btn-outline-primary h-100"]]]))

(defn- configure-theme
  "TODO"
  []
  (when-let [selected @(rf/subscribe [:theme/selected])]
    [:<>
     [:form
      {:on-submit (fn [e]
                    (.preventDefault e)
                    (let [add-or-edit (if (:db/id selected) :theme/edit :theme/add)]
                      (rf/dispatch [add-or-edit (oget e [:target :elements])])))}
      [:div.form-floating.mb-3
       [:input.form-control
        {:id "theme-title"
         :placeholder "Give your theme a title"
         :required true
         :value (:theme/title selected)
         :name "title"
         :on-change #(rf/dispatch [:theme.selected/update :theme/title
                                   (oget % [:target :value])])}]
       [:label {:for "theme-title"} "Give your theme a title"]]
      [:div.row
       [:div.col-md-5
        [:strong "Logo"]
        [:p.text-muted "Coming Soon"]
        [:strong "Vorschaubild"]
        [:p.text-muted "Coming Soon"]]
       [:div.col-md-7
        [:strong "Farbeinstellungen"]
        [color-picker :theme.colors/primary "Primäre Farbe"]
        [color-picker :theme.colors/secondary "Sekundäre Farbe"]
        [color-picker :theme.colors/background "Hintergrundfarbe"]]]
      [:input {:type :hidden :name "theme-id" :value (or (:db/id selected) "")}]
      [:button.btn.btn-outline-primary {:type :submit} "Speichern"]]
     (when-let [theme-id (:db/id selected)]
       [:button.float-end.btn.btn-sm.btn-link.text-danger
        {:on-click #(when (js/confirm "Möchtest du das Thema wirklich löschen?")
                      (rf/dispatch [:theme/delete theme-id]))}
        "Löschen"])]))

(defn theming
  "Main Theming view."
  []
  [:section
   [:div.text-center
    [:p.lead.pb-3 "Stelle hier das Erscheinungsbild deiner schnaqs ein!"]]
   [loaded-themes]
   [motion/fade-in-and-out [configure-theme]]
   [motion/fade-in-and-out [preview]]])

(defn view []
  [settings/user-view
   :user.settings/themes
   [pages/settings-panel
    "Thema / Branding definieren"
    [theming]]])

;; -----------------------------------------------------------------------------

(defn select-theme-for-schnaq
  "TODO"
  []
  [:section.mb-5
   [:h4 "Stelle hier dein Farbschema für diesen schnaq ein"]
   [:p "Sobald du ein Thema ausgewählt hast, wird es für diesen schnaq gespeichert. Deine Besucher:innen sehen dann beim nächsten Laden des schnaqs das neue Farbschema."]
   [list-personal-themes :theme.assign/discussion]])

(rf/reg-event-fx
 :theme.assign/discussion
 (fn [{:keys [db]} [_ theme]]
   {:fx [[:dispatch [:theme/select theme]]
         (http/xhrio-request db :put "/user/theme/assign/discussion"
                             [:theme.assign.discussion/success]
                             {:theme theme
                              :share-hash (get-in db [:schnaq :selected :discussion/share-hash])})]}))

(rf/reg-event-fx
 :theme.assign.discussion/success
 (fn []
   {:fx [[:dispatch [:notification/add
                     #:notification{:title "Thema erfolgreich gespeichert"
                                    :body "Dein Thema kann nun von dir in deinen schnaqs verwendet werden 🎉"
                                    :context :success}]]]}))

(rf/reg-event-fx
 :theme.apply/from-discussion
 (fn [{:keys [db]}]
   (let [theme (get-in db [:schnaq :selected :discussion/theme])]
     {:fx [[:page.root/set-color [:primary (:theme.colors/primary theme)]]
           [:page.root/set-color [:secondary (:theme.colors/secondary theme)]]
           [:page.root/set-color [:background (:theme.colors/background theme)]]]})))

;; -----------------------------------------------------------------------------

(rf/reg-fx
 :page.root/set-color
 (fn [[theme-field color]]
   (when color
     (set-root-color (theme-field->css-variable theme-field) color))))

(rf/reg-fx
 :page.root/remove-color
 (fn [theme-field]
   (remove-root-color (theme-field->css-variable theme-field))))

(rf/reg-event-fx
 :theme.selected/color
 (fn [{:keys [db]} [_ field color]]
   {:db (assoc-in db [:themes :selected field] color)
    :fx [[:page.root/set-color [field color]]]}))

(rf/reg-event-db
 :theme.selected/update
 (fn [db [_ field color]]
   (assoc-in db [:themes :selected field] color)))

(rf/reg-sub
 :theme/selected
 (fn [db]
   (get-in db [:themes :selected])))

(rf/reg-event-fx
 :theme/select
 (fn [{:keys [db]} [_ theme]]
   {:db (assoc-in db [:themes :selected] theme)
    :fx [[:page.root/set-color [:primary (:theme.colors/primary theme)]]
         [:page.root/set-color [:secondary (:theme.colors/secondary theme)]]
         [:page.root/set-color [:background (:theme.colors/background theme)]]]}))

(rf/reg-event-fx
 :theme/dummy
 ;; Add dummy data to the selected schnaq, e.g. for preview functions
 (fn [{:keys [db]}]
   (let [discussion #:discussion{:author {:user.registered/display-name "schnaqqi"}
                                 :title "Welcome to schnaq"
                                 :states [:discussion.state/read-only]}
         conclusion #:statement{:content "Welcome to schnaq"
                                :author {:user.registered/display-name "schnaqqi"}
                                :created-at nil}]
     {:db (-> db
              (assoc-in [:schnaq :selected] discussion)
              (assoc-in [:discussion :conclusion :selected] conclusion))
      :fx [[:dispatch [:schnaq.activation/temp-counter 0]]]})))

;; -----------------------------------------------------------------------------

(defn- theme-from-form
  "Extract theme information from form."
  [form]
  (let [get-field #(oget+ form [% :value])]
    {:db/id (get-field :theme-id)
     :theme/title (get-field :theme-title)
     :theme.colors/primary (get-field :color-primary)
     :theme.colors/secondary (get-field :color-secondary)
     :theme.colors/background (get-field :color-background)}))

(rf/reg-event-fx
 :theme/add
 ;; Send new theme to backend
 (fn [{:keys [db]} [_ form]]
   {:fx [(http/xhrio-request db :post "/user/theme/add"
                             [:theme.add-or-edit/success]
                             {:theme (-> form
                                         theme-from-form
                                         (dissoc :db/id))})]}))

(rf/reg-event-fx
 :theme/edit
 ;; Send new theme to backend
 (fn [{:keys [db]} [_ form]]
   {:fx [(http/xhrio-request db :put "/user/theme/edit"
                             [:theme.add-or-edit/success]
                             {:theme (theme-from-form form)})]}))

(rf/reg-event-fx
 :theme.add-or-edit/success
 (fn [_ [_ {:keys [theme]}]]
   {:fx [[:dispatch [:notification/add
                     #:notification{:title "Thema erfolgreich gespeichert"
                                    :body "Dein Thema kann nun von dir in deinen schnaqs verwendet werden 🎉"
                                    :context :success}]]
         [:dispatch [:theme/select theme]]
         [:dispatch [:themes.load/personal]]]}))

(rf/reg-event-fx
 :theme/delete
 (fn [{:keys [db]} [_ theme-id]]
   {:fx [(http/xhrio-request db :delete "/user/theme/delete"
                             [:themes.load.personal/success]
                             {:theme-id theme-id})
         [:dispatch [:theme/reset]]]}))

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :themes.load/personal
 (fn [{:keys [db]}]
   {:fx [(http/xhrio-request db :get "/user/themes" [:themes.load.personal/success])]}))

(rf/reg-event-db
 :themes.load.personal/success
 ;; Load personal themes
 (fn [db [_ {:keys [themes]}]]
   (assoc-in db [:themes :all] themes)))

(rf/reg-sub
 :themes/personal
 (fn [db]
   (get-in db [:themes :all])))

(rf/reg-event-db
 :themes/dissoc
 (fn [db]
   (dissoc db :themes)))

(rf/reg-event-fx
 :theme/reset
 (fn [{:keys [db]}]
   {:db (update db :themes dissoc :selected)
    :fx [[:page.root/remove-color :primary]
         [:page.root/remove-color :secondary]
         [:page.root/remove-color :background]]}))