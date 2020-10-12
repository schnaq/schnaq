(ns schnaq.interface.views.pages
  "Defining page-layouts."
  (:require [cljs.spec.alpha :as s]
            [ghostwheel.core :refer [>defn]]
            [schnaq.interface.views.base :as base]))

(s/def :page/heading string?)
(s/def :page/subheading (s/? string?))
(s/def ::page-headings
  (s/keys :req [:page/heading]
          :opt [:page/subheading]))

(>defn with-nav-and-header
  "Default page with header and curly wave."
  [{:page/keys [heading subheading]} body]
  [::page-headings (s/+ vector?) :ret vector?]
  [:<>
   [base/nav-header]
   [base/header heading subheading]
   body])
