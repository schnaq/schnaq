(ns meetly.meeting.interface.db)


(def default-db
  {:time (js/Date.)
   :time-color "green"
   :meetings [{:title "Jour Fixé every day"
               :description "Whatever"
               :share-hash "test-123-456"}]
   :current-page :home
   :controls {:username-input {:show? false}}
   :agenda {:number-of-forms 1
            :all {}}})