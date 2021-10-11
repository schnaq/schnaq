(ns schnaq.interface.views.qa.search
  (:require [goog.functions :as gfun]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]))

(def throttled-search
  (gfun/throttle
    #(rf/dispatch [:schnaq.qa/search (oget % [:?target :value])])
    500))
