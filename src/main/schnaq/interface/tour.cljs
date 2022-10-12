(ns schnaq.interface.tour
  (:require ["react-joyride" :refer [STATUS] :default Joyride]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.colors :refer [colors]]
            [schnaq.interface.config :as config]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.localstorage :refer [from-localstorage]]))

(def ^:private finished
  (oget STATUS :FINISHED))

;; -----------------------------------------------------------------------------

(def styles {:options {:primaryColor (:secondary colors)}})

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
     :placement :left}
    {:target "#graph-export"
     :content (labels :tour.mindmap/step-2)
     :title (labels :tour.mindmap/step-2-title)}
    {:target "#graph-settings"
     :content (labels :tour.mindmap/step-3)
     :title (labels :tour.mindmap/step-3-title)}]})

(defn tour []
  (let [steps @(rf/subscribe [:tour/steps])
        callback
        (fn [data]
          (let [{:keys [status]} (js->clj data :keywordize-keys true)]
            (when (= status finished) (rf/dispatch [:tour/stop true]))))]
    (when steps
      [:> Joyride {:callback callback
                   :continuous true
                   :run true
                   :showProgress true
                   :steps steps
                   :styles styles
                   :locale {:back (labels :tour.buttons/back)
                            :close (labels :tour.buttons/close)
                            :last (labels :tour.buttons/last)
                            :next (labels :tour.buttons/next)
                            :open (labels :tour.buttons/open)
                            :skip (labels :tour.buttons/skip)}}])))

;; -----------------------------------------------------------------------------

(rf/reg-sub
 :tour/steps
 (fn [db]
   (when-let [current-tour (get-in db [:tour :current])]
     (get tours current-tour))))

(rf/reg-event-db
 :tour/start
 (fn [db [_ current-tour]]
   (assoc-in db [:tour :current] current-tour)))

(rf/reg-event-fx
 :tour/start-if-not-visited
 (fn [{:keys [db]} [_ current-tour]]
   (let [current-tours (get-in db [:user :tours])]
     (when-not (and (current-tour current-tours) config/in-iframe?)
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
