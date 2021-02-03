(ns schnaq.interface.db)


(def default-db
  {:meetings {:all []}
   :controls {:username-input {:show? false}}
   :history {:full-context []}
   :votes {:up {}
           :down {}}
   :display-triggers {:meeting-link-success false}})