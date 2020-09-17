(ns schnaq.interface.views.text-editor.view
  (:require ["easymde" :as mde]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [schnaq.interface.text.display-data :as data]
            [re-frame.core :as rf]))

(defn view
  "Mark Up Text Editor View"
  [storage-key]
  (reagent/create-class
    {:display-name "mde-component"
     :reagent-render
     (fn [] [:textarea])
     :component-did-mount
     (fn [comp]
       (let [newMDE (mde.
                      (clj->js {:element (rdom/dom-node comp)
                                :initialValue (data/labels :meeting-form-desc-placeholder)}))]
         (.on (.-codemirror newMDE) "change"
              #(rf/dispatch [:mde/save-content storage-key (.value newMDE)]))))}))

(rf/reg-event-db
  :mde/save-content
  (fn [db [_ storage-key value]]
    (assoc-in db [:mde storage-key] value)))