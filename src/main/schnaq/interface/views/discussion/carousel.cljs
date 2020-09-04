(ns schnaq.interface.views.discussion.carousel
  (:require ["jquery" :as jquery]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [schnaq.interface.views.discussion.view-elements :as view]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; carousel

(defn statement-carousel-div [id statements on-click]
  (let [id# (str "#" id)]
    [:div.carousel.slide {:data-ride "carousel" :id id}
     ;; indicator
     [:ol.carousel-indicators.carousel-indicator-custom
      ;; range of number of statement and set the first element as selected
      (map
        (fn [i]
          (let [params {:key (str "indicator-" (:db/id (nth statements i)))
                        :data-target id#
                        :data-slide-to (str i)}]
            (if (zero? i)
              [:li.active params]
              [:li params])))
        (range (count statements)))]
     ;; content
     [:div.carousel-inner
      ;; set first indexed element as selected
      (map-indexed
        (fn [index premise]
          (let [params {:key (:db/id premise)}
                content [:div.premise-carousel-item
                         {:on-click
                          (on-click premise)}
                         [view/statement-bubble premise]]]
            (if (zero? index)
              [:div.carousel-item.active params content]
              [:div.carousel-item params content])))
        statements)]
     ;; interface elements
     [:a.carousel-control-prev {:href id# :role "button" :data-slide "prev"}
      [:span.carousel-control-prev-icon {:aria-hidden "true"}]
      [:span.sr-only "Previous"]]
     [:a.carousel-control-next {:href id# :role "button" :data-slide "next"}
      [:span.carousel-control-next-icon {:aria-hidden "true"}]
      [:span.sr-only "Next"]]])
  )

(defn statement-carousel
  "reagent component to launch a carousel which does not spin
  and sets the current element as selected statement"
  [statements on-click]
  (let [id "carouselIndicators"
        id# (str "#" id)]
    (reagent/create-class
      {:reagent-render
       (fn [] [statement-carousel-div id statements on-click])
       :component-did-mount
       (fn [_comp]
         ;; pause carousel rotation
         (.carousel (jquery id#) "pause")
         ;; on select function for current carousel element
         (.on (jquery id#) "slid.bs.carousel"
              (fn [] (let [index (.index (jquery "div.active"))
                           selected-statement (nth statements index)]
                       (println selected-statement)))))
       :display-name "carousel-component"})))

(defn premises-carousel [premises]
  (let [function (fn [premise]
                   (fn [] (rf/dispatch [:continue-discussion :premises/select premise])))]
    [statement-carousel premises function]))

(defn start-statement-carousel [starting-statements path-params]
  (let [function (fn [statement]
                   (fn [_e]
                     (rf/dispatch [:continue-discussion :starting-conclusions/select statement])
                     (rf/dispatch [:navigation/navigate :routes.discussion/continue
                                   {:id (:id path-params) :share-hash (:share-hash path-params)}])))]
    [statement-carousel starting-statements function]))

(defn view [premises]
  [:div.container.px-0
   [:div#other-premises.others-say-container.inner-shadow-custom
    (when (not-empty premises)
      [premises-carousel premises])]])