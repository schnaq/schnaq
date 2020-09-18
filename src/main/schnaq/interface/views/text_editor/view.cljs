(ns schnaq.interface.views.text-editor.view
  (:require ["easymde" :as mde]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [schnaq.interface.text.display-data :as data]
            [re-frame.core :as rf]))

(defn view
  "Mark Up Text Editor View"
  ([on-change-function]
   (view on-change-function "300px"))
  ([on-change-function min-height]
   (reagent/create-class
     {:display-name "mde-component"
      :reagent-render
      (fn [] [:textarea])
      :component-did-mount
      (fn [comp]
        (let [newMDE (mde.
                       (clj->js {:element (rdom/dom-node comp)
                                 :minHeight min-height
                                 :initialValue (data/labels :meeting-form-desc-placeholder)}))]
          (.on (.-codemirror newMDE) "change"
               #(on-change-function (.value newMDE)))))})))

(defn view-store-on-change
  "Mark Up Editor View which automatically stores its content in the local db.
  The value can be retrieved via subscribing to ':mde/load-content'"
  [storage-key]
  (view (fn [value]
           (rf/dispatch [:mde/save-content storage-key value]))))

(rf/reg-event-db
  :mde/save-content
  (fn [db [_ storage-key value]]
    (assoc-in db [:mde storage-key] value)))

(rf/reg-sub
  :mde/load-content
  (fn [db [_ storage-key]]
    (get-in db [:mde storage-key])))