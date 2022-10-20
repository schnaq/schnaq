(ns schnaq.interface.views.discussion.share
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.buttons :refer [button]]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.schnaq :as sc]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.clipboard :as clipboard]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.modal :as modal]
            [schnaq.interface.views.notifications :refer [notify!]]))

(defn- share-schnaq-via-access-code []
  (when @(rf/subscribe [:schnaq.selected/access-code])
    [:div.d-flex.flex-row.my-3
     [:p (labels :share-access-code/via)]
     [:div.flex.flex-fill.text-center.pe-5
      [:p.mb-0 (labels :share-access-code/title-1) " " [:span.font-monospace "https://schnaq.app"]]
      [:p (labels :share-access-code/title-2)]
      [:div.display-4.text-primary [sc/access-code]]]]))

(defn share-via-qr []
  (let [link @(rf/subscribe [:schnaq/share-link])]
    [:<>
     [:hr.my-4]
     [:div.d-flex.flex-row.flex-wrap.my-3
      [:p (labels :share-qr-code/via)]
      [:div.flex.flex-fill.text-center.pe-5
       [:div.d-md-none
        [sc/qr-code link 200]]
       [:div.d-none.d-md-block
        [sc/qr-code link]]]]]))

(defn- share-via-link []
  (let [link @(rf/subscribe [:schnaq/share-link])]
    [:<>
     [:hr.my-4]
     [:div.d-flex.flex-row.flex-wrap.flex-md-nowrap.my-3.pb-3.my-1
      [:div.d-flex.me-1.me-lg-5.align-self-center (labels :share-link/via)]
      [:div.d-flex.flex-row.flex-grow-1.flex-wrap.flex-md-nowrap
       [:input.form-control.my-1.bg-gray.text-white
        {:value link
         :readOnly true}]
       [:button.btn.btn-primary.text-nowrap.ms-md-3.my-1
        {:on-click (fn []
                     (clipboard/copy-to-clipboard! link)
                     (notify! (labels :schnaq/link-copied-heading)
                              (labels :schnaq/link-copied-success)
                              :info
                              false))}
        (labels :share-link/copy)]]]]))

(defn share-schnaq-modal
  "Create component to copy access link and access code."
  [component]
  [modal/modal {:size :lg}
   component
   (labels :sharing.modal/title)
   [:section
    [share-schnaq-via-access-code]
    [share-via-link]
    [share-via-qr]
    [:div.mx-auto.py-5.mt-3
     [common/schnaqqi-speech-bubble-blue
      "100px"
      (labels :sharing.modal/qanda-help)]]]])

(defn small-share-schnaq-button
  "Small share button with an icon in it."
  []
  [share-schnaq-modal
   (fn [props]
     [button [icon :share] nil "btn-outline-white ms-1" props])])
