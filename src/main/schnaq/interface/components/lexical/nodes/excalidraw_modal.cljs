(ns schnaq.interface.components.lexical.nodes.excalidraw-modal
  (:require ["@excalidraw/excalidraw" :refer [Excalidraw]]
            ["react" :refer [useEffect useLayoutEffect useRef useState]]
            ["react-dom" :refer [createPortal]]
            [oops.core :refer [ocall oget]]
            [reagent.core :as r]
            [schnaq.interface.translations :refer [labels]]))

(defn ExcalidrawModal [props]
  (let [{:keys [closeOnClickOutside? onSave onDelete initialElements shown?]} (js->clj props :keywordize-keys true)
        excalidraw-modal-ref (useRef nil)
        [discard-modal-open? discard-modal-open!] (useState false)
        [elements elements!] (useState initialElements)
        save-fn #(let [filtered-elements (ocall elements "filter" (fn [el] (not (oget el :isDeleted))))]
                   (if (pos? (count filtered-elements))
                     (onSave elements)
                      ;; else delete node if the scene is clear
                     (onDelete)))
        discard-fn (fn [event]
                     (.preventDefault event)
                     (discard-modal-open! true)
                     (onDelete))
        on-change-fn #(elements! %)
        ShowDiscardDialog [:div.excalidraw-discard-modal
                           [:button {:on-click (fn []
                                                 (discard-modal-open! false)
                                                 (onDelete))}
                            (labels :excalidraw/discard)]
                           [:button {:on-click #(discard-modal-open! false)}
                            (labels :excalidraw/cancel)]]]

    (useEffect #(when-let [current (oget excalidraw-modal-ref :current)]
                  (ocall current "focus")) #js [])

    (useEffect
     (fn []
       (let [modal-overlay-element (oget excalidraw-modal-ref [:?current :?parentElement])
             current (oget excalidraw-modal-ref :current)
             click-outside-handler (fn [^MouseEvent event]
                                     (let [target (oget event :target)]
                                       (when (and current
                                                  (not (ocall current "contains" target))
                                                  closeOnClickOutside?)
                                         (onDelete))))]
         (when (and current modal-overlay-element)
           (ocall modal-overlay-element "addEventListener" "click" click-outside-handler))
         (fn []
           (when modal-overlay-element
             (ocall modal-overlay-element "removeEventListener" "click" click-outside-handler)))))
     #js [closeOnClickOutside? onDelete])

    (useLayoutEffect
     #(let [current-modal-ref (oget excalidraw-modal-ref :current)
            on-key-down (fn [^KeyboardEvent event]
                          (when (= "Escape" (oget event :key))
                            (onDelete)))]
        (when current-modal-ref
          (ocall current-modal-ref "addEventListener" "keydown" on-key-down))
        (fn []
          (when current-modal-ref
            (ocall current-modal-ref "removeEventListener" "keydown" on-key-down))))
     #js [elements onDelete])

    (when shown?
      (createPortal
       (r/as-element
        [:section.excalidraw
         [:div.excalidraw-modal-overlay {:role :dialog}
          [:div.excalidraw-modal {:ref excalidraw-modal-ref
                                  :tabIndex -1}
           [:div.excalidraw-modal-row
            (when discard-modal-open? [ShowDiscardDialog])
            [:> Excalidraw {:onChange on-change-fn
                            :initialData {:appState {:isLoading false}
                                          :elements initialElements}
                            :UIOptions {:canvasActions {:loadScene false
                                                        :export false
                                                        :saveToActiveFile false
                                                        :saveAsImage false
                                                        :clearCanvas false
                                                        :changeViewBackgroundColor false}}}]
            [:div.excalidraw-modal-actions
             [:button {:on-click discard-fn}
              (labels :excalidraw/discard)]
             [:button {:on-click save-fn}
              (labels :excalidraw/save)]]]]]])
       (oget js/document :body)))))
