(ns schnaq.interface.components.editor
  (:require [schnaq.interface.components.tiptap :as tiptap]))

;; -----------------------------------------------------------------------------

(defn- view []
  [:<>
   [tiptap/page]])

(defn page []
  [view])
