(ns user
  (:require [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.server :as server]))

(defn cljs-repl
  "Connects to a given build-id. Defaults to `:app`."
  ([]
   (cljs-repl :app))
  ([build-id]
   (println "Welcome to schnaq-dev. Starting now shadow-cljs.")
   (server/start!)
   (shadow/watch build-id)
   (shadow/nrepl-select build-id)))
