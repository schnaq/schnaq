(ns schnaq.keycloak
  (:require [keycloak.deployment :refer [keycloak-client client-conf]]
            [schnaq.config.keycloak :as kc-config]))

(def kc-client
  (-> (client-conf {:auth-server-url (format "%sauth/" kc-config/server)
                    :realm kc-config/realm
                    :client-id "admin-cli"})
      (keycloak-client kc-config/backend-admin-id kc-config/backend-admin-secret)))
