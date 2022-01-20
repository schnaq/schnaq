(ns schnaq.interface.components.wavy)

(defn top-and-bottom
  "Adds a wave-bottom and a wave-top to the component.
  Valid classes: see `_startpage.scss`"
  [class component wave-class]
  (let [str-class (name class)
        str-wave-class (if wave-class (name wave-class) str-class)
        wave-bottom-class (str "wave-bottom-" str-wave-class)]
    [:<>
     [:div {:class wave-bottom-class}]
     [:div {:class (str "py-1 bg-" str-class)} component]
     [:div {:class wave-bottom-class
            :style {:transform "scale(-1)"
                    :margin-top "-1px"}}]]))
