(ns schnaq.media
  (:require [ghostwheel.core :refer [>defn]]
            [ring.util.http-response :refer [ok]]
            [schnaq.validator :as validator]
            [taoensso.timbre :as log]))


(>defn set-preview-image
  "Set preview image"
  [{:keys [body-params]}]
  [:ring/request :ret :ring/response]
  (let [{:keys [share-hash edit-hash image-url]} body-params]
    (if (validator/valid-credentials? share-hash edit-hash)
      (do (log/debug (str "Set preview image" image-url))
          (ok (merge
                {:message "Image set"}
                {:result "ToDo"})))
      (validator/deny-access))))