(ns schnaq.links
  #?(:clj  (:require [com.fulcrologic.guardrails.core :refer [>defn]]
                     [schnaq.config :as config]
                     [schnaq.database.specs :as specs])
     :cljs (:require [com.fulcrologic.guardrails.core :refer [>defn]]
                     [goog.string :as gstring]
                     [oops.core :refer [oget]]
                     [reitit.frontend.easy :as reitfe]
                     [schnaq.database.specs :as specs])))

(>defn get-share-link
  "Takes a share hash and returns a link to the schnaq."
  [share-hash]
  [:discussion/share-hash :ret string?]
  #?(:clj  (format "%s/schnaq/%s" config/frontend-url share-hash)
     :cljs (let [path (reitfe/href :routes.schnaq/start {:share-hash share-hash})
                 location (oget js/window :location)]
             (gstring/format "%s//%s%s" (oget location :protocol) (oget location :host) path))))

(>defn get-link-to-statement
  [share-hash statement-id]
  [:discussion/share-hash :db/id :ret string?]
  (str (get-share-link share-hash) "/statement/" statement-id))

(>defn get-link-to-ask-interface
  "Return link to the ask-interface in the Q&A case."
  [share-hash]
  [:discussion/share-hash :ret string?]
  (str (get-share-link share-hash) "/ask"))

(>defn get-admin-link
  "Building a URL to the admin-center of a schnaq."
  [share-hash edit-hash]
  [:discussion/share-hash :discussion/edit-hash :ret string?]
  #?(:clj  (format "%s/schnaq/%s/manage/%s" config/frontend-url share-hash edit-hash)
     :cljs (let [path (reitfe/href :routes.schnaq/admin-center {:share-hash share-hash
                                                                :edit-hash edit-hash})
                 location (oget js/window :location)]
             (gstring/format "%s//%s%s" (oget location :protocol) (oget location :host) path))))

(>defn get-summary-link
  "Takes a share-hash and returns the link to the summary view."
  [share-hash]
  [:discussion/share-hash :ret string?]
  #?(:clj  (format "%s/schnaq/%s/dashboard" config/frontend-url share-hash)
     :cljs (let [path (reitfe/href :routes.schnaq/dashboard {:share-hash share-hash})
                 location (oget js/window :location)]
             (gstring/format "%s//%s%s" (oget location :protocol) (oget location :host) path))))

(>defn add-links-to-discussion
  "Takes a discussion and adds a share-link to the structure."
  [{:discussion/keys [share-hash edit-hash] :as discussion}]
  [::specs/discussion :ret ::specs/discussion]
  (assoc discussion
         :discussion/share-link (get-share-link share-hash)
         :discussion/admin-link (get-admin-link share-hash edit-hash)))

(>defn checkout-link
  "Get link to checkout page. This should be called after the login of a user."
  [price-id]
  [:stripe.price/id :ret string?]
  #?(:cljs (let [path (reitfe/href :routes.subscription.redirect/checkout {} {:price-id price-id})
                 location (oget js/window :location)]
             (gstring/format "%s//%s%s" (oget location :protocol) (oget location :host) path))
     :clj (throw (ex-info "Not implemented" {:price-id price-id}))))
