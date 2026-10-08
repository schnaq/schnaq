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
    [:button.border-0.bg-transparent.p-0.text-reset
     (merge {:type "button"
             :title (labels :analytics.users/copy-button)
             :style {:font "inherit"}
             :on-click (fn []
                         (clipboard/copy-to-clipboard! access-code)
                         (notify! (labels :schnaq.access-code.clipboard/header)
                                  (labels :schnaq.access-code.clipboard/body)
                                  :info
                                  false))}
            options)
     (subs padded-access-code 0 (/ code-length 2)) [:span.ps-3]
     (subs padded-access-code (/ code-length 2))]))

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
               :eyeRadius 5}])
  ([link size options]
   [:> QRCode (merge {:value link
                      :fgColor (colors :positive/default)
                      :bgColor (colors :white)
                      :logoImage (img-path :logo.square.schnaqqi/blue)
                      :ecLevel "Q"
                      :size size
                      :qrStyle "dots"
                      :eyeRadius 5}
                     options)]))
