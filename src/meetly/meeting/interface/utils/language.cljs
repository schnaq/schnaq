(ns meetly.meeting.interface.utils.language
  (:require [oops.core :refer [oget]]))

(defn locale []
   (oget js/navigator :language))


