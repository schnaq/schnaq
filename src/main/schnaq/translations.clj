(ns schnaq.translations)

(def ^:private email-footer
  "Falls Sie mehr Infos zu schnaq möchten, besuchen Sie https://schnaq.com")

(defn email-templates
  [identifier]
  (identifier
   {:admin-center/title "Moderator link for the schnaq: %s"
    :admin-center/body
    (str "Hello,\n
You were invited to moderate the following schnaq: %s
Follow this URL to get into the admin center and become a moderator:\n\n%s\n\n" email-footer)
    :demo-request/title "Demo Anfrage!"
    :demo-request/body
    (str "Hallo,\n
%s hat eine Demo für ein Unternehmen angefragt.\n
E-Mail: %s\n
Firma (optional): %s\n
Telefon (optional): %s")}))
