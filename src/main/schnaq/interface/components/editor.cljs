(ns schnaq.interface.components.editor
  (:require [schnaq.interface.components.lexical.editor :as lexical]))

;; -----------------------------------------------------------------------------

(defn- view []
  [:<>
   [lexical/page]])

(defn page []
  [view])
