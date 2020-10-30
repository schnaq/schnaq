(ns schnaq.interface.config)

;; Third parameter is a default value
(goog-define api-url "http://localhost:3000")
(goog-define environment "development")
(goog-define build-hash "dev")
(goog-define demo-discussion-link "https://schnaq.com/meetings/3e4c1f44-721e-48e0-88f6-dc81528cf08f/")

(def config
  {:rest-backend api-url
   :environment environment})

(def graph-controversy-upper-bound 65)

(def periodic-update-time
  "Define how many times should the client query the server for live updates.
  Time must be in milliseconds."
  3000)