(ns schnaq.interface.tour
  (:require ["react-bootstrap" :refer [Button]]
            ["react-joyride" :refer [STATUS] :default Joyride]
            [hodgepodge.core :refer [local-storage]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.colors :refer [colors]]
            [schnaq.interface.translations :refer [labels]]))

(def ^:private finished
  (oget STATUS :FINISHED))

;; -----------------------------------------------------------------------------

(def styles {:options {:primaryColor (:secondary colors)}})

(def ^:private tours
  {:user []
   :discussion
   [{:target ".info-card"
     :content "Hier findest du immer den Beitrag oder das Thema, um das es hier gerade geht."}
    {:target ".selection-card"
     :content "Formuliere nun deinen eigenen Beitrag. Siehst du die Leiste über dem Eingabefeld? Dort kannst du deinen Beitrag passend stylen und sogar Zeichnungen, Bilder und Dateien einfügen."}
    {:target ".statement-card"
     :content "Das hier ist ein Beitrag. Du kannst darauf direkt reagieren oder Reaktionen auf diesen Beitrag anschauen."}]
   :designs
   [{:target "#theme-title"
     :content "Du kannst hier dein eigenes Design erstellen. Fange an, indem du deinem Design einen Namen gibst."}
    {:target "#primary-color-picker"
     :content "Farben und Bilder kannst du hier auswählen. Vergiss nicht auf \"Speichern\" zu klicken."}
    {:target "#theme-preview-title"
     :content "Siehe hier eine Vorschau deiner Farben. Speichere, und dein Design steht dir für deine schnaqs zur Verfügung. Gehe dafür in deinem schnaq in die Einstellungen."}]})

(defn tour []
  (let [steps @(rf/subscribe [:tour/steps])
        callback
        (fn [data]
          (let [{:keys [status]} (js->clj data :keywordize-keys true)]
            (when (= status finished) (rf/dispatch [:tour/stop true]))))]
    [:<>
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
                             :skip (labels :tour.buttons/skip)}}])
     [:> Button {:variant "primary"
                 :on-click #(rf/dispatch [:tour/start :discussion])} "Start Discussion Tour"]
     [:> Button {:variant "primary"
                 :on-click #(rf/dispatch [:tour/start :user])} "Start User Tour"]
     [:> Button {:variant "primary"
                 :on-click #(rf/dispatch [:tour/start :designs])} "Designs"]]))

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
     (when-not (current-tour current-tours)
       {:fx [[:dispatch [:tour/start current-tour]]]}))))

(rf/reg-event-fx
 :tour/stop
 (fn [{:keys [db]} [_ save-tour?]]
   (if save-tour?
     (when-let [current-tour (get-in db [:tour :current])]
       {:db (-> db
                (update :tour dissoc :current)
                (update-in [:user :tours] conj current-tour))
        :fx [[:tour/to-localstorage current-tour]]})
     {:db (update db :tour dissoc :current)})))

(rf/reg-event-fx
 :user.tours/from-localstorage
 (fn [{:keys [db]}]
   (if-let [tours (:tours local-storage)]
     {:db (assoc-in db [:user :tours] tours)}
     {:db (assoc-in db [:user :tours] #{})
      :fx [[:localstorage/assoc [:tours #{}]]]})))

(rf/reg-sub
 :user/tours
 :-> #(get-in % [:user :tours]))

(rf/reg-fx
 :tour/to-localstorage
 (fn [tour]
   (when tour
     (let [new-tours (conj (or (:tours local-storage) #{}) tour)]
       (assoc! local-storage :tours new-tours)))))
