(ns schnaq.interface.components.navbar-lib
  (:require ["react-bootstrap" :refer [Alert]]
            ["react-bootstrap/NavDropdown" :as NavDropdown]
            [ajax.core :as ajax]
            [com.fulcrologic.guardrails.core :refer [=> >defn]]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.components.icons :refer [icon stacked-icon]]
            [schnaq.interface.matomo :as matomo]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.file-download :as file-download]
            [schnaq.interface.utils.toolbelt :as toolbelt :refer [session-storage-enabled?]]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.common :as common]))

(def ^:private NavDropdownDivider (oget NavDropdown :Divider))
(def ^:private NavDropdownItem (oget NavDropdown :Item))

(defn LanguageDropdown [& {:keys [props vertical?]}]
  (let [current-language @(rf/subscribe [:current-language])]
    [tooltip/text
     (labels :nav.buttons/language-toggle)
     [:> NavDropdown (merge {:id "language-dropdown"
                             :align :end
                             :title (r/as-element [:<> [stacked-icon :vertical? vertical? :icon-key :language] current-language])}
                            props)
      [:> NavDropdownItem {:href (navigation/switch-language-href :de)
                           :lang "de-DE" :hrefLang "de-DE"}
       "Deutsch"]
      [:> NavDropdownItem {:href (navigation/switch-language-href :en)
                           :lang "en-US" :hrefLang "en-US"}
       "English"]]]))

;; -----------------------------------------------------------------------------
;; Graph stuff... Move to other ns TODO

(defn- stabilize-graph
  "Stabilize the graph."
  []
  (let [^js graph @(rf/subscribe [:graph/get-object])]
    [:button.btn.btn-outline-primary
     {:on-click #(.stabilize graph)}
     (labels :graph.settings/stabilize)]))

(defn- gravity-slider
  "Show gravity slider."
  []
  (let [slider-id (gstring/format "gravity-slider-%s" (random-uuid))
        set-gravity! (fn [e]
                       (let [slider-value (js/parseInt (oget e [:target :value]))]
                         (rf/dispatch [:graph.settings/gravity! (/ slider-value 100)])))]
    [:div.mb-3
     [:label.form-label {:for slider-id}
      (labels :graph.settings.gravity/label)]
     [:input.form-control-range.graph-settings-gravity.d-block
      {:id slider-id
       :on-input set-gravity! ;; For browser compatibility, set both events
       :on-change set-gravity!
       :min 0 :max 100
       :value (* 100 @(rf/subscribe [:graph.settings/gravity]))
       :type "range"}]]))

(defn graph-settings-notification
  "Configure the gravity of the nodes."
  []
  (rf/dispatch
   [:notification/add
    #:notification{:title (labels :graph.settings/title)
                   :body [:<>
                          [:p (labels :graph.settings/description)]
                          [:hr] [gravity-slider]
                          [:hr] [stabilize-graph]]
                   :context :info
                   :stay-visible? true}]))

;; -----------------------------------------------------------------------------
;; Argdown Export  

(defn- create-txt-download-handler
  "Receives the export apis answer and creates a download."
  [title [ok response]]
  (when ok
    (file-download/export-data
     (gstring/format "# %s\n%s" title (:string-representation response)))))

(defn- show-error
  [& _not-needed]
  (rf/dispatch [:ajax.error/as-notification (labels :error/export-failed)]))

(>defn txt-export-request
  "Initiate an export as a txt file for the currently selected schnaq."
  [share-hash title]
  [:discussion/share-hash string? => any?]
  (ajax/ajax-request
   {:method :get
    :uri (str shared-config/api-url "/export/argdown")
    :format (ajax/transit-request-format)
    :params {:share-hash share-hash}
    :response-format (ajax/transit-response-format)
    :handler (partial create-txt-download-handler title)
    :error-handler show-error}))

;; -----------------------------------------------------------------------------

(defn- login-not-possible
  "Show a different component, if login is not possible."
  []
  [tooltip/html
   [:<> [icon :cookie-bite "me-2"] (labels :login.not-possible/tooltip)]
   [:> Alert {:variant "light" :class "m-0 p-2"}
    [icon :exclamation-triangle "me-2"] (labels :login.not-possible/text)]
   {:trigger :mouseenter}])

(defn- name-input
  "An input, where the user can set their name. Happens automatically by typing."
  []
  (let [username @(rf/subscribe [:user/display-name])]
    [:form.dropdown-item
     {:on-submit
      (fn [e] (.preventDefault e)
        (rf/dispatch [:user/set-display-name
                      (oget e [:target :elements :name-input :value])]))}
     [:input#name-input.form-control.form-round-05
      {:type "text"
       :autoFocus true
       :required true
       :defaultValue username
       :placeholder (labels :user.button/set-name-placeholder)}]
     [:input.btn.mt-1 {:type "submit"
                       :value (labels :user.button/set-name)}]]))

(defn- change-name-button
  "Display button to change the user's nickname."
  []
  [:form.clickable
   {:on-click #(rf/dispatch [:user/show-display-name-input])
    :on-submit #(.preventDefault %)}
   [:input.btn.dropdown-item {:type "submit"
                              :value (labels :user.button/change-name)
                              :on-click #(matomo/track-event "Active User", "Secondary Action", "Create User-Name")}]])

(defn- namechange-menu-point
  "A bar containing all user related utilities and information."
  []
  (if @(rf/subscribe [:user/show-display-name-input?])
    [name-input]
    [change-name-button]))

(defn admin-dropdown
  "Show Admin pages when user is authenticated and has admin role."
  [& {:keys [vertical? props]}]
  (let [admin? @(rf/subscribe [:user/administrator?])
        analytics-admin? @(rf/subscribe [:user/analytics-admin?])]
    ;; Analytics-Admin also is true when user is super-admin
    (when analytics-admin?
      [:> NavDropdown (merge {:title (r/as-element [:span.text-secondary [stacked-icon :vertical? vertical? :icon-key :ghost] "Admin"])
                              :align :end}
                             props)
       [:> NavDropdownItem {:href (navigation/href :routes/analytics)}
        (labels :router/analytics)]
       (when admin?
         [:<>
          [:> NavDropdownItem {:href (navigation/href :routes/admin-center)}
           (labels :router/admin-center)]
          [:> NavDropdownItem {:href (navigation/href :routes/feedbacks)}
           (labels :router/all-feedbacks)]
          [:> NavDropdownItem {:href (navigation/href :routes.admin/summaries)}
           (labels :router/summaries)]
          (when-not shared-config/production?
            [:> NavDropdownItem {:href (navigation/href :routes.playground/editor)}
             (labels :routes.playground/editor)])])])))

(defn- profile-picture-in-nav
  "Show profile picture-element in the navbar."
  [& {:keys [props vertical?]}]
  (let [username @(rf/subscribe [:user/display-name])
        authenticated? @(rf/subscribe [:user/authenticated?])
        pro? @(rf/subscribe [:user/pro?])
        icon-size 25]
    [:span props
     (if authenticated?
       [common/avatar
        :props (when vertical? {:className "d-block mx-auto"})
        :size icon-size
        :inline? (not vertical?)]
       [:span {:className (when vertical? "d-flex mx-auto")}
        [common/identicon
         :props {:className (if vertical? "d-block mx-auto" "me-1")}
         :name username
         :size icon-size]])
     [:span.text-nowrap
      (when pro? [icon :star "me-1"])
      (toolbelt/truncate-to-n-chars username 15)]]))

(defn user-navlink-dropdown
  [& {:keys [props vertical?]}]
  (let [authenticated? @(rf/subscribe [:user/authenticated?])]
    [:> NavDropdown (merge {:title (r/as-element [profile-picture-in-nav :vertical? vertical?])
                            :align :end}
                           props)
     (if authenticated?
       [:<>
        [:> NavDropdownItem {:disabled true} [common/avatar :size 32]]
        [:> NavDropdownDivider]
        [:> NavDropdownItem {:href (navigation/href :routes.user.manage/account)}
         (labels :user.profile/settings)]
        [:> NavDropdownItem {:on-click #(rf/dispatch [:keycloak/logout])}
         (labels :user/logout)]]
       [:<>
        [namechange-menu-point]
        (if session-storage-enabled?
          [:> NavDropdownItem {:on-click #(rf/dispatch [:keycloak/login])}
           (labels :user/register)]
          [login-not-possible])])]))

;; -----------------------------------------------------------------------------

(rf/reg-event-db
 :schnaq.qa.new-question/pulse
 (fn [db [_ pulse]]
   (assoc-in db [:schnaq :qa :new-question :pulse] pulse)))

(rf/reg-sub
 :schnaq.qa.new-question/pulse?
 (fn [db _]
   (get-in db [:schnaq :qa :new-question :pulse] false)))
