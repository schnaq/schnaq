(ns schnaq.interface.views.user.themes
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [com.fulcrologic.guardrails.core :refer [>defn- =>]]
            [goog.string :as gstring]
            [oops.core :refer [oget oget+]]
            [re-frame.core :as rf]
            [schnaq.interface.components.buttons :as buttons]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.inputs :as inputs]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.matomo :as matomo]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.discussion.conclusion-card :refer [info-card selection-card]]
            [schnaq.interface.views.navbar.elements :as elements]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.schnaq.activation :as activation]
            [schnaq.interface.views.user.settings :as settings]
            [schnaq.shared-toolbelt :as shared-tools]))

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
  [::css-variable => any?]
  (.removeProperty
   (.. js/document -documentElement -style)
   css-variable))

(>defn- color-picker
  "Color picker for the theme colors."
  [theme-field label]
  [keyword? string? => :re-frame/component]
  (let [color (get @(rf/subscribe [:schnaq.selected/theme]) theme-field)
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
  (when @(rf/subscribe [:schnaq.selected/theme])
    [:<>
     [:h3#theme-preview-title (labels :themes.personal.preview/heading)]
     [:section.theming-enabled
      [:div.base-wrapper.p-3
       [elements/navbar-title
        [:h1.h6.fw-bold.my-auto.text-dark "Welcome to schnaq"]
        false]
       [activation/activation-card]
       [:div.d-flex.flex-row
        [buttons/button "primary button"]
        [buttons/button "secondary button" nil "btn-secondary ms-2"]
        [buttons/button "primary outlined button" nil "btn-outline-primary ms-2"]
        [buttons/button "secondary outlined button" nil "btn-outline-secondary ms-2"]]
       [info-card]
       [selection-card]]]]))

;; -----------------------------------------------------------------------------

(defn- list-personal-themes
  "Show all configured themes."
  [dispatch-event]
  (let [themes @(rf/subscribe [:themes/personal])
        selected @(rf/subscribe [:schnaq.selected/theme])]
    [:div.row.row-cols-2.row-cols-xl-4.g-2.g-lg-3
     (for [theme themes]
       [:div.col
        {:key (str "theme-" (:db/id theme))}
        [buttons/button
         (toolbelt/truncate-to-n-chars-string (:theme/title theme) 24)
         #(rf/dispatch [dispatch-event theme])
         (if (= (:db/id selected) (:db/id theme))
           "btn-secondary w-100 h-100"
           "btn-outline-dark w-100 h-100")]])]))

(defn- loaded-themes
  "Display all available themes."
  []
  (let [user-name @(rf/subscribe [:user/display-name])
        pro-user? @(rf/subscribe [:user/pro?])]
    [:section.pb-5
     [:h3 (labels :themes.personal.creation/heading)]
     [:p (labels :themes.personal.creation/lead)]
     (when-not pro-user?
       [:div.alert.alert-info (labels :themes.personal.creation/pro-hint)])
     [list-personal-themes :theme/select]
     [:div.pt-3
      [buttons/button
       (labels :themes.personal.creation.buttons/create-new)
       (fn []
         (rf/dispatch [:theme/reset])
         (rf/dispatch [:theme.selected/update :theme/title
                       (gstring/format (labels :themes.personal.creation/theme-placeholder) user-name)])
         (matomo/track-event "Active User", "Secondary Action", "Design New Theme"))
       "btn-outline-primary h-100"
       {:id "design-create-new"}]]]))

;; -----------------------------------------------------------------------------

(defn- save-button-or-carrot
  "Activate save-button only for pro-users. Other users see an subscription 
  information."
  []
  (let [pro-user? @(rf/subscribe [:user/pro?])]
    [:<>
     (when-not pro-user?
       [:div.alert.alert-info
        (labels :themes.pro-carrot/text)
        " 🚀"])
     [:button.btn.btn-outline-primary
      {:type :submit
       :disabled (not pro-user?)}
      (labels :themes.personal.creation.buttons/save)]]))

(defn- input-activation-phrase
  "Change the activation phrase.
  Schnaqqi won't like this #sadtorooo."
  []
  (let [selected @(rf/subscribe [:schnaq.selected/theme])]
    [:div.form-floating.mb-3
     [:input.form-control
      {:id "theme-activation-phrase"
       :placeholder (labels :schnaq.activation/phrase)
       :value (:theme.texts/activation selected)
       :name "activation-phrase"
       :on-change #(rf/dispatch [:theme.selected/update :theme.texts/activation
                                 (oget % [:target :value])])}]
     [:label {:for "theme-activation-phrase"}
      (labels :themes.personal.creation.texts/activation)]]))

(defn- input-title
  "Select a theme's title."
  []
  (let [selected @(rf/subscribe [:schnaq.selected/theme])]
    [:div.form-floating.mb-3
     [:input.form-control
      {:id "theme-title"
       :placeholder (labels :themes.personal.creation.title/label)
       :required true
       :value (:theme/title selected)
       :name "theme-title"
       :on-change #(rf/dispatch [:theme.selected/update :theme/title
                                 (oget % [:target :value])])}]
     [:label {:for "theme-title"} (labels :themes.personal.creation.title/label)]]))

(defn- delete-button
  "Delete theme or a single image."
  [text confirmation-text deletion-fn]
  [:button.float-end.btn.btn-sm.btn-link.text-danger
   {:on-click #(when (js/confirm confirmation-text)
                 (.preventDefault %)
                 (deletion-fn))}
   text])

(rf/reg-event-db
 :themes.personal.header/reset
 (fn [db _]
   (-> db
       (assoc-in [:schnaq :selected :discussion/theme :delete :header] true)
       (update-in [:schnaq :selected :discussion/theme :temporary] dissoc :header)
       (update-in [:schnaq :selected :discussion/theme] dissoc :theme.images/header))))

(rf/reg-event-db
 :themes.personal.logo/reset
 (fn [db _]
   (-> db
       (assoc-in [:schnaq :selected :discussion/theme :delete :logo] true)
       (update-in [:schnaq :selected :discussion/theme :temporary] dissoc :logo)
       (update-in [:schnaq :selected :discussion/theme] dissoc :theme.images/logo))))

(defn- image-upload-with-preview
  "Add image inputs and provide a preview if image is present."
  []
  (let [{:theme.images/keys [logo header]} @(rf/subscribe [:schnaq.selected/theme])]
    [:<>
     [:div.row.pb-3
      [:div.col-md-8
       [inputs/image
        (labels :themes.personal.creation.images.logo/title)
        "input-logo"
        [:schnaq :selected :discussion/theme :temporary :logo]]]
      [:div.col-md-4.pt-4
       (when logo
         [:<>
          [:img.img-fluid {:src (gstring/format "%s?%s" logo (.getTime (js/Date.)))
                           :alt (labels :themes.personal.creation.images.logo/alt)}]
          [delete-button
           (labels :themes.personal.edit.image/delete)
           (labels :themes.personal.edit.image/delete-confirmation)
           #(rf/dispatch [:themes.personal.logo/reset])]])]]
     [:div.row.pb-3
      [:div.col-md-8
       [inputs/image
        (labels :themes.personal.creation.images.header/title)
        "input-header"
        [:schnaq :selected :discussion/theme :temporary :header]]
       [:div.pt-3.small.text-info
        [icon :info "me-1"] (labels :themes.personal.creation.images/info)]]
      [:div.col-md-4.pt-4
       (when header
         [:<>
          [:img.img-fluid {:src (gstring/format "%s?%s" header (.getTime (js/Date.)))
                           :alt (labels :themes.personal.creation.images.header/title)}]
          [delete-button
           (labels :themes.personal.edit.image/delete)
           (labels :themes.personal.edit.image/delete-confirmation)
           #(rf/dispatch [:themes.personal.header/reset])]])]]]))

(defn- configure-theme
  "Form to configure theme."
  []
  (when-let [selected @(rf/subscribe [:schnaq.selected/theme])]
    (let [pro-user? @(rf/subscribe [:user/pro?])
          theme-id (:db/id selected)]
      [:<>
       [:form
        {:on-submit (fn [e]
                      (.preventDefault e)
                      (let [add-or-edit (if theme-id :theme/edit :theme/add)]
                        (rf/dispatch [add-or-edit (oget e [:target :elements])])))}
        [input-title]
        [:div.row
         [:div.col-md-7
          [:strong (labels :themes.personal.creation.colors/title)]
          [color-picker :theme.colors/primary (labels :themes.personal.creation.colors.primary/title)]
          [color-picker :theme.colors/secondary (labels :themes.personal.creation.colors.secondary/title)]
          [color-picker :theme.colors/background (labels :themes.personal.creation.colors.background/title)]]
         [:div.col-md-5
          [input-activation-phrase]]]
        [image-upload-with-preview]
        [:input {:type :hidden :name "theme-id" :value (or theme-id "")}]
        [save-button-or-carrot]]
       (when (and theme-id pro-user?)
         [delete-button
          (labels :themes.personal.creation.buttons/delete)
          (labels :themes.personal.creation.delete/confirmation)
          #(rf/dispatch [:theme/delete theme-id])])])))

;; -----------------------------------------------------------------------------

(defn theming
  "Main Theming view."
  []
  [:section
   [:div.text-center
    [:p.lead.pb-3 (labels :themes.personal/lead)]]
   [loaded-themes]
   [motion/fade-in-and-out [configure-theme]]
   [:hr.my-5]
   [motion/fade-in-and-out [preview]]])

(defn view []
  [settings/user-view
   :user.settings/themes
   [pages/settings-panel
    (labels :user.settings/themes)
    [theming]]])

;; -----------------------------------------------------------------------------
;; Theme assignment to schnaq

(defn assign-theme-to-schnaq
  "Assigns a theme to a schnaq."
  []
  [:section
   [:h4 (labels :themes.schnaq.settings/heading)]
   [:p (labels :themes.schnaq.settings/lead)]
   [list-personal-themes :theme.discussion/assign]
   [:div.d-flex.flex-row.pt-3
    [buttons/button
     (labels :themes.schnaq.settings.buttons/edit)
     #(rf/dispatch [:navigation/navigate :routes.user.manage/themes])
     "btn-outline-info"]
    [buttons/button
     (labels :themes.schnaq.settings.buttons/unassign)
     (fn [_e]
       (when (js/confirm (labels :themes.schnaq.settings.unassign/confirmation))
         (rf/dispatch [:theme.discussion/unassign])))
     "btn-link btn-sm text-danger ms-3"]]])

(rf/reg-event-fx
 :theme.discussion/assign
 (fn [{:keys [db]} [_ theme]]
   {:fx [[:dispatch [:theme/select theme]]
         (http/xhrio-request db :put "/user/theme/discussion/assign"
                             [:no-op]
                             {:theme theme
                              :share-hash (get-in db [:schnaq :selected :discussion/share-hash])
                              :edit-hash (get-in db [:schnaq :selected :discussion/edit-hash])})]}))

(rf/reg-event-fx
 :theme.discussion/unassign
 (fn [{:keys [db]}]
   {:fx [[:dispatch [:theme/reset]]
         (http/xhrio-request db :delete "/user/theme/discussion/unassign"
                             [:theme.discussion.unassign/success]
                             {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])
                              :edit-hash (get-in db [:schnaq :selected :discussion/edit-hash])})]}))

(rf/reg-event-fx
 :theme.discussion.unassign/success
 (fn [_]
   {:fx [[:dispatch [:notification/add
                     #:notification{:title (labels :themes.schnaq.unassign.notification/title)
                                    :body (labels :themes.schnaq.unassign.notification/body)
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
 (fn [_ [_ field color]]
   {:fx [[:dispatch [:theme.selected/update field color]]
         [:page.root/set-color [field color]]]}))

(rf/reg-event-db
 :theme.selected/update
 (fn [db [_ field color]]
   (assoc-in db [:schnaq :selected :discussion/theme field] color)))

(rf/reg-event-fx
 :theme/select
 (fn [{:keys [db]} [_ theme]]
   {:db (assoc-in db [:schnaq :selected :discussion/theme] theme)
    :fx [[:page.root/set-color [:primary (:theme.colors/primary theme)]]
         [:page.root/set-color [:secondary (:theme.colors/secondary theme)]]
         [:page.root/set-color [:background (:theme.colors/background theme)]]]}))

(rf/reg-event-fx
 :theme/dummy
 ;; Add dummy data to the selected schnaq, e.g. for preview functions
 (fn [{:keys [db]}]
   (let [discussion #:discussion{:author {:user.registered/display-name "schnaqqi"}
                                 :title "Welcome to schnaq"
                                 :states #{:discussion.state/read-only}}
         dummy-conclusion-id :dummy-conclusion
         conclusion #:statement{:db/id dummy-conclusion-id
                                :content "Welcome to schnaq"
                                :author {:user.registered/display-name "schnaqqi"}
                                :created-at nil}]
     {:db (-> db
              (assoc-in [:schnaq :selected] discussion)
              (assoc-in [:statements :focus] dummy-conclusion-id)
              (assoc-in [:schnaq :statements dummy-conclusion-id] conclusion))
      :fx [[:dispatch [:schnaq.activation/temp-counter 0]]]})))

;; -----------------------------------------------------------------------------

(defn- theme-builder
  "Extract theme information from form."
  [db form]
  (let [get-field #(oget+ form [% :value])]
    (shared-tools/remove-nil-values-from-map
     {:db/id (get-field :theme-id)
      :theme/title (get-field :theme-title)
      :theme.colors/primary (get-field :color-primary)
      :theme.colors/secondary (get-field :color-secondary)
      :theme.colors/background (get-field :color-background)
      :theme.texts/activation (get-field :activation-phrase)
      :theme.images.raw/logo (get-in db [:schnaq :selected :discussion/theme :temporary :logo])
      :theme.images.raw/header (get-in db [:schnaq :selected :discussion/theme :temporary :header])})))

(rf/reg-event-fx
 :theme/add
 ;; Send new theme to backend
 (fn [{:keys [db]} [_ form]]
   {:fx [(http/xhrio-request db :post "/user/theme/add"
                             [:theme.save/success]
                             {:theme (-> (theme-builder db form)
                                         (dissoc :db/id))})]}))

(rf/reg-event-fx
 :theme/edit
 (fn [{:keys [db]} [_ form]]
   {:fx [(http/xhrio-request db :put "/user/theme/edit"
                             [:theme.save/success]
                             {:theme (theme-builder db form)
                              :delete-header? (get-in db [:schnaq :selected :discussion/theme :delete :header] false)
                              :delete-logo? (get-in db [:schnaq :selected :discussion/theme :delete :logo] false)})]}))

(rf/reg-event-fx
 :theme.save/success
 (fn [{:keys [db]} [_ {:keys [theme]}]]
   {:db (assoc-in db [:schnaq :selected :discussion/theme] theme)
    :fx [[:dispatch [:notification/add
                     #:notification{:title (labels :themes.save.notification/title)
                                    :body [:<> (labels :themes.save.notification/body) " 🎉"]
                                    :context :success}]]
         [:dispatch [:theme/select theme]]
         [:dispatch [:themes.load/personal]]]}))

(rf/reg-event-fx
 :theme/delete
 (fn [{:keys [db]} [_ theme-id]]
   {:fx [(http/xhrio-request db :delete "/user/theme/delete"
                             [:themes.load.personal/success]
                             {:theme {:db/id theme-id}})
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
   {:db (update-in db [:schnaq :selected] dissoc :discussion/theme)
    :fx [[:page.root/remove-color :primary]
         [:page.root/remove-color :secondary]
         [:page.root/remove-color :background]]}))
