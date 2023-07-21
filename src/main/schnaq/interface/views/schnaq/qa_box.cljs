(ns schnaq.interface.views.schnaq.qa-box
  (:require ["react-bootstrap/Button" :as Button]
            ["react-bootstrap/Form" :as Form]
            ["react-bootstrap/InputGroup" :as InputGroup]
            [com.fulcrologic.guardrails.core :refer [=> >defn-]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.database.specs :as specs]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.inputs :as inputs]
            [schnaq.interface.matomo :as matomo]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]))

(def ^:private FormGroup (oget Form :Group))
(def ^:private FormLabel (oget Form :Label))
(def ^:private FormControl (oget Form :Control))

;; TODO i18n

(>defn- qa-box-card
  "Show a qa box card, where users can ask questions of the presenter."
  [qa-box]
  [::specs/qa-box => :re-frame/component]
  [:section.statement-card.activation-card
   [:div.mx-4.my-2
    [:div.d-flex
     [:h6.pb-2.text-center.mx-auto (:qa-box/label qa-box)]
     [:p "TODO…"]]
    (let [input-name (str "question-" (:db/id qa-box))]
      [:> Form {:on-submit (fn [e]
                             (.preventDefault e)
                             (rf/dispatch [:TODO (oget e [:target :elements input-name :value])]))
                :on-key-down (fn [e]
                               (when (tools/ctrl-press? e "Enter")
                                 (rf/dispatch [:TODO (oget e [:target :elements input-name :value])])))} ;;TODO
       [:> FormGroup
        [:> InputGroup
         [:> FormControl {:name input-name :placeholder "Gib hier deine Frage ein"}]
         [:> Button {:variant "primary" :type :submit} [icon :plane]]]]])]])

(defn qa-box-tab
  "QA box tab menu to create a qa-box."
  []
  (let [label-name "qa-input"
        checkbox-name "qa-checkbox"]
    [:> Form {:className "mt-4"
              :on-submit (fn [event]
                           (.preventDefault event)
                           (let [label (oget event [:target :elements label-name :value])
                                 visible? (oget event [:target :elements checkbox-name :checked])]
                             (rf/dispatch [:qa-box/create visible? label])))} ;; TODO
     [inputs/floating "Überschrift (optional)" label-name]
     [inputs/checkbox "Q&A sichtbar für Nutzer:innen" checkbox-name {:defaultChecked true}]
     [:div.text-center
      [:> Button {:variant "primary"
                  :className "w-75 mt-3 mx-auto d-block"
                  :type :submit
                  :on-click #(matomo/track-event "Active User" "Action" "Create Q&A Box")}
       "Q&A Box erstellen"]]]))

;; TODO aktuell werden mit der Diskussion auch die unsichtbaren Boxen geladen

(rf/reg-event-fx
 :qa-box/create
 (fn [db [_ visible? label]]
   {:db (assoc-in db [:schnaq :selected :discussion/qa-boxes]
                  (conj (:schnaq/qa-boxes db)
                        {:qa-box/visible visible?
                         :qa-box/label (or label "")}))
    ;; TODO next continue here
    :fx [(http/xhrio-request db :post "/qa-box" [:qa-box.create/success]
                             {:label label
                              :visible? visible?}
                             [:qa-box.create/failure])]}))

(rf/reg-sub
 :qa-boxes
 (fn [db _]
   (get-in db [:schnaq :selected :discussion/qa-boxes])))

(rf/reg-sub
 :qa-box
 (fn [db [_ qa-box-id]]
   (->>
    (get-in db [:schnaq :selected :discussion/qa-boxes])
    (filter #(= (:db/id %) qa-box-id))
    first)))