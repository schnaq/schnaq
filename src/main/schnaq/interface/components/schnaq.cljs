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
     (subs padded-access-code 0 (/ code-length 2)) [:span.pl-3]
     (subs padded-access-code (/ code-length 2))]))

(defn discussion-options-button-group
  "Build a button-group with for the discussion-view."
  [[first-button & rest-buttons]]
  (let [{:keys [on-click label-key]} first-button]
    [:div.btn-group.btn-group-toggle.button-discussion-options.h-100 {:data-toggle "buttons"}
     [:label.form-label.btn.btn-sm.btn-outline-primary.active
      [:input {:type "radio" :autoComplete "off" :defaultChecked true
               :onClick on-click}]
      [:small.d-md-none (labels label-key)]
      [:div.d-none.d-md-block.mt-2 (labels label-key)]]
     (for [{:keys [on-click label-key]} rest-buttons]
       [:label.form-label.btn.btn-sm.btn-outline-primary.px-1.px-md-2 {:key (str "discussion-options-button-group-item-" label-key)}
        [:input {:type "radio" :autoComplete "off"
                 :onClick on-click}]
        [:small.d-md-none (labels label-key)]
        [:div.d-none.d-md-block.mt-2 (labels label-key)]])]))

(defn discussion-options-dropdown
  "Build a dropdown menu for the discussion view"
  [button-title dropdown-id [first-button & rest-buttons]]
  (let [{:keys [on-click label-key]} first-button
        dropdown-menu-id dropdown-id
        button-title button-title]
    [:div.dropdown.h-100
     [:button.btn.btn-sm.btn-primary.dropdown-toggle.h-100
      {:id dropdown-menu-id :type "button" :data-toggle "dropdown"
       :aria-haspopup "true" :aria-expanded "false"}
      button-title]
     [:div.dropdown-menu {:aria-labelledby dropdown-menu-id}
      [:button.dropdown-item
       {:on-click on-click}
       (labels label-key)]
      (for [{:keys [on-click label-key]} rest-buttons]
        [:button.dropdown-item
         {:key (str "discussion-options-dropdown-item-" label-key)
          :on-click on-click}
         (labels label-key)])]]))

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
