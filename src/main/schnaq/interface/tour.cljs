(ns schnaq.interface.tour
  (:require ["react-bootstrap" :refer [Button]]
            ["react-joyride" :refer [STATUS] :default Joyride]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.colors :refer [colors]]))

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
     :content "Hier kannst du deine eigenen Beiträge auf oberster Ebene platzieren. Siehst du die Leiste über dem Eingabefeld? Dort kannst du deinen Beitrag passend stylen und sogar Zeichnungen, Bilder und Dateien einfügen."}
    {:target ".statement-card"
     :content "Das hier ist ein Beitrag. Du kannst darauf direkt reagieren oder Reaktionen auf diesen Beitrag anschauen."}
    {:target ".activation-card"
     :content "In dieser Karte werden Umfragen, Wortwolken oder auch Aktivierungsphrasen angezeigt."}
    {:target ".schnaq-navbar"
     :content "Das sind die Optionen und unterschiedlichen Ansichten für deinen schnaq. Dort kommst du auch zu deinem Profil, wenn du dir eins erstellst."}]})

(defn tour []
  (let [steps @(rf/subscribe [:tour/steps])
        callback
        (fn [data]
          (let [{:keys [status]} (js->clj data :keywordize-keys true)]
            (when (= status finished) (rf/dispatch [:tour/stop]))))]
    [:<>
     (when steps
       [:> Joyride {:callback callback
                    :continuous true
                    :run true
                    :steps steps
                    :styles styles}])
     [:> Button {:variant "primary"
                 :on-click #(rf/dispatch [:tour/run :discussion])} "Start Discussion Tour"]
     [:> Button {:variant "primary"
                 :on-click #(rf/dispatch [:tour/run :user])} "Start User Tour"]
     [:> Button {:variant "primary"
                 :on-click #(rf/dispatch [:tour/run :feed])} "Start Overview Tour"]]))

(rf/reg-sub
 :tour/steps
 (fn [db]
   (let [tour-type (get-in db [:tour :type])]
     (get tours tour-type))))

(rf/reg-event-db
 :tour/run
 (fn [db [_ tour-type]]
   (assoc-in db [:tour :type] tour-type)))

(rf/reg-event-db
 :tour/stop
 (fn [db]
   (update db :tour dissoc :type)))
