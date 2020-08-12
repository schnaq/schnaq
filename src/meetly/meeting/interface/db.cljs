(ns meetly.meeting.interface.db)


(def default-db
  {:time (js/Date.)
   :time-color "green"
   :meetings [{:db/id "Fake"
               :meeting/title "Jour Fixé every day"
               :meeting/description "Whatever"
               :meeting/share-hash "test-123-456"
               :meeting/author "Wegi"}]
   :current-page :home
   :controls {:username-input {:show? false}}
   :agenda {:number-of-forms 1
            :all {}}
   :history {:full-context []}
   :votes {:up {}
           :down {}}
   :display-triggers {:meeting-link-success false}
   :edit-meeting {:meeting {}
                  :agendas []
                  :new-agendas []}})