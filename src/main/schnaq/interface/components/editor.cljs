(ns schnaq.interface.components.editor
  (:require [schnaq.interface.components.slate :as slate]
            [schnaq.interface.components.tiptap :as tiptap]))

;; -----------------------------------------------------------------------------

(defn- view []
  [:<>
   [tiptap/page]
   [slate/page]])

(defn page []
  [view])
