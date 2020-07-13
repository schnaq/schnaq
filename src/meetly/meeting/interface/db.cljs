(ns meetly.meeting.interface.db)


(def default-db
  {:time (js/Date.)
   :time-color "green"
   :meetings ["Jour Fixé every day"]
   :current-page :home
   :agenda {:number-of-forms 1
            :all {}}})