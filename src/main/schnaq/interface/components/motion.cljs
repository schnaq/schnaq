(ns schnaq.interface.components.motion
  (:require ["framer-motion" :refer [motion]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]))

(def card-fade-in-time
  "Set a common setting for fading in the cards, e.g. statement cards."
  0.25)

(defn animated-list
  "A motion list wrapper"
  [component]
  [:> (.-ul motion)
   {:layout true
    :style {:list-style-type "none"}}
   component])

(defn animated-list-item
  "A motion list item wrapper"
  [component]
  [:> (.-li motion)
   {:layout true}
   component])

(defn zoom-image
  "Create an image, which zooms in and out on click.

  Usage: `[zoom-image {:src \"path-to-file\" :class \"additional-classes\"}]`"
  ([properties]
   (let [open? (reagent/atom false)
         transition {:type :spring
                     :damping 25
                     :stiffness 120}]
     (fn []
       [:span.image-container {:class (when @open? "open")
                               :style {:z-index (when @open? 1000)}}
        [:> (.-span motion)
         {:animate {:opacity (if @open? 1 0)}
          :transition transition
          :class "shade"
          :on-click #(reset! open? false)}]
        [:> (.-img motion)
         (merge
          {:on-click #(swap! open? not)
           :layout true
           :transition transition}
          properties)]]))))

(defn fade-in-and-out
  "Add animation to component, which fades the component in and out. Takes
  optional parameter `delay`, which adds a delay to the point when the component
  should fade in."
  ([component]
   [fade-in-and-out component card-fade-in-time])
  ([component delay]
   [:> (.-div motion)
    {:initial {:opacity 0}
     :animate {:opacity 1}
     :transition {:delay delay}
     :exit {:opacity 0}}
    component]))

(defn- basic-move-in
  "A basic move-in animation. Pass any transition you like."
  [from-direction transition component]
  (let [direction (case from-direction
                    :top {:y "-200%"}
                    :bottom {:y "200%"}
                    :left {:x "-200%"}
                    {:x "200%"})]
    [:> (.-div motion)
     {:initial direction
      :animate {:x 0 :y 0}
      :exit direction
      :transition transition}
     component]))

(defn move-in
  "Add animation to component, which fades the component in and out."
  [from-direction component]
  [basic-move-in
   from-direction
   {:ease "easeOut" :duration 0.5}
   component])

(defn move-in-spring
  "The spring animation for any component"
  [from-direction component]
  [basic-move-in
   from-direction
   {:type "spring"
    :bounce 0.4
    :duration 1}
   component])

(defn collapse-in-out
  "A collapse animation with transitions for overflow hidden and visible."
  [collapsed? component]
  [:> (.-div motion)
   {:initial {:height "0"
              :overflow "hidden"}
    :animate (if collapsed?
               {:height "0"
                :overflow "hidden"}
               {:height "100%"
                :transition-end {:overflow "visible"}})
    :transition {:duration 0.3}}
   component])

(defn rotate
  "A basic rotation animation. Has an inline-block display attribute."
  [rotation component]
  [:> (.-div motion)
   {:style {:display "inline-block"}
    :animate {:rotate rotation}
    :transition {:duration 0.3}}
   component])

(defn spring-transition
  "Lets the elements bounce, when they transition"
  [component animation-attributes]
  [:> (.-div motion)
   {:animate animation-attributes
    :transition {:type "spring"
                 :bounce 0.3
                 :duration 1}}
   component])

(defn pulse-once
  "Lets your component pulse a number of times.
  Pulses once if the pulse-sub subscription returns true.
  Then dispatches the pulse-stop-event. Optional pulse color can be given."
  ([component pulse-sub pulse-stop-event]
   [pulse-once component pulse-sub pulse-stop-event #"000" #"000"])
  ([component pulse-sub pulse-stop-event base-color pulse-color]
   [:> (.-div motion)
    {:variants {:pulse {:scale [1 2.5 1 1]
                        :color [base-color pulse-color base-color]
                        :transition {:delay 0.1
                                     :duration 2}}}
     :animate (if @(rf/subscribe pulse-sub) :pulse :nothing)
     :on-animation-complete #(rf/dispatch pulse-stop-event)}
    component]))
