(ns schnaq.interface.utils.tooltip
  (:require ["@tippyjs/react" :default Tippy]
            [reagent.core :as reagent]))

(defn return-focus
  "Tippy `onHide` handler: give the focus back to the trigger if it was inside the popover."
  [^js instance]
  (when (.contains (.-popper instance) (.-activeElement js/document))
    (let [reference (.-reference instance)]
      (.focus (or (.querySelector reference "button, a[href], input") reference)))))

(def ^:private hide-on-esc
  "Tippy plugin: Escape closes the popover and returns the focus to its trigger."
  #js {:name "hideOnEsc"
       :defaultValue true
       :fn (fn [^js instance]
             (let [on-key-down #(when (= "Escape" (.-key %)) (.hide instance))]
               #js {:onShow #(.addEventListener js/document "keydown" on-key-down)
                    :onHide (fn []
                              (.removeEventListener js/document "keydown" on-key-down)
                              (return-focus instance))}))})

(def ^:private uncontrolled-plugins #js [hide-on-esc])

(defn html
  "Wraps some content in a tooltip with the provided html inside. Popovers that
  are not controlled through `:visible` close on Escape."
  ([tooltip-content wrapped-element]
   [html tooltip-content wrapped-element nil nil])
  ([tooltip-content wrapped-element options]
   [html tooltip-content wrapped-element options nil])
  ([tooltip-content wrapped-element options deactivated-options]
   [:> Tippy
    (apply dissoc
           (merge
            {:animation "shift-away"
             :arrow true
             :content (reagent/as-element tooltip-content)
             :interactive true
             :offset [0 10]
             :placement "bottom"
             :theme "light"
             :trigger "click"}
            (when-not (contains? options :visible)
              {:plugins uncontrolled-plugins})
            options)
           deactivated-options)
    wrapped-element]))

(defn text
  "Wraps content in a tooltip with the provided text. If you pass a react-
  component as `content`, wrap it in a `span` to enable forward referencing."
  [title content options]
  [:> Tippy
   (merge
    {:animation "shift-away"
     :arrow true
     :offset [0 10]
     :placement "bottom"
     :theme "light"
     :content title}
    options)
   content])
