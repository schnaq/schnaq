(ns schnaq.interface.views.navbar.elements
  (:require [ajax.core :as ajax]
            [com.fulcrologic.guardrails.core :refer [>defn >defn- ?]]
            [goog.string :as gstring]
            [oops.core :refer [oset!]]
            [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.components.colors :refer [colors]]
            [schnaq.interface.components.common :as common-components]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.file-download :as file-download]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.navbar.user-management :as um]))

(defn language-dropdown
  "Dropdown for bootstrap navbar to display the allowed languages."
  ([]
   [language-dropdown true {}])
  ([side-by-side? options]
   (let [icon-classes (if side-by-side? "" "d-block mx-auto")]
     [:<>
      [:button#schnaq-language-dropdown.btn.btn-link.nav-link.dropdown-toggle
       (merge
        {:role "button" :data-bs-toggle "dropdown"
         :aria-haspopup "true" :aria-expanded "false"}
        options)
       [icon :language icon-classes {:size "lg"}]
       [:span.small " " @(rf/subscribe [:current-language])]]
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
  [side-by-side? options]
  [tooltip/text
   (labels :nav.buttons/language-toggle)
   [:span [language-dropdown side-by-side? options]]])

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

(defn open-settings
  "Open Settings for the graph."
  []
  [button-with-icon
   :sliders-h
   (labels :graph.settings/title)
   (labels :discussion.navbar/settings)
   show-notification
   {:id :graph-settings}])

;; -----------------------------------------------------------------------------
;; Admin Settings stuff. Refactor TODO

(defn moderator-center
  "Button to access moderator panel."
  []
  [button-with-icon
   :sliders-h
   (labels :schnaq.admin/tooltip)
   (labels :discussion.navbar/settings)
   #(rf/dispatch [:navigation/navigate :routes.schnaq/moderation-center
                  {:share-hash @(rf/subscribe [:schnaq/share-hash])}])])

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
  [surrounding-div]
  [button-with-icon
   :file-download
   (labels :graph.download/as-png)
   (labels :discussion.navbar/download)
   #(let [canvas (.querySelector js/document (gstring/format "%s div canvas" surrounding-div))
          anchor (.createElement js/document "a")]
      (oset! anchor [:href] (.toDataURL canvas "image/png"))
      (oset! anchor [:download] "graph.png")
      (.click anchor))
   {:id :graph-export}])

(defn txt-export
  "Request a txt-export of the discussion."
  [share-hash title]
  (let [request-fn #(ajax/ajax-request
                     {:method :get
                      :uri (str shared-config/api-url "/export/argdown")
                      :format (ajax/transit-request-format)
                      :params {:share-hash share-hash}
                      :response-format (ajax/transit-response-format)
                      :handler (partial create-txt-download-handler title)
                      :error-handler show-error})]
    (when share-hash
      [button-with-icon
       :file-download
       (labels :schnaq.export/as-text)
       (labels :discussion.navbar/download)
       #(request-fn)])))

;; -----------------------------------------------------------------------------

(defn- clickable-title
  ([]
   [clickable-title "text-dark"])
  ([title-class]
   (let [{:discussion/keys [title share-hash]} @(rf/subscribe [:schnaq/selected])]
     [:a.link-unstyled
      {:href (navigation/href :routes.schnaq/start {:share-hash share-hash})}
      [:h1.h6.d-none.d-md-block.text-wrap {:class title-class} (toolbelt/truncate-to-n-chars title 64)]
      [:div.d-md-none {:class title-class} (toolbelt/truncate-to-n-chars title 32)]])))

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
  ([title]
   [navbar-title title true])
  ([title clickable-title?]
   [:div.d-flex.align-items-center.flex-row.schnaq-navbar-title.me-2.bg-white
    [:a.schnaq-logo-container.d-flex.h-100 (when clickable-title? {:href (navigation/href :routes.schnaqs/personal)})
     [schnaq-logo]]
    [:div.mx-0.mx-md-4.text-wrap title]
    [:div.h-100.d-none.d-md-block.p-2
     [common-components/theme-logo {:style {:max-height "100%"}}]]]))

(defn navbar-qanda-title []
  [:div.d-flex.align-items-center.flex-row.schnaq-navbar-title.me-2
   [:a.p-3.d-flex.h-100 {:href (toolbelt/current-overview-link)}
    [schnaq-logo]]
   [:div.mx-1.mx-md-5.px-md-5.pt-2
    [clickable-title "text-white"]]
   [:div.d-none.d-md-inline
    [common-components/theme-logo {:style {:max-width "150px"}}]]])

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

(>defn- discussion-button-builder
  "Build buttons in the discussion navigation."
  [label icon href]
  [keyword? keyword? (? string?) :ret vector?]
  [:a.dropdown-item {:href href}
   [:div.text-center
    [:img.navbar-view-toggle
     {:src (img-path icon)
      :alt "graph icon"}]
    [:p.small.m-0.text-nowrap (labels label)]]])

(defn graph-button
  "Rounded square button to navigate to the graph view"
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [discussion-button-builder
     :graph.button/text :icon-graph-dark
     (navigation/href :routes/graph-view {:share-hash share-hash})]))

(defn summary-button
  "Button to navigate to the summary view."
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [discussion-button-builder
     :summary.link.button/text :icon-summary-dark
     (navigation/href :routes.schnaq/dashboard {:share-hash share-hash})]))

(defn- standard-view-button
  "Button to navigate to the standard overview."
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [discussion-button-builder
     :discussion.button/text :icon-cards-dark
     (navigation/href :routes.schnaq/start {:share-hash share-hash})]))

(defn- qanda-view-button
  "Button to navigate to the Q&A view."
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [discussion-button-builder
     :qanda.button/text :icon-qanda-dark
     (navigation/href :routes.schnaq/qanda {:share-hash share-hash})]))

(defn dropdown-views
  "Displays a Dropdown menu button for the available views"
  ([]
   [dropdown-views :icon-views-dark ""])
  ([icon-id toggle-class]
   (let [dropdown-id "schnaq-views-dropdown"
         current-route @(rf/subscribe [:navigation/current-route-name])]
     [tooltip/text
      (labels :discussion.navbar/views)
      [:div.dropdown
       [separated-button
        [:div.dropdown-toggle
         {:class toggle-class}
         [:img.navbar-view-toggle.d-block
          {:src (img-path icon-id) :alt (labels :navbar.icon.views/alt-text)}]
         [:span.small
          (case current-route
            :routes.schnaq/start (labels :discussion.button/text)
            :routes.schnaq.select/statement (labels :discussion.button/text)
            :routes/graph-view (labels :graph.button/text)
            :routes.schnaq/dashboard (labels :summary.link.button/text)
            :routes.schnaq/qanda (labels :qanda.button/text)
            (labels :discussion.navbar/views))]]
        {:id dropdown-id :data-bs-toggle "dropdown"
         :aria-haspopup "true" :aria-expanded "false"}
        [:div.dropdown-menu.dropdown-menu-end {:aria-labelledby dropdown-id}
         [standard-view-button]
         [graph-button]
         [summary-button]
         [qanda-view-button]]]]])))

;; -----------------------------------------------------------------------------

(defn navbar-upgrade-button
  "Show an upgrade button in the navbar."
  [on-white-background?]
  (when-not @(rf/subscribe [:user/pro?])
    [button-with-icon
     :star
     (labels :pricing.upgrade-nudge/tooltip)
     (labels :pricing.upgrade-nudge/button)
     #(rf/dispatch [:navigation.redirect/follow {:redirect "https://schnaq.com/pricing"}])
     {:class (if on-white-background? "btn-outline-secondary" "btn-secondary")}]))

(defn navbar-settings
  "Either display schnaq or graph settings button"
  []
  (let [current-route @(rf/subscribe [:navigation/current-route-name])
        graph? (= current-route :routes/graph-view)]
    (if graph?
      [open-settings]
      (when @(rf/subscribe [:user/moderator?])
        [moderator-center]))))

(defn navbar-download
  "Download button for either text or graph"
  []
  (let [{:discussion/keys [title share-hash]} @(rf/subscribe [:schnaq/selected])
        current-route @(rf/subscribe [:navigation/current-route-name])
        graph? (= current-route :routes/graph-view)]
    (if graph?
      [graph-download-as-png (gstring/format "#%s" graph-id)]
      [txt-export share-hash title])))

(defn statement-counter
  "A counter showing all statements and pulsing live."
  []
  (let [number-of-questions @(rf/subscribe [:schnaq.selected/statement-number])
        share-hash @(rf/subscribe [:schnaq/share-hash])]
    [:a
     {:href (navigation/href :routes.schnaq/start {:share-hash share-hash})}
     [separated-button
      [:div.d-flex.text-white
       [motion/pulse-once [icon :comment/alt]
        [:schnaq.qa.new-question/pulse?]
        [:schnaq.qa.new-question/pulse false]
        (:white colors)
        (:secondary colors)]
       [:div.ms-2 number-of-questions]]]]))

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
   [language-toggle-with-tooltip false {:class "text-dark btn"}]])

(defn discussion-title
  "Display the schnaq title and info"
  []
  [navbar-title
   [clickable-title]])

(defn user-button
  "Display the user settings button"
  ([]
   [user-button false])
  ([on-white-background?]
   [:div.d-flex.align-items-center
    [um/user-dropdown-button on-white-background?]]))
