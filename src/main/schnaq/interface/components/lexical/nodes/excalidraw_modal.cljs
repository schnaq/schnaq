(ns schnaq.interface.components.lexical.nodes.excalidraw-modal
  (:require ["@excalidraw/excalidraw" :refer [Excalidraw]]
            ["react" :refer [useEffect useRef useState useLayoutEffect]]
            [oops.core :refer [ocall oget]]
            [reagent.core :as r]))

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
        discard-fn #(let [filtered-elements (ocall elements "filter" (fn [el] (not (oget el :isDeleted))))]
                      (if (zero? (count filtered-elements))
                        (onDelete)
                        (discard-modal-open! true)))
        on-change-fn #(elements! %)
        ShowDiscardDialog [:div {:class "ExcalidrawModal__discardModal"}
                           [:button {:on-click (fn []
                                                 (discard-modal-open! false)
                                                 (onDelete))}
                            "Discard"]
                           [:button {:on-click #(discard-modal-open! false)}
                            "Cancel"]]]

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
            on-key-down (fn [^KeyboardEvent _event] (onDelete))]
        (when current-modal-ref
          (ocall current-modal-ref "addEventListener" "keydown" on-key-down))
        (fn []
          (when current-modal-ref
            (ocall current-modal-ref "removeEventListener" "keydown" on-key-down))))
     #js [elements onDelete])

    (r/as-element
     (when shown?
       [:div {:class "ExcalidrawModal__overlay"
              :role "dialog"}
        [:div {:class "ExcalidrawModal__modal"
               :ref excalidraw-modal-ref
               :tabIndex -1}
         [:div {:class "ExcalidrawModal__row"}
          (when discard-modal-open? [ShowDiscardDialog])
          [:> Excalidraw {:onChange on-change-fn
                          :initialData {:appState {:isLoading false}
                                        :elements initialElements}}]
          [:div {:class "ExcalidrawModal__actions"}
           [:button {:class "action-button"
                     :on-click discard-fn}
            "Discard"]
           [:button {:class "action-button"
                     :on-click save-fn}
            "Save"]]]]]))))
