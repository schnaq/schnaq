(ns schnaq.database.main
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :as walk]
            [com.fulcrologic.guardrails.core :refer [>defn ?]]
            [datomic.api :as d]
            [schnaq.api.dto-specs :as dto]
            [schnaq.config :as config]
            [schnaq.database.models :as models]
            [schnaq.database.specs :as specs]
            [schnaq.shared-toolbelt :as shared-tools]
            [schnaq.test-data :as test-data]
            [schnaq.toolbelt :as toolbelt]
            [taoensso.timbre :as log])
  (:import (java.time Instant)
           (java.time.temporal ChronoUnit)
           (java.util Date UUID)))

(def ^:private current-datomic-uri (atom config/datomic-uri))

(defn new-connection
  "Connects to the database and returns a connection."
  []
  (d/connect @current-datomic-uri))

(>defn connection-possible?
  "Try to establish a connection to the database. If not possible, print error
   message and stacktrace."
  []
  [:ret boolean?]
  (try
    (new-connection)
    (log/info "Database connection established.")
    true
    (catch Exception e
      (log/error (format "Database connection error! Please check credentials.\n%s" (.getMessage e)))
      false)))

(defn- convert-java-time-Instant-to-Date-walker
  "Converts all java.time.Instant instances from a data structure to a Date Instant"
  [data]
  (walk/postwalk
   #(if (instance? Instant %)
      (Date/from %)
      %)
   data))

(defn transact
  "Shorthand for transaction. Deref the result, if you need to further use it."
  [data]
  (->> data
       convert-java-time-Instant-to-Date-walker
       (d/transact (new-connection))))

(defn query
  "Shorthand to not type out the same first param every time"
  [query-vector & args]
  (apply d/q query-vector (d/db (new-connection)) args))

(defn init!
  "Initialization function, which does everything needed at a fresh app-install.
  Particularly transacts the database schema defined in models.clj.
  If no parameters are provided, the function reads its configuration from the
  config-namespace."
  ([]
   (init! config/datomic-uri))
  ([datomic-uri]
   (reset! current-datomic-uri datomic-uri)
   (d/create-database datomic-uri)
   (transact models/datomic-schema)))

(defn init-and-seed!
  "Initializing the datomic database and feeding it with test-data.
  If no parameters are provided, the function reads its configuration from the
  config-namespace."
  ([]
   (init-and-seed! config/datomic-uri))
  ([datomic-uri]
   (init-and-seed! datomic-uri test-data/schnaq-test-data))
  ([datomic-uri test-data]
   (init! datomic-uri)
   (transact test-data)))

;; ##### Input functions #####
(defn now [] (Date.))

(>defn days-ago
  "Timestamp some days ago."
  [days]
  [integer? :ret inst?]
  (-> (Instant/now) (.minus days ChronoUnit/DAYS) Date/from))

(>defn minutes-ago
  "Timestamp minutes ago."
  [minutes]
  [integer? :ret inst?]
  (-> (Instant/now) (.minus minutes ChronoUnit/MINUTES) Date/from))

(>defn seconds-ago
  "Timestamp seconds ago."
  [seconds]
  [integer? :ret inst?]
  (-> (Instant/now) (.minus seconds ChronoUnit/SECONDS) Date/from))

(defn fast-pull
  "Pulls any entity with star-syntax and current db."
  ([id]
   (fast-pull id '[*]))
  ([id pattern]
   (d/pull (d/db (new-connection)) pattern id))
  ([id pattern db]
   (d/pull db pattern id)))

(>defn clean-and-add-to-db!
  "Removes empty strings and nil values from map before transacting it to the
  database. Checks if the specification still matches. If true, transact the
  entity."
  [entity spec]
  [associative? keyword? :ret int?]
  (let [clean-entity (shared-tools/remove-nil-values-from-map entity)
        identifier (format "new-entity-%s"
                           (.toString (UUID/randomUUID)))]
    (when (s/valid? spec clean-entity)
      (get-in
       @(transact [(assoc clean-entity :db/id identifier)])
       [:tempids identifier]))))

(>defn transact-and-pull-temp
  "Syntactic sugar to transact and then synchronously pull an id from the resulting database.
  Takes a temp-id from the transaction instead of a live-id."
  [transaction-vector temp-id pattern]
  [vector? ::specs/non-blank-string vector? :ret map?]
  (let [tx @(transact transaction-vector)
        entity-id (get-in tx [:tempids temp-id])
        db-after (:db-after tx)]
    (fast-pull entity-id pattern db-after)))

(>defn increment-number
  "A generic transaction that atomically increments a number, using compare-and-swap.
  When there is no value, it is assumed to be 0.
  Prevents race-conditions and updating the same value multiple times. Tries at most 20 times until cas works.
  Returns the dereffed transaction."
  ([entity attribute]
   [:db/id keyword? :ret (? map?)]
   (increment-number entity attribute 1))
  ([entity attribute increment-value]
   [:db/id keyword? nat-int? :ret (? map?)]
   (toolbelt/try-times
    20
    (let [old-val (get (fast-pull entity [attribute]) attribute)
          new-val (if old-val (+ old-val increment-value) increment-value)]
      @(transact [[:db/cas entity attribute old-val new-val]])))))

(>defn decrement-number
  "A generic transaction that atomically decrements a number, using compare-and-swap.
  When there is no value, it is assumed to be 0.
  Prevents race-conditions and updating the same value multiple times. Tries at most 20 times until cas works.
  Returns the dereffed transaction."
  ([entity attribute]
   [:db/id keyword? :ret (? map?)]
   (decrement-number entity attribute Long/MIN_VALUE))
  ([entity attribute minimum-value]
   [:db/id keyword? int? :ret (? map?)]
   (toolbelt/try-times
    20
    (let [old-val (get (fast-pull entity [attribute]) attribute)
          new-val (max (if old-val (dec old-val) -1) minimum-value)]
      @(transact [[:db/cas entity attribute old-val new-val]])))))

;; -----------------------------------------------------------------------------
;; Feedback functions

(>defn add-feedback!
  "Adds a feedback to the database. Returns the id of the newly added feedback."
  [feedback]
  [::dto/feedback :ret :db/id]
  (clean-and-add-to-db! (assoc feedback :feedback/created-at (now)) ::specs/feedback))

(defn all-feedbacks
  "Return complete feedbacks from database, sorted by descending timestamp."
  []
  (->> (query
        '[:find [(pull ?feedback [*]) ...]
          :where [?feedback :feedback/description _ ?tx]])
       (sort-by :feedback/created-at toolbelt/ascending)))

;; -----------------------------------------------------------------------------

(>defn set-activation-focus
  "Set the focus on the current entity. Takes a db/id or a vector to reference
  the id (e.g. `[:discussion/share-hash \"share-hash\"]`)"
  [discussion-ref entity-ref]
  [(s/or :id :db/id :vector vector?) (s/or :id :db/id :vector vector?) => future?]
  (transact [[:db/add discussion-ref :discussion/activation-focus entity-ref]]))
