(ns schnaq.links
  #?(:clj (:require [clojure.spec.alpha :as s]
                    [com.fulcrologic.guardrails.core :refer [=> >defn ?]]
                    [schnaq.config :as config]
                    [schnaq.database.specs :as specs])
     :cljs (:require [cljs.spec.alpha :as s]
                     [com.fulcrologic.guardrails.core :refer [>defn ? =>]]
                     [goog.string :refer [format]]
                     [oops.core :refer [oget]]
                     [reitit.frontend.easy :as reitfe]
                     [schnaq.database.specs :as specs])))

(s/def ::path (s/and string? #(.startsWith % "/")))

(>defn relative-to-absolute-url
  "Convert a relative url to an absolute url. Points to the currently configured
  frontend as a default."
  [path]
  [(? ::path) => (? string?)]
  (when path
    #?(:cljs (let [location (oget js/window :location)]
               (format "%s//%s%s" (oget location :protocol) (oget location :host) path))
       :clj (format "%s%s" config/frontend-url path))))

(>defn get-share-link
  "Takes a share hash and returns a link to the schnaq."
  [share-hash]
  [(? :discussion/share-hash) :ret (? string?)]
  #?(:clj (format "%s/schnaq/%s" config/frontend-url share-hash)
     :cljs (let [path (reitfe/href :routes.schnaq/start {:share-hash share-hash})]
             (relative-to-absolute-url path))))

(>defn get-link-to-statement
  "Generate a direct url to the statement."
  [share-hash statement-id]
  [(? :discussion/share-hash) (? :db/id) :ret (? string?)]
  (when (and share-hash statement-id)
    (format "%s/statement/%s" (get-share-link share-hash) statement-id)))

(>defn get-moderator-center-link
  "Building a URL to the moderator-center of a schnaq."
  [share-hash]
  [:discussion/share-hash :ret string?]
  #?(:clj (format "%s/schnaq/%s/manage" config/frontend-url share-hash)
     :cljs (let [path (reitfe/href :routes.schnaq/moderation-center {:share-hash share-hash})]
             (relative-to-absolute-url path))))

(>defn get-summary-link
  "Takes a share-hash and returns the link to the summary view."
  [share-hash]
  [:discussion/share-hash :ret string?]
  #?(:clj (format "%s/schnaq/%s/dashboard" config/frontend-url share-hash)
     :cljs (let [path (reitfe/href :routes.schnaq/dashboard {:share-hash share-hash})]
             (relative-to-absolute-url path))))

(>defn add-links-to-discussion
  "Takes a discussion and adds a share-link to the structure."
  [{:discussion/keys [share-hash] :as discussion}]
  [::specs/discussion :ret ::specs/discussion]
  (assoc discussion
         :discussion/share-link (get-share-link share-hash)
         :discussion/moderation-link (get-moderator-center-link share-hash)))
