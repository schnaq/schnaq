(ns meetly.interface.config)

;; Third parameter is a default value
(goog-define api-url "http://localhost:3000")
(goog-define environment "development")
(goog-define build-hash "dev")

(def config
  {:rest-backend api-url
   :environment environment})