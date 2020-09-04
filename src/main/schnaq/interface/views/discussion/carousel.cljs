(ns schnaq.interface.views.discussion.carousel
  (:require ["jquery" :as jquery]
            [re-frame.core :as rf]
            [schnaq.interface.views.discussion.view-elements :as view]
            [reagent.core :as reagent]))


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

(defn- statement-carousel-div [id statement on-click]
  (let [id# (str "#" id)]
    [:div.carousel.slide {:data-ride "carousel" :id id}
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
        event-name "slid.bs.carousel"
        add-listener  #(.on (jquery id#) event-name
                             (fn []
                               (let [index (.index (jquery "div.active"))
                                          selected-statement (nth % index)]
                                      (println selected-statement))))
        remove-listener #(.off (jquery id#) event-name)]
    (reagent/create-class
      {:reagent-render
       (fn [] [statement-carousel-div id @statements-atom on-click])
       :component-did-mount
       (fn [_comp]
         ;; on select function for current carousel element
         (add-listener @statements-atom))
       :component-did-update
       (fn [this _argv]
         (let [[_ _ new-statements] (reagent/argv this)]
           (reset! statements-atom new-statements)
           (remove-listener)
           (add-listener @statements-atom)
           ))
       :component-will-unmount
       (fn [] (remove-listener))
       :display-name "carousel-component"})))

(defn premises-carousel [premises]
  (let [id "carouselIndicators"
        function (fn [premise]
                   #(rf/dispatch [:discussion/continue :premises/select premise]))]
    [statement-carousel id premises function]))

(defn view [premises]
  [:div.container.px-0
   [:div#other-premises.others-say-container.inner-shadow-custom
    (when (not-empty premises)
      [premises-carousel premises])]])