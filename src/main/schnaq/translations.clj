(ns schnaq.translations)

(def ^:private email-footer
  "Falls Sie mehr Infos zu schnaq möchten, besuchen Sie https://schnaq.com")

(defn email-templates
  [identifier]
  (identifier
    {:invitation/title "Einladung zum schnaq: %s"
     :invitation/body
     (str "Hallo,\n
Du wurdest eingeladen um an einem schnaq teilzunehmen: %s
Der Zugangslink lautet: \n\n%s\n\n" email-footer)
     :admin-center/title "Admin-Center zu deinem schnaq: %s"
     :admin-center/body
     (str "Hallo,\n
Du erhälst mit dieser Mail die URL zum Admin-Center des schnaqs: %s
Rufe die folgende Adresse auf, um deinen schnaq zu administrieren:\n\n%s\n\n" email-footer)
     :demo-request/title "Demo Anfrage!"
     :demo-request/body
     (str "Hallo,\n
%s hat eine Demo für ein Unternehmen angefragt.\n
E-Mail: %s\n
Firma (optional): %s\n
Telefon (optional): %s")
     :welcome/title "Willkommen"
     :welcome/body
     (str
       "Schön, dass du da bist!"
       "\n"
       "Danke, dass du dich bei schnaq registriert hast! "
       "Wir wollen dir helfen, deinen modernen Arbeitsalltag so effizient wie möglich zu gestalten."
       "\n"
       "In einem schnaq sammelst du Wissen über ein Thema und es wird automatisch visualisiert. "
       "Probiere es direkt aus! Erstelle einen schnaq und diskutiere mit deinen Kolleg:innen!"
       "\n"
       "Du kannst vorher natürlich auch erst dein Profil individualisieren.")
     :lead-magnet/title "schnaq.com – Datenschutzkonform verteilt arbeiten Checkliste"
     :lead-magnet/body
     (str
       "Schön, dass du den ersten Schritt getan hast um flexibel und sicher arbeiten zu können!"
       "\n"
       "Hier ist dein persönlicher Downloadlink: $DOWNLOAD_LINK"
       "\n"
       "Willst du mehr zu dem Thema erfahren? Schaue öfter mal in unserem Blog vorbei: https://schnaq.com/blog/"
       "\n\n"
       "Liebe Grüße\nDein schnaq-Team")}))
