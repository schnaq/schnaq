(ns schnaq.interface.components.editor
  (:require [schnaq.interface.components.lexical.editor :as lexical]
            [schnaq.interface.components.slate :as slate]
            [schnaq.interface.components.tiptap :as tiptap]))

;; -----------------------------------------------------------------------------

(defn- view []
  [:<>
   [tiptap/page]
   [:hr]
   [slate/page]
   [:hr]
   [lexical/page]])

(defn page []
  [view])
