(ns schnaq.interface.tour
  (:require ["react-joyride" :refer [Joyride STATUS]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.colors :refer [colors]]
            [schnaq.interface.config :as config]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.localstorage :refer [from-localstorage]]))

(def ^:private tour-over?
  "Statuses which mean the user is done with the tour, either by finishing or by
  dismissing it."
  #{(oget STATUS :FINISHED) (oget STATUS :SKIPPED)})

;; -----------------------------------------------------------------------------

(def options
  "Joyride's shared step options, e.g. theming."
  {:primaryColor (:secondary colors)
   :showProgress true
   ;; Small and in the corner, so the beacon doesn't sit on top of card content.
   :beaconSize 24
   :beaconPlacement "bottom-end"})

(defn- reduced-motion-styles
  "Stop the pulsing beacon for users who prefer reduced motion."
  []
  (if (.-matches (js/matchMedia "(prefers-reduced-motion: reduce)"))
    {:beaconInner {:animation "none"}
     :beaconOuter {:animation "none"}}
    {}))

(def ^:private tours
  {:user []
   :discussion
   [{:target ".info-card"
     :content (labels :tour.discussion/step-1)
     :title (labels :tour.discussion/step-1-title)}
    {:target ".selection-card"
     :content (labels :tour.discussion/step-2)
     :title (labels :tour.discussion/step-2-title)}
    {:target ".statement-card"
     :content (labels :tour.discussion/step-3)
     :title (labels :tour.discussion/step-3-title)}]
   :themes
   [{:target "#theme-title"
     :content (labels :tour.themes/step-1)
     :title (labels :tour.themes/step-1-title)}
    {:target "#primary-color-picker"
     :content (labels :tour.themes/step-2)
     :title (labels :tour.themes/step-2-title)}
    {:target "#theme-preview-title"
     :content (labels :tour.themes/step-3)
     :title (labels :tour.themes/step-3-title)}]
   :mindmap
   [{:target "#graph"
     :content (labels :tour.mindmap/step-1)
     :title (labels :tour.mindmap/step-1-title)
     :placement :auto
     ;; Joyride hands this to floating-ui's offset(), which accepts an object. It
     ;; keeps the corner beacon 12px inside the full-width canvas instead of on
     ;; the footer seam and the screen edge.
     :floatingOptions {:beaconOptions {:offset {:mainAxis -36 :alignmentAxis 12}}}}
    {:target "#graph-export"
     :content (labels :tour.mindmap/step-2)
     :title (labels :tour.mindmap/step-2-title)}
    {:target "#graph-settings"
     :content (labels :tour.mindmap/step-3)
     :title (labels :tour.mindmap/step-3-title)}]})

(defn tour []
  (let [steps @(rf/subscribe [:tour/steps])
        on-event
        (fn [data]
          (let [status (oget data "?status")]
            (when (tour-over? status) (rf/dispatch [:tour/stop true]))))]
    (when (seq steps)
      [:> Joyride {:onEvent on-event
                   :continuous true
                   :run true
                   :steps steps
                   :options options
                   :styles (reduced-motion-styles)
                   :locale {:back (labels :tour.buttons/back)
                            :close (labels :tour.buttons/close)
                            :last (labels :tour.buttons/last)
                            :next (labels :tour.buttons/next)
                            :nextWithProgress (labels :tour.buttons/next-with-progress)
                            :open (labels :tour.buttons/open)
                            :skip (labels :tour.buttons/skip)}}])))

;; -----------------------------------------------------------------------------

(rf/reg-sub
 :tour/steps
 (fn [db]
   (when-let [current-tour (get-in db [:tour :current])]
     (let [steps (get tours current-tour)]
       ;; Below Bootstrap xl the split navbar holding #graph-export and
       ;; #graph-settings is display:none, so Joyride would skip those steps.
       (if (and (= :mindmap current-tour)
                (not (.-matches (js/matchMedia (str "(min-width: " (:xl config/breakpoints) "px)")))))
         (subvec steps 0 1)
         steps)))))

(rf/reg-event-db
 :tour/start
 (fn [db [_ current-tour]]
   (assoc-in db [:tour :current] current-tour)))

(rf/reg-event-fx
 :tour/start-if-not-visited
 (fn [{:keys [db]} [_ current-tour]]
   (let [current-tours (get-in db [:user :tours])]
     (when-not (or (current-tour current-tours) config/in-iframe?)
       {:fx [[:dispatch [:tour/start current-tour]]]}))))

(rf/reg-event-fx
 :tour/stop
 (fn [{:keys [db]} [_ save-tour?]]
   (if save-tour?
     (when-let [current-tour (get-in db [:tour :current])]
       (let [new-tours (conj (or (from-localstorage :tours) #{}) current-tour)]
         {:db (-> db
                  (update :tour dissoc :current)
                  (update-in [:user :tours] conj current-tour))
          :fx [[:localstorage/assoc [:tours new-tours]]]}))
     {:db (update db :tour dissoc :current)})))

(rf/reg-event-fx
 :user.tours/from-localstorage
 (fn [{:keys [db]}]
   (if-let [tours (from-localstorage :tours)]
     {:db (assoc-in db [:user :tours] tours)}
     {:db (assoc-in db [:user :tours] #{})
      :fx [[:localstorage/assoc [:tours #{}]]]})))
