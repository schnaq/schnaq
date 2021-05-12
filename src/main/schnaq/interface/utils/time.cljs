(ns schnaq.interface.utils.time
  (:require ["date-fns" :as df]
            ["date-fns-tz" :as df-tz]
            ["date-fns/locale" :as df-locale]
            [ghostwheel.core :refer [>defn]]
            [schnaq.interface.config :as config]
            [schnaq.interface.utils.tooltip :as tooltip]))

(def ^:private select-locale
  {:de df-locale/de
   :en df-locale/en})

(>defn format-distance
  "Return a string containing a description when the timestamp occurred compared
  to the current time, e.g. '1 day ago'."
  [timestamp locale]
  [inst? keyword? :ret string?]
  (if timestamp
    (df/formatDistance timestamp (js/Date.)
                       #js {:addSuffix true
                            :locale (get select-locale locale df-locale/en)})
    ""))

(>defn formatted-with-timezone
  "Return formatted and timezoned string containing the time and date according
  to the pattern, e.g. '16:47 02.03.2021'."
  [timestamp]
  [inst? :ret string?]
  (if timestamp
    (df-tz/format (df-tz/utcToZonedTime timestamp (:timezone config/time-settings))
                  (:pattern config/time-settings)
                  #js {:timeZone (:timezone config/time-settings)})
    ""))

(>defn timestamp-with-tooltip
  "Print prettified string containing the timestamp with a tooltip."
  [timestamp locale]
  [inst? keyword? :ret vector?]
  [tooltip/inline-element :bottom (formatted-with-timezone timestamp)
   (format-distance timestamp locale)])