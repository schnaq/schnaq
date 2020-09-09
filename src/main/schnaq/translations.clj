(ns schnaq.translations)

(defn email-templates
  [identifier]
  (identifier
    {:invitation/title "Einladung zum schnaq"
     :invitation/body
     "Hallo,

Sie wurden eingeladen um an einem schnaq teilzunehmen.
Der Zugangslink lautet %s

Falls Sie mehr Infos zu schnaq möchten, besuchen Sie https://schnaq.com"}))