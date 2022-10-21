(ns schnaq.interface.views.navbar.elements
  (:require ["react-bootstrap/NavDropdown" :as NavDropdown]
            [ajax.core :as ajax]
            [com.fulcrologic.guardrails.core :refer [=> >defn]]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.components.icons :refer [stacked-icon]]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.file-download :as file-download]
            [schnaq.interface.utils.tooltip :as tooltip]))

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

(rf/reg-event-db
 :schnaq.qa.new-question/pulse
 (fn [db [_ pulse]]
   (assoc-in db [:schnaq :qa :new-question :pulse] pulse)))

(rf/reg-sub
 :schnaq.qa.new-question/pulse?
 (fn [db _]
   (get-in db [:schnaq :qa :new-question :pulse] false)))
