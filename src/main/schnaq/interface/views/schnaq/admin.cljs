(ns schnaq.interface.views.schnaq.admin
  (:require [ajax.core :as ajax]
            [goog.string :as gstring]
            [oops.core :refer [oset!]]
            [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.components.navbar :as navbar-components]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.file-download :as file-download]))

(defn moderator-center
  "Button to access moderator panel."
  []
  [navbar-components/button-with-icon
   :sliders-h
   (labels :schnaq.admin/tooltip)
   (labels :discussion.navbar/settings)
   #(rf/dispatch [:navigation/navigate :routes.schnaq/moderation-center
                  {:share-hash @(rf/subscribe [:schnaq/share-hash])}])])

(defn- create-txt-download-handler
  "Receives the export apis answer and creates a download."
  [title [ok response]]
  (when ok
    (file-download/export-data
     (gstring/format "# %s\n%s" title (:string-representation response)))))

(defn- show-error
  [& _not-needed]
  (rf/dispatch [:ajax.error/as-notification (labels :error/export-failed)]))

(defn graph-download-as-png
  "Download the current graph as a png file."
  [surrounding-div]
  [navbar-components/button-with-icon
   :file-download
   (labels :graph.download/as-png)
   (labels :discussion.navbar/download)
   #(let [canvas (.querySelector js/document (gstring/format "%s div canvas" surrounding-div))
          anchor (.createElement js/document "a")]
      (oset! anchor [:href] (.toDataURL canvas "image/png"))
      (oset! anchor [:download] "graph.png")
      (.click anchor))
   {:id :graph-export}])

(defn txt-export
  "Request a txt-export of the discussion."
  [share-hash title]
  (let [request-fn #(ajax/ajax-request
                     {:method :get
                      :uri (str shared-config/api-url "/export/argdown")
                      :format (ajax/transit-request-format)
                      :params {:share-hash share-hash}
                      :response-format (ajax/transit-response-format)
                      :handler (partial create-txt-download-handler title)
                      :error-handler show-error})]
    (when share-hash
      [navbar-components/button-with-icon
       :file-download
       (labels :schnaq.export/as-text)
       (labels :discussion.navbar/download)
       #(request-fn)])))
