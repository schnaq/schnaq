(ns schnaq.translations)

(def ^:private email-footer
  "Falls Sie mehr Infos zu schnaq möchten, besuchen Sie https://schnaq.com")

(defn email-templates
  [identifier]
  (identifier
    {:invitation/title "Einladung zum schnaq"
     :invitation/body
     (str "Hallo,\n
Sie wurden eingeladen um an einem schnaq teilzunehmen.
Der Zugangslink lautet: \n\n%s\n\n" email-footer)
     :admin-center/title "Admin-Center zu Ihrem schnaq"
     :admin-center/body
     (str "Hallo,\n
Sie erhalten mit dieser Mail Ihren URL zum Admin-Center Ihres schnaqs. Rufen
Sie folgende Adresse auf, um Ihren schnaq zu administrieren:\n\n%s\n\n" email-footer)}))