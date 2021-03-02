(ns schnaq.interface.utils.time
  (:require ["date-fns" :as df]
            ["date-fns/locale" :as df-locale]
            [ghostwheel.core :refer [>defn]]))

(def ^:private select-locale
  {:de df-locale/de
   :en df-locale/en})

(>defn format-distance
  "Return a string containing a description when the timestamp occurred compared
  to the current time, e.g. '1 day ago'."
  [timestamp locale]
  [inst? keyword? :ret string?]
  (df/formatDistance timestamp (js/Date.)
                     #js {:addSuffix true
                          :locale (get select-locale locale df-locale/en)}))