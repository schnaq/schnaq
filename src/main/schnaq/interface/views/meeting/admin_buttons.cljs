(ns schnaq.interface.views.meeting.admin-buttons
  (:require [ajax.core :as ajax]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.file-download :as file-download]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.meeting.calendar-invite :as calendar-invite]))

(defn tooltip-button
  [tooltip-location tooltip content on-click-fn]
  (reagent/create-class
    {:component-did-mount
     (fn [comp] (js-wrap/tooltip (rdom/dom-node comp)))
     :component-will-unmount
     (fn [comp]
       (js-wrap/tooltip (rdom/dom-node comp) "disable")
       (js-wrap/tooltip (rdom/dom-node comp) "dispose"))
     :reagent-render
     (fn [] [:button.button-secondary-b-1.button-md.my-2.mx-3
             {:on-click on-click-fn
              :data-toggle "tooltip"
              :data-placement tooltip-location
              :title tooltip} content])}))

(defn admin-center
  "Button to access admin menu."
  [share-hash edit-hash]
  [tooltip-button "bottom"
   (labels :meeting/admin-center-tooltip)
   [:i {:class (str "m-auto fas " (fa :cog))}]
   #(rf/dispatch [:navigation/navigate
                  :routes.meeting/admin-center
                  {:share-hash share-hash :edit-hash edit-hash}])])

(defn- create-txt-download-handler
  "Receives the export apis answer and creates a download."
  [title [ok response]]
  (when ok
    (file-download/export-data
      (gstring/format "# %s\n%s" title (:string-representation response)))))

(defn- show-error
  [& _not-needed]
  (rf/dispatch [:ajax.error/as-notification (labels :error/export-failed)]))

(defn txt-export
  "Request a txt-export of the discussion."
  [share-hash title]
  (let [request-fn #(ajax/ajax-request {:method :get
                                        :uri (str (:rest-backend config) "/export/txt")
                                        :format (ajax/transit-request-format)
                                        :params {:share-hash share-hash}
                                        :response-format (ajax/transit-response-format)
                                        :handler (partial create-txt-download-handler title)
                                        :error-handler show-error})]
    (when share-hash
      [tooltip-button "bottom" (labels :meeting/admin-center-export)
       [:i {:class (str "fas " (fa :file-download))}]
       #(request-fn)])))

(defn edit
  "Button to enter edit-mode."
  [share-hash edit-hash]
  [tooltip-button "bottom"
   (labels :meetings/edit-schnaq-button)
   [:i {:class (str "m-auto fas " (fa :eraser))}]
   #(rf/dispatch [:navigation/navigate
                  :routes.meeting/edit
                  {:share-hash share-hash :edit-hash edit-hash}])])

(defn calendar-invite
  "Button for calendar invitations."
  []
  [tooltip-button "bottom"
   (labels :meetings/share-calendar-invite)
   [:i {:class (str "m-auto fas " (fa :calendar))}]
   #(rf/dispatch [:modal {:show? true :large? false
                          :child [calendar-invite/modal]}])])
