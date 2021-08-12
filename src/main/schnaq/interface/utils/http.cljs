(ns schnaq.interface.utils.http
  (:require [ajax.core :as ajax]
            [cljs.spec.alpha :as s]
            [ghostwheel.core :refer [>defn]]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.auth :as auth]))

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
   (let [path (if (.startsWith path "/") path (str "/" path))
         csrf-token (get-in db [:internals :csrf-token])
         headers (cond-> (auth/authentication-header db)
                         csrf-token (assoc :X-CSRF-Token csrf-token))]
     (println headers)
     [:http-xhrio {:method method
                   :uri (str shared-config/api-url path)
                   :format (ajax/transit-request-format)
                   :params params
                   :headers headers
                   :response-format (ajax/transit-response-format)
                   :on-success on-success
                   :on-failure on-failure}])))