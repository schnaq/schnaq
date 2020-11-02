(ns schnaq.interface.views.pages
  "Defining page-layouts."
  (:require [cljs.spec.alpha :as s]
            [ghostwheel.core :refer [>defn]]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.navbar :as navbar]))

(s/def :page/heading string?)
(s/def :page/subheading (s/? string?))
(s/def :page/more-for-heading vector?)
(s/def ::page-headings
  (s/keys :req [:page/heading]
          :opt [:page/subheading :page/more-for-heading]))

(>defn with-nav-and-header
  "Default page with header and curly wave."
  [{:page/keys [title heading subheading more-for-heading]} body]
  [::page-headings (s/+ vector?) :ret vector?]
  (common/set-website-title! (or title heading))
  [:<>
   [navbar/navbar]
   [base/header heading subheading more-for-heading]
   body])
