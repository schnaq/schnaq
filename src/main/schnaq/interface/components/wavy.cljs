(ns schnaq.interface.components.wavy)

(defn top-and-bottom
  "Adds a wave-bottom and a wave-top to the component.
  Valid classes: see `_startpage.scss`"
  [class component]
  (let [str-class (name class)
        wave-bottom-class (str "wave-bottom-" str-class)]
    [:<>
     [:div {:class wave-bottom-class}]
     [:div {:class (str "bg-" str-class)} component]
     [:div {:class wave-bottom-class
            :style {:transform "scale(-1)"
                    :margin-top "-5px"}}]]))
