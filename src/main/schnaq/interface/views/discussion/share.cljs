(ns schnaq.interface.views.discussion.share
  (:require [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.schnaq :as sc]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.clipboard :as clipboard]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.modal :as modal]
            [schnaq.interface.views.notifications :refer [notify!]]
            [schnaq.links :as links]))

(defn- share-qanda-via-access-code []
  (when @(rf/subscribe [:schnaq.selected/access-code])
    [:<>
     [:div.d-flex.flex-row.mt-3.mb-5
      [:p (labels :share-access-code/via)]
      [:div.flex.flex-fill.text-center.pr-5
       [:p.mb-0 (labels :share-access-code/title-1) " " [:span.text-monospace "https://schnaq.app"]]
       [:p (labels :share-access-code/title-2)]
       [:div.display-4 [sc/access-code]]]]
     [:hr.my-4]]))

(defn- share-via-link [link]
  [:div.d-flex.flex-row.mt-3
   [:div.d-flex.mr-1.mr-lg-5.align-self-center (labels :share-link/via)]
   [:div.flex.flex-fill.lg-md-5
    [:div.input-group
     [:input.form-control
      {:value link
       :readOnly true}]
     [:div.input-group-append
      [:button.btn.btn-dark
       {:on-click (fn []
                    (clipboard/copy-to-clipboard! link)
                    (notify! (labels :schnaq/link-copied-heading)
                             (labels :schnaq/link-copied-success)
                             :info
                             false))}
       (labels :share-link/copy)]]]]])

(defn- share-qanda-modal
  "Modal showing sharing options."
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [modal/modal-template
     (labels :sharing.modal/title)
     [:section
      [share-qanda-via-access-code]
      [share-via-link (links/get-link-to-ask-interface share-hash)]
      [:div.mx-auto.py-5.mt-3
       [common/schnaqqi-speech-bubble-blue
        "100px"
        (labels :sharing.modal/qanda-help)]]]]))

(defn share-discussion-modal
  "Modal showing sharing options."
  []
  (let [share-hash @(rf/subscribe [:schnaq/share-hash])]
    [modal/modal-template
     (labels :sharing.modal/title)
     [:section
      [share-via-link (links/get-share-link share-hash)]
      [:div.mx-auto.py-5.mt-3
       [common/schnaqqi-speech-bubble-blue
        "100px"
        (labels :sharing.modal/schnaqqi-help)]]]]))

(defn- open-share-qanda
  "Open the share-schnaq dialog."
  []
  (rf/dispatch [:modal {:show? true
                        :large? true
                        :child [share-qanda-modal]}]))

(defn- open-share-discussion
  "Open the share-schnaq dialog."
  []
  (rf/dispatch [:modal {:show? true
                        :large? true
                        :child [share-discussion-modal]}]))

(defn share-discussion-button
  "Button to copy access link and notify the user."
  []
  [tooltip/tooltip-button "bottom" (labels :sharing/tooltip)
   [icon :share "m-auto"] open-share-discussion])

(defn share-qanda-button
  "Button to copy access link and acces code."
  []
  [tooltip/tooltip-button "bottom" (labels :sharing/tooltip)
   [icon :share "m-auto"] open-share-qanda])