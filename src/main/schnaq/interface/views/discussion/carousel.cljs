(ns schnaq.interface.views.discussion.carousel
  (:require ["jquery" :as jquery]
            [cljs.spec.alpha :as s]
            [ghostwheel.core :refer [>defn]]
            [reagent.core :as reagent]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.views.discussion.view-elements :as view]
            [schnaq.meeting.specs :as specs]
            [re-frame.core :as rf]
            [reagent.dom :as rdom]))

(defn- carousel-indicators
  "Display indicators as circles at bottom"
  [id# statements]
  [:ol.carousel-indicators.carousel-indicator-custom
   ;; get number of statements and set the first element as selected
   [:li.active {:key (str "indicator-active-" (:db/id (nth statements 0)))
                :data-target id#
                :data-slide-to 0}]
   (for [[index statement] (map-indexed vector (rest statements))]
     [:li {:key (str "indicator-" (:db/id statement))
           :data-target id#
           :data-slide-to (inc index)}])])

(defn- carousel-content
  "Display statement-bubbles inside a carousel."
  [statements on-click]
  [:div.carousel-inner
   ;; set first indexed element as selected
   (map-indexed
     (fn [index premise]
       (let [content [:div.premise-carousel-item
                      {:on-click (on-click premise)
                       :data-pause true}
                      [view/statement-bubble premise]]]
         (if (zero? index)
           [:div.carousel-item.active {:key (:db/id premise)} content]
           [:div.carousel-item {:key (:db/id premise)} content])))
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
  [statements on-click]
  (let [id "carouselIndicators"
        statements-atom (reagent/atom statements)]
    (reagent/create-class
      {:reagent-render
       (fn [] [statement-carousel-div id @statements-atom on-click])
       :component-did-update
       (fn [this _argv]
         (let [[_ new-statements _] (reagent/argv this)]
           (reset! statements-atom new-statements)
           ;; Fix for double active elements when adding statements dynamically
           (.removeClass (jquery ".carousel-item:not(:first)") "active")))
       :display-name "carousel-component"})))

(>defn carousel-element
  "Build a carousel. Can either be for conclusions in the beginning of a
  discussion or for premises in all other cases."
  [statements]
  [(s/coll-of ::specs/statement) :ret :re-frame/component]
  (let [history-count (count @(rf/subscribe [:discussion-history]))
        shown-statements (if (> history-count 1)
                           statements
                           (remove #(= :argument.type/undercut (:meta/argument-type %)) statements))]
    (when (seq shown-statements)
      [:div.container.px-0
       [:div.carousel-wrapper.inner-shadow-straight
        [:p.display-6.carousel-header.discussion-primary-background
         (labels :discussion.carousel/heading)]
        [statement-carousel shown-statements
         (fn [premise]
           (fn []
             (rf/dispatch [:discussion.history/push premise])
             (rf/dispatch [:discussion.statement/select premise])))]]])))