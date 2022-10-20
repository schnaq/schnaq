(ns schnaq.interface.views.navbar.elements
  (:require ["react-bootstrap/Navbar" :as Navbar]
            ["react-bootstrap/NavDropdown" :as NavDropdown]
            [ajax.core :as ajax]
            [com.fulcrologic.guardrails.core :refer [=> >defn ?]]
            [goog.string :as gstring]
            [oops.core :refer [oget oset!]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.components.common :as common-components]
            [schnaq.interface.components.icons :refer [icon stacked-icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.file-download :as file-download]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.navbar.user-management :as um]))

(def ^:private NavbarText (oget Navbar :Text))
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

(defn language-dropdown
  "Dropdown for bootstrap navbar to display the allowed languages."
  ([]
   [language-dropdown true false {}])
  ([side-by-side? small? options]
   (let [icon-classes (if side-by-side? "" "d-block mx-auto")]
     [:<>
      [:button#schnaq-language-dropdown.btn.btn-link.nav-link.dropdown-toggle
       (merge
        {:role "button" :data-bs-toggle "dropdown"
         :aria-haspopup "true" :aria-expanded "false"}
        options)
       [icon :language icon-classes {:size "lg"}]
       [:span {:class (when small? "small")} " " @(rf/subscribe [:current-language])]]
      [:div.dropdown-menu {:aria-labelledby "schnaq-language-dropdown"}
       [:a.btn.dropdown-item
        {:href (navigation/switch-language-href :de)
         :lang "de-DE" :hrefLang "de-DE"}
        "Deutsch"]
       [:a.btn.dropdown-item
        {:href (navigation/switch-language-href :en)
         :lang "en-US" :hrefLang "en-US"}
        "English"]]])))

(defn language-toggle-with-tooltip
  "Uses language-dropdown and adds a mouse-over label."
  [side-by-side? small? options]
  [tooltip/text
   (labels :nav.buttons/language-toggle)
   [:span [language-dropdown side-by-side? small? options]]])

(>defn button-with-icon
  "Build a button for the navbar, with icon, text and tooltip."
  ([icon-key tooltip-text button-text on-click-fn]
   [keyword? string? string? (? fn?) => :re-frame/component]
   [button-with-icon icon-key tooltip-text button-text on-click-fn nil])
  ([icon-key tooltip-text button-text on-click-fn attrs]
   [keyword? string? string? (? fn?) (? map?) => :re-frame/component]
   [tooltip/tooltip-button "bottom" tooltip-text
    [:<>
     [icon icon-key "m-auto d-block" {:size "lg"}]
     [:span.small.text-nowrap button-text]]
    on-click-fn
    attrs]))

;; -----------------------------------------------------------------------------
;; Graph stuff... Move to other ns TODO

(def graph-id "graph")

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

(defn show-notification
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
;; Admin Settings stuff. Refactor TODO

(defn- create-txt-download-handler
  "Receives the export apis answer and creates a download."
  [title [ok response]]
  (when ok
    (file-download/export-data
     (gstring/format "# %s\n%s" title (:string-representation response)))))

(defn- show-error
  [& _not-needed]
  (rf/dispatch [:ajax.error/as-notification (labels :error/export-failed)]))

(defn graph-download-as-png
  "Download the current graph as a png file."
  []
  (let [surrounding-div (gstring/format "#%s" graph-id)]
    [button-with-icon
     :file-download
     (labels :graph.download/as-png)
     (labels :discussion.navbar/download)
     #(let [canvas (.querySelector js/document (gstring/format "%s div canvas" surrounding-div))
            anchor (.createElement js/document "a")]
        (oset! anchor [:href] (.toDataURL canvas "image/png"))
        (oset! anchor [:download] "graph.png")
        (.click anchor))
     {:id :graph-export}]))

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

(defn txt-export
  "Request a txt-export of the discussion."
  []
  (when-let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    (let [title @(rf/subscribe [:schnaq/title])]
      [button-with-icon
       :file-download
       (labels :schnaq.export/as-text)
       (labels :discussion.navbar/download)
       #(txt-export-request share-hash title)])))

;; -----------------------------------------------------------------------------

(defn- schnaq-logo []
  [:<>
   [:img.schnaq-brand-logo.align-middle.me-2.d-md-none.d-none.d-xxl-block
    {:src (img-path :logo-white) :alt "schnaq logo"
     :style {:max-height "100%" :max-width "100%" :object-fit "contain"}}]
   [:img.schnaq-brand-logo.align-middle.me-2.d-xxl-none
    {:src (img-path :schnaqqifant/white) :alt "schnaq logo"
     :style {:max-height "100%" :max-width "100%" :object-fit "contain"}}]])

(defn navbar-title
  "Brand logo and title with dynamic resizing."
  ([]
   [navbar-title true])
  ([clickable-title?]
   (let [title @(rf/subscribe [:page/title])]
     [:div.d-flex.align-items-center.flex-row.me-2.bg-white
      [:a.schnaq-logo-container.d-flex.h-100 (when clickable-title? {:href (navigation/href :routes.schnaqs/personal)})
       [schnaq-logo]]
      [:> NavbarText {:className "text-dark"}
       [:h1.h6.text-wrap (toolbelt/truncate-to-n-chars title 50)]]
      [:div.h-100.d-none.d-md-block.p-2
       [common-components/theme-logo {:style {:max-height "100%"}}]]])))

;; -----------------------------------------------------------------------------

(defn separated-button
  "TODO Move this"
  ([button-content]
   [separated-button button-content {}])
  ([button-content attributes]
   [separated-button button-content attributes nil])
  ([button-content attributes dropdown-content]
   [:<>
    [:button.btn.discussion-navbar-button.text-decoration-none
     (merge
      {:type "button"}
      attributes)
     button-content]
    dropdown-content]))

;; -----------------------------------------------------------------------------

(rf/reg-event-db
 :schnaq.qa.new-question/pulse
 (fn [db [_ pulse]]
   (assoc-in db [:schnaq :qa :new-question :pulse] pulse)))

(rf/reg-sub
 :schnaq.qa.new-question/pulse?
 (fn [db _]
   (get-in db [:schnaq :qa :new-question :pulse] false)))

(defn language-toggle
  "Language Toggle dropdown button"
  []
  [:div.dropdown
   [language-toggle-with-tooltip false true {:class "text-dark btn"}]])

(defn user-button
  "Display the user settings button"
  ([]
   [user-button false])
  ([on-white-background?]
   [:div.d-flex.align-items-center
    [um/user-dropdown-button on-white-background?]]))
