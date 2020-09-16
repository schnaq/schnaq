(ns schnaq.interface.views.text-editor.view
  (:require ["easymde" :as mde]
            [reagent.core :as reagent]
            [schnaq.interface.text.display-data :as data]))


(defn view
  "Mark Up Text Editor View"
  []
  (let [id "text-area"]
    (reagent/create-class
      {:display-name "carousel-component"
       :reagent-render
       (fn [] [:textarea {:id id}])
       :component-did-mount
       (fn [_comp]
         (let [_newMDE (mde.
                        (clj->js {:element (js/document.getElementById id)
                                  :initialValue (data/labels :meeting-form-desc-placeholder)}))]))})))