(ns schnaq.interface.components.motion
  (:require ["framer-motion" :refer [motion AnimatePresence]]
            [cljs.core.async :refer [go <! timeout]]
            [reagent.core :as reagent]))

(defn zoom-image
  "Create an image, which zooms in and out on click.

  Usage: `[zoom-image {:src \"path-to-file\" :class \"additional-classes\"}]`"
  [properties]
  (let [open? (reagent/atom false)
        transition {:type "spring"
                    :damping 25
                    :stiffness 120}]
    (fn []
      [:div.image-container {:class (when @open? "open")}
       [:> (.-div motion)
        {:animate {:opacity (if @open? 1 0)}
         :transition transition
         :class "shade"
         :on-click #(reset! open? false)}]
       [:> (.-img motion)
        (merge
          {:on-click #(swap! open? not)
           :layout true
           :transition transition}
          properties)]])))

(defn- delay-render
  "Wrap a component in this component to wait for a certain amount of
  milliseconds, until the provided component is rendered."
  [_component _delay]
  (let [ready? (reagent/atom false)]
    (reagent/create-class
      {:component-did-mount
       (fn [comp]
         (let [[_ _component delay-in-milliseconds] (reagent/argv comp)]
           (go (<! (timeout delay-in-milliseconds))
               (reset! ready? true))))
       :display-name "Delay Rendering of wrapped component"
       :reagent-render
       (fn [component _delay]
         (when @ready? [:> AnimatePresence component]))})))

(defn fade-in-and-out
  "Add animation to component, which fades the component in and out."
  [component]
  [:> (.-div motion)
   {:initial {:opacity 0}
    :animate {:opacity 1}
    :exit {:opacity 0}}
   component])

(defn delayed-fade-in
  "Takes a component and applies a delay and a fade-in-and-out animation.
  Optionally takes a `delay` in milliseconds."
  ([component]
   [delayed-fade-in component 500])
  ([component delay]
   [delay-render [fade-in-and-out component] delay]))

(defn move-in
  "Add animation to component, which fades the component in and out."
  [from-direction component]
  (let [direction (case from-direction
                    :top {:y "-200%"}
                    :bottom {:y "200%"}
                    :left {:x "-200%"}
                    {:x "200%"})]
    [:> (.-div motion)
     {:initial direction
      :animate {:x 0 :y 0}
      :exit direction
      :transition {:ease "easeOut" :duration 0.5}}
     component]))
