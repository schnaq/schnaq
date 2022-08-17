(ns schnaq.interface.tour
  (:require ["react-joyride" :refer [STATUS]]
            ["react-joyride$default" :as Joyride]
            [oops.core :refer [oget]]
            [reagent.core :as r]
            [schnaq.interface.components.colors :refer [colors]]))

(def ^:private finished
  (oget STATUS :FINISHED))

;; -----------------------------------------------------------------------------

(def styles {:options {:primaryColor (:secondary colors)}})

(def ^:private steps
  [{:target ".info-card"
    :content "Aktuelles Thema / Aussage"}
   {:target ".selection-card"
    :content "Hier kannst du deine eigenen Beiträge auf oberster Ebene platzieren. Siehst du die Leiste über dem Eingabefeld? Dort kannst du deinen Beitrag passend stylen und sogar Zeichnungen, Bilder und Dateien einfügen."}
   {:target ".statement-card"
    :content "Das hier ist ein Beitrag. Du kannst darauf direkt reagieren oder Reaktionen auf diesen Beitrag anschauen."}
   {:target ".schnaq-navbar"
    :content "Das sind die Optionen und unterschiedlichen Ansichten für deinen schnaq. Dort kommst du auch zu deinem Profil, wenn du dir eins erstellst."}])

(defn tour []
  (let [run? (r/atom false)]
    (fn []
      (let [callback
            (fn [data]
              (let [{:keys [status]} (js->clj data :keywordize-keys true)]
                (when (= status finished) (reset! run? false))))]
        [:<>
         [:> Joyride {:callback callback
                      :continuous true
                      :run @run?
                      :steps steps
                      :styles styles}]
         [:button.btn.btn-primary {:on-click #(reset! run? true)} "Start"]]))))
