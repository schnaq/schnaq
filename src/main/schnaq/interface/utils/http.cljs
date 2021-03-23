(ns schnaq.interface.utils.http
  (:require [ajax.core :as ajax]
            [cljs.spec.alpha :as s]
            [ghostwheel.core :refer [>defn]]
            [schnaq.interface.auth :as auth]
            [schnaq.interface.config :refer [config]]))

(s/def ::http-methods #{:get :post :put :delete :patch})

(>defn xhrio-request
  "Returns an xhrio-request-fx for usage in re-frame."
  ([db method path on-success]
   [map? ::http-methods string? vector? :ret vector?]
   (xhrio-request db method path on-success {}))
  ([db method path on-success params]
   [map? ::http-methods string? vector? map? :ret vector?]
   (xhrio-request db method path on-success params [:ajax.error/to-console]))
  ([db method path on-success params on-failure]
   [map? ::http-methods string? vector? map? vector? :ret vector?]
   (let [path (if (.startsWith path "/") path (str "/" path))]
     [:http-xhrio {:method method
                   :uri (str (:rest-backend config) path)
                   :format (ajax/transit-request-format)
                   :params params
                   :headers (auth/authentication-header db)
                   :response-format (ajax/transit-response-format)
                   :on-success on-success
                   :on-failure on-failure}])))