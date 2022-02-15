(ns schnaq.interface.pages.privacy
  "Page explaining our privacy and how we are storing data."
  (:require [cljs.pprint :refer [pprint]]
            [goog.string :as gstring]
            [hodgepodge.core :refer [local-storage clear!]]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.utils.localstorage :as ls]
            [schnaq.interface.utils.rows :as rows]
            [schnaq.interface.views.notifications :refer [notify!]]
            [schnaq.interface.views.pages :as pages]))

(defn open-privacy-settings
  "Open privacy settings."
  []
  [:button.btn.btn-outline-primary {:on-click (js-wrap/show-js-klaro)}
   (labels :privacy/open-settings)])

(defn- localstorage-explanation []
  (notify!
   (labels :privacy.localstorage.notification/title)
   [:<>
    [:p (labels :privacy.localstorage.notification/body)]
    [:pre [:code
           (with-out-str (pprint (ls/localstorage->map)))]]
    [:button.btn.btn-sm.btn-outline-danger
     {:on-click #(when (js/confirm (labels :privacy.localstorage.notification/confirmation))
                   (clear! local-storage)
                   (.reload js/location))}
     (labels :privacy.localstorage.notification/delete-button)]]
   :info
   true))

(defn- gdpr-row []
  [rows/icon-right
   [icon :shield "m-auto" {:size "lg"}]
   :privacy.made-in-germany])

(defn- personal-data-row []
  [rows/icon-left
   [:<>
    [icon :user/lock "m-auto" {:size "lg"}]
    [:div [open-privacy-settings]]]
   :privacy.personal-data])

(defn- localstorage-row
  "Explaining localstorage."
  []
  [rows/icon-right
   [:<>
    [icon :cookie/complete "m-auto" {:size "lg"}]
    [:div [:button.btn.btn-outline-primary
           {:on-click localstorage-explanation}
           (labels :privacy.localstorage/show-data)]]]
   :privacy.localstorage])

(defn- data-processing-anonymous []
  [rows/icon-left
   [icon :user/ninja "m-auto" {:size "lg"}]
   :privacy.data-processing.anonymous])

(defn- data-processing-registered []
  [rows/icon-right
   [icon :user/plus "m-auto" {:size "lg"}]
   :privacy.data-processing.registered])

(defn- link-to-privacy []
  [:section.text-center.pb-5
   [:p.lead
    (gstring/format "%s " (labels :privacy.link-to-privacy/lead))
    [:a.btn.btn-lg.btn-link {:href (navigation/href :routes/privacy-extended)}
     (labels :privacy/note)]
    "."]])

(defn- page []
  [pages/with-nav-and-header
   {:page/heading (labels :privacy/heading)
    :page/subheading (labels :privacy/subheading)
    :page/vertical-header? true}
   [:section.container
    [gdpr-row]
    [personal-data-row]
    [localstorage-row]
    [data-processing-anonymous]
    [data-processing-registered]
    [link-to-privacy]]])

;; -----------------------------------------------------------------------------

(defn view []
  [page])
