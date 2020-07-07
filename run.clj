#!/bin/env bb
(require '[babashka.classpath :refer [add-classpath]]
         '[clojure.tools.cli :refer [parse-opts]])

;; Adds current project to babashka's classpath
(add-classpath (:out (shell/sh "clojure" "-Spath")))
(require '[meetly.config :as config])

(def cli-options
  [["-h" "--help"]])

(defn datomic-install
  "Download datomic-pro, unzip it and put it into the project's root folder."
  []
  (let [datomic-version "datomic-pro-1.0.6165"
        address (format "https://s3.cs.hhu.de/dialogo/%s.zip" datomic-version)]
    (println (format "Downloading %s.zip. This might take a while..." datomic-version))
    (shell/sh "curl" "-O" address)
    (println "Finished downloading. Unpacking datomic...")
    (shell/sh "unzip" (format "%s.zip" datomic-version))
    (shell/sh "mv" datomic-version "datomic")))

(defn datomic-run
  "Uses the local datomic database and starts the in-memory driver.
  Configuration is read from meetly.config."
  []
  (let [{:keys [server-type access-key secret endpoint]} config/datomic
        [host port] (str/split endpoint #":")
        shell-command ["datomic/bin/run" "-m" (format "datomic.%s" (name server-type)) "-h" host "-p" port "-a" (format "%s,%s" access-key secret) "-d" (format "%s,datomic:mem://%s" config/db-name config/db-name)]]
    (println "Starting datomic database...")
    (println "Executing the following command:" (str/join " " shell-command))
    (println (apply shell/sh shell-command))))


;; -----------------------------------------------------------------------------

(defn usage
  "Print usage summary."
  [options-summary]
  (->> ["Managing dialog.core."
        ""
        "Usage: bb run.clj [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  datomic/install   Downloads, unzips and moves datomic database into a folder named \"datomic/\"."
        "  datomic/run       Start in-memory datomic instance. Uses configuration from dialog.discussion.config."
        ""
        "Please refer to the manual page for more information."]
       (str/join \newline)))


;; -----------------------------------------------------------------------------

(defn -main []
  (let [{:keys [arguments summary]} (parse-opts *command-line-args* cli-options)]
    (if (zero? (count arguments))
      (println (usage summary))
      (doseq [argument arguments]
        (case argument
          "datomic/install" (datomic-install)
          "datomic/run" (datomic-run)
          (println (usage summary)))))))

(-main)