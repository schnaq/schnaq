(ns schnaq.interface.views.pages
  "Defining page-layouts."
  (:require [ghostwheel.core :refer [>defn]]
            [schnaq.interface.views.base :as base]
            [cljs.spec.alpha :as s]))

(s/def :page/heading string?)
(s/def :page/subheading (s/? string?))
(s/def ::page
  (s/keys :req [:page/heading]
          :opt [:page/subheading]))

(>defn with-nav-and-header
  "Default page with header and curly wave."
  [{:page/keys [heading subheading]} & body]
  [::navigation (s/+ vector?) :ret vector?]
  [:<>
   [base/nav-header]
   [base/header heading subheading]
   body])
