(ns schnaq.interface.components.schnaq
  (:require ["react-qrcode-logo" :refer [QRCode]]
            [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.components.colors :refer [colors]]
            [schnaq.interface.components.icons :refer [icon]]
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
  (let [{:keys [on-click icon-key label-key]} first-button]
    [:div.btn-group.btn-group-toggle.button-discussion-options {:data-toggle "buttons"}
     [:label.btn.btn-outline-primary.active
      [:input {:type "radio" :autoComplete "off" :defaultChecked true
               :onClick on-click}]
      [icon icon-key]
      [:div.small (labels label-key)]]
     (for [{:keys [on-click icon-key label-key]} rest-buttons]
       [:label.btn.btn-outline-primary {:key (str "discussion-options-button-group-item-" label-key)}
        [:input {:type "radio" :autoComplete "off"
                 :onClick on-click}]
        [icon icon-key]
        [:div.small (labels label-key)]])]))

(defn qr-code [link]
  [:> QRCode {:value link
              :fgColor (colors :positive/default)
              :bgColor (colors :white)
              :logoImage (img-path :schnaqqifant/qr)
              :ecLevel "Q"
              :size 300
              :qrStyle "dots"
              :eyeRadius 5}])
