(ns schnaq.interface.components.lexical.nodes.excalidraw-modal
  (:require ["@excalidraw/excalidraw" :refer [Excalidraw exportToSvg]]
            ["react" :refer [useEffect useLayoutEffect useRef useState]]
            [oops.core :refer [ocall oget]]
            [promesa.core :as p]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn ExcalidrawModal [props]
  (let [{:keys [closeOnClickOutside? onSave onDelete initialElements shown?]} (js->clj props :keywordize-keys true)
        excalidraw-modal-ref (useRef nil)
        [discard-modal-open? discard-modal-open!] (useState false)
        [elements elements!] (useState initialElements)
        save-fn #(let [filtered-elements (ocall elements "filter" (fn [el] (not (oget el :isDeleted))))]
                   (if (pos? (count filtered-elements))
                     (do
                       (rf/dispatch [:excalidraw.elements/convert elements])
                       (onSave elements))
                      ;; else delete node if the scene is clear
                     (onDelete)))
        discard-fn #(let [filtered-elements (ocall elements "filter" (fn [el] (not (oget el :isDeleted))))]
                      (rf/dispatch [:excalidraw.elements/dissoc])
                      (if (zero? (count filtered-elements))
                        (onDelete)
                        (discard-modal-open! true)))
        on-change-fn #(elements! %)
        ShowDiscardDialog [:div.excalidraw-discard-modal
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
            on-key-down (fn [^KeyboardEvent event]
                          (when (= "Escape" (oget event :key))
                            (onDelete)))]
        (when current-modal-ref
          (ocall current-modal-ref "addEventListener" "keydown" on-key-down))
        (fn []
          (when current-modal-ref
            (ocall current-modal-ref "removeEventListener" "keydown" on-key-down))))
     #js [elements onDelete])

    (r/as-element
     (when shown?
       [:div.excalidraw
        [:div {:class "excalidraw-overlay"
               :role :dialog}
         [:div {:class "excalidraw-modal"
                :ref excalidraw-modal-ref
                :tabIndex -1}
          [:div {:class "excalidraw-row"}
           (when discard-modal-open? [ShowDiscardDialog])
           [:> Excalidraw {:onChange on-change-fn
                           :initialData {:appState {:isLoading false}
                                         :elements initialElements}}]
           [:div {:class "excalidraw-actions"}
            [:button {:on-click discard-fn}
             "Discard"]
            [:button {:on-click save-fn}
             "Save"]]]]]]))))

;; -----------------------------------------------------------------------------

(defn- remove-external-excalidraw-fonts
  "Remove fonts from excalidraw.com, we import them manually from our servers."
  [svg]
  (let [style-tag (oget svg [:?firstElementChild :?firstElementChild])
        view-box (.getAttribute svg "viewBox")]
    (when view-box
      (let [view-box-dimensions (.split view-box " ")]
        (ocall svg "setAttribute" "width" (get view-box-dimensions 2))
        (ocall svg "setAttribute" "width" (get view-box-dimensions 3))))
    (when (and style-tag (= (oget style-tag :tagName) "style"))
      (ocall style-tag "remove"))))

(rf/reg-event-db
 :excalidraw.elements/svg
 (fn [db [_ svg]]
   (assoc-in db [:excalidraw :elements :svg] (oget svg :?outerHTML))))

(rf/reg-fx
 :excalidraw.elements/to-svg
 (fn [elements]
   (p/let [svg (exportToSvg #js {:elements elements :files nil})]
     (remove-external-excalidraw-fonts svg)
     (ocall svg "setAttribute" "width" "100%")
     (ocall svg "setAttribute" "height" "100%")
     (ocall svg "setAttribute" "display" "block")
     (rf/dispatch [:excalidraw.elements/svg svg]))))

(rf/reg-event-fx
 :excalidraw.elements/convert
 (fn [{:keys [db]} [_ elements]]
   {:db (assoc-in db [:excalidraw :elements :raw] elements)
    :fx [[:excalidraw.elements/to-svg elements]]}))

(rf/reg-event-db
 :excalidraw.elements/dissoc
 (fn [db]
   (update db :excalidraw dissoc :elements)))

(rf/reg-sub
 :excalidraw.elements/svg
 (fn [db]
   (get-in db [:excalidraw :elements :svg])))
