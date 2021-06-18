(ns schnaq.links
  #?(:clj  (:require [ghostwheel.core :refer [>defn]]
                     [schnaq.config :as config]
                     [schnaq.database.specs :as specs])
     :cljs (:require [ghostwheel.core :refer [>defn]]
                     [goog.string :as gstring]
                     [oops.core :refer [oget]]
                     [reitit.frontend.easy :as reitfe]
                     [schnaq.database.specs :as specs])))

(>defn get-share-link
  "Takes a share hash and returns a link to the schnaq."
  [share-hash]
  [string? :ret string?]
  #?(:clj  (format "%s/schnaq/%s/" config/frontend-url share-hash)
     :cljs (let [path (reitfe/href :routes.schnaq/start {:share-hash share-hash})
                 location (oget js/window :location)]
             (gstring/format "%s//%s%s" (oget location :protocol) (oget location :host) path))))

(>defn get-admin-center-link
  "Building a URL to the admin-center of a schnaq.."
  [share-hash edit-hash]
  [string? string? :ret string?]
  #?(:clj  (format "%s/schnaq/%s/manage/%s" config/frontend-url share-hash edit-hash)
     :cljs (let [path (reitfe/href :routes.schnaq/admin-center {:share-hash share-hash
                                                                :edit-hash edit-hash})
                 location (oget js/window :location)]
             (gstring/format "%s//%s%s" (oget location :protocol) (oget location :host) path))))

(>defn get-summary-link
  "Takes a share-hash and returns the link to the summary view."
  [share-hash]
  [:discussion/share-hash :ret string?]
  #?(:clj  (format "%s/schnaq/%s/summary" config/frontend-url share-hash)
     :cljs (let [path (reitfe/href :routes.schnaq/summary {:share-hash share-hash})
                 location (oget js/window :location)]
             (gstring/format "%s//%s%s" (oget location :protocol) (oget location :host) path))))

(>defn add-share-link
  "Takes a discussion and adds a share-link to the structure."
  [discussion]
  [::specs/discussion :ret ::specs/discussion]
  (assoc discussion :discussion/share-link
                    (get-share-link (:discussion/share-hash discussion))))