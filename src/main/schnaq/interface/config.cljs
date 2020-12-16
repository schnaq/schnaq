(ns schnaq.interface.config)

;; Third parameter is a default value
(goog-define api-url "http://localhost:3000")
(goog-define environment "development")
(goog-define build-hash "dev")
(goog-define demo-discussion-link "https://schnaq.com/meetings/ce682862-9ca2-49c8-af44-d6bf484fb87c/agenda/92358976736052/discussion/start")

(def deleted-statement-text "[deleted]")

(def config
  {:rest-backend api-url
   :environment environment})

(def user-language (atom :de))

(def graph-controversy-upper-bound 65)

(def periodic-update-time
  "Define how many times should the client query the server for live updates.
  Time must be in milliseconds."
  3000)
