(ns schnaq.interface.components.lexical.plugins.register-editor
  (:require ["@lexical/react/LexicalComposerContext" :refer [useLexicalComposerContext]]
            [re-frame.core :as rf]))

(defn RegisterEditorPlugin
  "Register an editor to access the id of the editor in effects."
  [{:keys [id]}]
  (let [[editor] (useLexicalComposerContext)]
    (rf/dispatch [:editor/register id editor])
    nil))
