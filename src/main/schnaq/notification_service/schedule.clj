(ns schnaq.notification-service.schedule
  (:require [chime.core :as chime-core]
            [chime.core-async :refer [chime-ch]]
            [clojure.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [>defn-]]
            [schnaq.config.shared :as shared-config])
  (:import (java.time LocalTime LocalDate LocalDateTime ZonedDateTime ZoneId Period DayOfWeek)
           (java.time.temporal ChronoUnit TemporalAdjusters)))

(s/def :time/zoned-date-time (partial instance? ZonedDateTime))

(def ^:private start-next-morning
  "ZonedDateTime to indicate the start, today at 7 o'clock in UTC, which is 
   8 o'clock in Europe/Berlin in winter (9 during summer time)."
  (-> (LocalDateTime/of (LocalDate/now) (LocalTime/of 7 0))
      (.adjustInto (ZonedDateTime/now (ZoneId/of (:timezone shared-config/time-settings))))))

(defn- timestamp-next-monday
  "Takes a `ZonedDateTime` and returns the date of the next monday."
  [timestamp]
  [:time/zoned-date-time :ret :time/zoned-date-time]
  (let [timestamp-next-monday (-> timestamp (.with (TemporalAdjusters/next DayOfWeek/MONDAY)))
        days-left (.between ChronoUnit/DAYS timestamp timestamp-next-monday)]
    (.plusDays timestamp days-left)))

(>defn- create-schedule
  "Create an infinite collection with instances, re-occurring depending on the 
   `days`-parameter starting at `timestamp`."
  [timestamp days]
  [:time/zoned-date-time pos-int? :ret (s/coll-of inst?)]
  (chime-core/periodic-seq (.toInstant timestamp) (Period/ofDays days)))

;; -----------------------------------------------------------------------------

(def daily
  "Create a core.async channel containing a list of instances, repeating every
   day. Drops first element in list to prevent direct-sending mail."
  (atom
   (chime-ch (rest (create-schedule start-next-morning 1)))))

(def weekly
  "Same as `daily`, but for a weekly schedule."
  (atom
   (chime-ch (create-schedule (timestamp-next-monday start-next-morning) 7))))
