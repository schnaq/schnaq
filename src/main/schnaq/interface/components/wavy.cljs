(ns schnaq.interface.components.wavy)

(defn top-and-bottom
  "Adds a wave-bottom and a wave-top to the component.
  Valid classes: see `_startpage.scss`"
  [component class]
  (let [wave-bottom-class (str "wave-bottom-" (name class))]
    [:<>
     [:div {:class wave-bottom-class}]
     component
     [:div {:class wave-bottom-class
            :style {:transform "scale(-1)"
                    :margin-top "-5px"}}]]))
