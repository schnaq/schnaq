(ns schnaq.interface.views.discussion.carousel
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.discussion.view-elements :as view]))

(defn- carousel-indicators
  "Display indicators as circles at bottom"
  [id# statements]
  [:ol.carousel-indicators.carousel-indicator-custom
   ;; get number of statements and set the first element as selected
   (map (fn [i] (let [params {:key (str "indicator-" (:db/id (nth statements i)))
                              :data-target id#
                              :data-slide-to (str i)}]
                  (if (zero? i)
                    [:li.active params]
                    [:li params])))
        (range (count statements)))])

(defn- carousel-content
  "Display statement-bubbles inside a carousel."
  [statements on-click]
  [:div.carousel-inner
   ;; set first indexed element as selected
   (map-indexed
     (fn [index premise]
       (let [params {:key (:db/id premise)
                     :data-pause true}
             content [:div.premise-carousel-item
                      {:on-click (on-click premise)}
                      [view/statement-bubble premise]]]
         (if (zero? index)
           [:div.carousel-item.active params content]
           [:div.carousel-item params content])))
     statements)])

(defn- statement-carousel-div
  "The div containing the carousel element."
  [id statement on-click]
  (let [id# (str "#" id)]
    [:div.carousel.slide {:data-ride "carousel" :id id
                          :data-interval "false"}
     ;; indicator
     [carousel-indicators id# statement]
     ;; content
     [carousel-content statement on-click]
     ;; interface elements
     [:a.carousel-control-prev {:href id# :role "button" :data-slide "prev"}
      [:span.carousel-control-prev-icon {:aria-hidden "true"}]
      [:span.sr-only "Previous"]]
     [:a.carousel-control-next {:href id# :role "button" :data-slide "next"}
      [:span.carousel-control-next-icon {:aria-hidden "true"}]
      [:span.sr-only "Next"]]]))


(defn- statement-carousel
  "reagent component to launch a carousel which does not spin
  and sets the current element as selected statement"
  [id statements on-click]
  (let [id# (str "#" id)
        statements-atom (reagent/atom statements)
        ;; selected-statement is used for demonstration purposes here. It may be unnecessary on
        ;; the next discussion-flow rework.
        selected-statement (reagent/atom nil)
        event-name "slid.bs.carousel"
        listener-fn #(let [index (js-wrap/element-index "div.active")
                           active-statement (nth @statements-atom index)]
                       (reset! selected-statement active-statement))]
    (reagent/create-class
      {:reagent-render
       (fn [] [statement-carousel-div id @statements-atom on-click])
       :component-did-mount
       (fn [_comp]
         ;; on select function for current carousel element
         (js-wrap/add-listener id# event-name listener-fn))
       :component-did-update
       (fn [this _argv]
         (let [[_ _ new-statements _] (reagent/argv this)]
           (reset! statements-atom new-statements)
           (js-wrap/remove-listener id# event-name)
           (js-wrap/add-listener id# event-name listener-fn)))
       :component-will-unmount
       (fn [] (js-wrap/remove-listener id# event-name))
       :display-name "carousel-component"})))

(defn- premises-carousel
  "Displays a carousel containing the input premises"
  [premises]
  (let [id "carouselIndicators"
        function (fn [premise]
                   #(rf/dispatch [:discussion/continue :premises/select premise]))]
    [statement-carousel id premises function]))

(defn carousel-element [premises]
  [:div.container.px-0
   [:div#other-premises.others-say-container.inner-shadow-custom
    (when (not-empty premises)
      [premises-carousel premises])]])