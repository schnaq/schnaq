(ns build
  (:require [clojure.tools.build.api :as b]))

(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def uber-file "target/schnaq-standalone.jar")

(defn clean [_]
      (b/delete {:path "target"}))

(defn uber [_]
      (clean nil)
      (b/copy-dir {:src-dirs ["src" "resources"]
                   :target-dir class-dir})
      (b/compile-clj {:basis basis
                      :src-dirs ["src/main"]
                      :class-dir class-dir})
      (b/uber {:class-dir class-dir
               :uber-file uber-file
               :basis basis
               :main 'schnaq.api
               :exclude ["^META-INF/license/.*"]}))
