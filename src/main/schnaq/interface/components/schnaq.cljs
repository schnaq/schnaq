(ns schnaq.interface.components.schnaq
  (:require ["react-qrcode-logo" :refer [QRCode]]
            [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.components.colors :refer [colors]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.clipboard :as clipboard]
            [schnaq.interface.views.notifications :refer [notify!]]))

(defn access-code
  "Component to add leading zeros and a padding between access code blocks."
  [options]
  (let [access-code @(rf/subscribe [:schnaq.selected/access-code])
        code-length shared-config/access-code-length
        padded-access-code (.padStart (str access-code) code-length "0")]
    [:span.clickable
     (merge {:on-click (fn []
                         (clipboard/copy-to-clipboard! access-code)
                         (notify! (labels :schnaq.access-code.clipboard/header)
                                  (labels :schnaq.access-code.clipboard/body)
                                  :info
                                  false))}
            options)
     (subs padded-access-code 0 (/ code-length 2)) [:span.ps-3]
     (subs padded-access-code (/ code-length 2))]))

(defn schnaq-statement-filter-button-group
  "Build a button-group to filter the statements in a schnaq."
  [[first-button & rest-buttons]]
  (let [{:keys [on-click label-key]} first-button
        active-filters? @(rf/subscribe [:filters/active?])]
    [:div.btn-group.button-discussion-options.h-100
     [:input.btn-check {:id label-key :name :filter-discussion-options
                        :type "radio" :autoComplete "off"
                        :onClick on-click}]
     (println active-filters?)
     [:label.btn.btn-sm.btn-outline-primary
      (cond-> {:for label-key}
        (not active-filters?) (assoc :class "active"))
      [:small.d-md-none (labels label-key)]
      [:div.d-none.d-md-block.mt-2 (labels label-key)]]
     (for [{:keys [on-click label-key]} rest-buttons]
       [:<>
        {:key (str "discussion-options-button-group-item-" label-key)}
        [:input.btn-check {:id label-key :type "radio" :autoComplete "off"
                           :onClick on-click :name :filter-discussion-options}]
        [:label.btn.btn-sm.btn-outline-primary.px-1.px-md-2
         {:for label-key}
         [:small.d-md-none (labels label-key)]
         [:div.d-none.d-md-block.mt-2 (labels label-key)]]])]))

(defn qr-code
  ([link]
   [qr-code link 300])
  ([link size]
   [:> QRCode {:value link
               :fgColor (colors :positive/default)
               :bgColor (colors :white)
               :logoImage (img-path :logo.square.schnaqqi/blue)
               :ecLevel "Q"
               :size size
               :qrStyle "dots"
               :eyeRadius 5}]))
