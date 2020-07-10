(ns meetly.meeting.interface.db)


(def default-db
  {:time (js/Date.)
   :time-color "#f88"
   :meetings ["Jour Fixé every day"]
   :current-page :home})