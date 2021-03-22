(ns schnaq.interface.views.privacy
  "Page explaining our privacy and how we are storing data."
  (:require [cljs.pprint :refer [pprint]]
            [goog.string :as gstring]
            [hodgepodge.core :refer [local-storage clear!]]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.localstorage :as ls]
            [schnaq.interface.utils.rows :as rows]
            [schnaq.interface.views.notifications :refer [notify!]]
            [schnaq.interface.views.pages :as pages]))

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
   [:i {:class (str "m-auto fas fa-lg " (fa :shield))}]
   :privacy.made-in-germany])

(defn- personal-data-row []
  [rows/icon-left
   [:i {:class (str "m-auto fas fa-lg " (fa :user/lock))}]
   :privacy.personal-data])

(defn- localstorage-row
  "Explaining localstorage."
  []
  [rows/icon-right
   [:<>
    [:i#cookie-icon {:class (str "m-auto fas fa-lg " (fa :cookie/complete))}]
    [:div [:button.btn.btn-outline-primary
           {:on-click localstorage-explanation}
           (labels :privacy.localstorage/show-data)]]]
   :privacy.localstorage])

(defn- data-processing []
  [rows/icon-left
   [:i#data-proc {:class (str "m-auto fas fa-lg " (fa :server))}]
   :privacy.data-processing])

(defn- link-to-privacy []
  [:section.text-center.pb-5
   [:p.lead
    (gstring/format "%s " (labels :privacy.link-to-privacy/lead))
    [:a {:href "https://disqtec.com/datenschutz"}
     (labels :privacy.link-to-privacy/privacy)]
    "."]])

(defn- page []
  [pages/with-nav-and-header
   {:page/heading (labels :privacy/heading)
    :page/subheading (labels :privacy/subheading)}
   [:section.container
    [gdpr-row]
    [personal-data-row]
    [localstorage-row]
    [data-processing]
    [link-to-privacy]]])



;; -----------------------------------------------------------------------------

(defn view []
  [page])