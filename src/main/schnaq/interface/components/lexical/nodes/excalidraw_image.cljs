(ns schnaq.interface.components.lexical.nodes.excalidraw-image
  (:require [goog.string :refer [format]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]))

(defn ExcalidrawImage [props]
  (let [{:keys [imageContainerRef rootClassName]} props
        drawing @(rf/subscribe [:excalidraw.elements/svg])]
    (when drawing
      [:div {:ref imageContainerRef
             :className rootClassName
             :dangerouslySetInnerHTML
             {:__html drawing}}])))

(comment

  (def b64svg "PHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgdmlld0JveD0iMCAwIDU4MS4wODIwMzEyNSAxOTUuMTM2NzE4NzUiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGRpc3BsYXk9ImJsb2NrIj4KICA8IS0tIHN2Zy1zb3VyY2U6ZXhjYWxpZHJhdyAtLT4KICAKICA8ZGVmcz4KICAgIAogIDwvZGVmcz4KICA8cmVjdCB4PSIwIiB5PSIwIiB3aWR0aD0iNTgxLjA4MjAzMTI1IiBoZWlnaHQ9IjE5NS4xMzY3MTg3NSIgZmlsbD0iI2ZmZmZmZiI+PC9yZWN0PjxnIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoMTAgMTApIHJvdGF0ZSgwIDI4MC41NDEwMTU2MjUgODcuNTY4MzU5Mzc1KSI+PHBhdGggZD0iTTI4MS4zNSAtMC45MSBDMzM2LjggMTUuODgsIDM5My41NiAzNS4xMSwgNTYxLjQ4IDg3LjQzIE0yODEuNzIgLTAuNTUgQzM3NC4wNyAyNi43OSwgNDY3LjYxIDU3LjE4LCA1NjAuOTUgODcuMjggTTU2Mi40MSA4Ny4wNSBDNDk1LjQ4IDEwNi4yNywgNDM0LjQgMTI4LjE4LCAyNzkuNjYgMTc1Ljk3IE01NjAuODMgODcuOTcgQzQ4NS43MiAxMTIuODgsIDQwNy45NiAxMzcuNzMsIDI4MC41NyAxNzUuNzcgTTI4MS4xNCAxNzQuNDcgQzE3My40NCAxNDQuOTgsIDY4LjggMTEwLjExLCAwLjM0IDg5LjI0IE0yODAuOTcgMTc1LjQ0IEMxODUuMDcgMTQ3LjA3LCA4OC45NyAxMTYuNzUsIDAuMTggODcuNjcgTS0wLjExIDg4Ljk0IEMxMTIuMTYgNTEuODksIDIyMS45MyAxNi45OCwgMjgxLjQgMC40IE0wLjIzIDg4LjcxIEM4Ny41MyA2MS4yMywgMTc1LjgyIDMyLjc5LCAyODAuNTEgLTAuNDMiIHN0cm9rZT0iIzAwMDAwMCIgc3Ryb2tlLXdpZHRoPSIxIiBmaWxsPSJub25lIj48L3BhdGg+PC9nPjwvc3ZnPg==")
  (def share-hash "120900da-912e-422b-8ab2-19983cb63cd8")

  (def file-svg
    {:name "drawing.svg"
     #_#_:size 32
     :type "text/plain"
     :content (format "data:application/octet-stream;base64,%s" b64svg)})

  (rf/dispatch [:file/upload share-hash file-svg :schnaq/media [:ajax.error/as-notification] [:ajax.error/as-notification]])

  (.serializeToString (js/XMLSerializer.) ssvg)
  (.btoa js/window (oget ssvg :outerHTML))

  nil)
