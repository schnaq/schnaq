(ns meetly.meeting.interface.views.modals.subs
  (:require [re-frame.core :as rf])
  (:require-macros [reagent.ratom :refer [reaction]]))

(rf/reg-sub-raw
  :modal
  (fn [db _] (reaction (:modal @db))))