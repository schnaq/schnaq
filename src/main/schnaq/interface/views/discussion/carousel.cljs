(ns schnaq.interface.views.discussion.carousel
  (:require ["jquery" :as jquery]
            [re-frame.core :as rf]
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
       (let [params {:key (:db/id premise)}
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

(defn premises-carousel [premises]
  (let [id "carouselIndicators"
        function (fn [premise]
                   #(rf/dispatch [:discussion/continue :premises/select premise]))]
    [statement-carousel-div id premises function]))

(defn view [premises]
  [:div.container.px-0
   [:div#other-premises.others-say-container.inner-shadow-custom
    (when (not-empty premises)
      [premises-carousel premises])]])