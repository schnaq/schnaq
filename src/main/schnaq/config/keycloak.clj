(ns schnaq.config.keycloak
  (:require [buddy.core.keys :as keys]
            [clojure.string :as string]
            [keycloak.deployment :refer [keycloak-client client-conf]]
            [schnaq.config.shared :as shared-config]
            [taoensso.timbre :as log]))

(def server
  "Define your keycloak server's base url."
  (let [server shared-config/keycloak-host]
    (if (string/ends-with? server "/")
      server
      (format "%s/" server))))

(def realm
  "Specify the realm you are connecting to."
  (or (System/getenv "KEYCLOAK_REALM") "development"))

(def client-id
  "Specify the client on the realm you want to interact with."
  (or (System/getenv "KEYCLOAK_CLIENT_ID") "development"))

(def openid-endpoint
  "OpenID Endpoint to authenticate using oauth2."
  (format "%sauth/realms/%s/protocol/openid-connect" server realm))

(def ^:private backend-admin-id
  (or (System/getenv "KEYCLOAK_ADMIN_ID") "info@schnaq.com"))
(def ^:private backend-admin-secret
  (or (System/getenv "KEYCLOAK_ADMIN_SECRET") "***REMOVED***"))

(def kc-client
  "Client to interact with our keycloak instance."
  (-> (client-conf {:auth-server-url (format "%sauth/" server)
                    :realm realm
                    :client-id "admin-cli"})
      (keycloak-client backend-admin-id backend-admin-secret)))

;; -----------------------------------------------------------------------------

(def ^:private public-keys
  "These are the public keys from keycloak."
  {"development" "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoC1Z9HQAXVys5HeMJj0TJcG8h5OGcOvGf+Y4dZynJIMy6bcRoBeuaphOIjzP9qXeY88htuWQP/bnZkXPrPrhGCePDQaGfJbgCn/CtpuBlYYY/GxZ2/jbn+CCt88bK7SdZktLxEbozwHb1T5j318eoDdltSThgLh5HRyWJyyR75K4UN/EWtmQ3RXdcaDTnFazMT4ZVs6l+729BrtLEIBLkGr7Nref7iiu2Q3S4pae2zUTL8bbDeuxFte7E7nyLqp2bNnUDchi+DCXBmOrOHMchwTQVOxCm5h5KvJ9Un54P0ra7qksq8+2/gke+fu43xoLpj/c19miSOZdWB8ak0GpywIDAQAB"
   "schnaq" "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgkt6KamzIJjHV4TDZ5La1v6z5obxv1uDSBDPyQyZZW0rxVXwcR35GSztB3g4ET6UIQ7APlr+RgwbfEU1bSsU6MxvCp4Io6HGzxv7H8VNhBI5zLE6vEpTjbwR33M2GfgtgM/IeKN+d+bFO6te0K95GHr9D4BwI3jEoPwdUDVJeazAgs+kDA2px+r5ONu5O7le1jLla4Y9v8PObCFfZ+0GWlsx64S83SQGeNAZJhnPbO0edmaVziGL8Yv4igQSUYpJRmjhlvWTk+0/0WZDco0mc128Uhs6kTnb8W87AqXnKfG4FyROmjd9wGHM12ELpO+jjqgZwVp95cHGHEnEKlu8VwIDAQAB"})

(defn- choose-public-key
  "Choose a public key by the configured realm. If there is no such key, throws
  an exception."
  [realm]
  (if-let [key (get public-keys realm)]
    key
    (throw (ex-info (format "[Misconfiguration] No public key found for your realm \"%s\"" realm) {:realm realm}))))

(def keycloak-public-key
  "Build public key from server's response."
  (let [public-key (choose-public-key realm)]
    (log/info "[Keycloak] Successfully loaded public key:" public-key)
    (keys/str->public-key
     (format "-----BEGIN PUBLIC KEY-----\n%s\n-----END PUBLIC KEY-----"
             public-key))))
