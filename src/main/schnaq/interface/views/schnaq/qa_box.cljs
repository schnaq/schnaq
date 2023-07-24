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
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.shared-toolbelt :as shared-tools]))

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
  [:> Form {:className "mt-4"
            :on-submit (fn [event]
                         (.preventDefault event)
                         (rf/dispatch [:qa-box/create (oget event [:target])]))}
   [inputs/floating "Überschrift (optional)" "qa-input"]
   [inputs/checkbox "Q&A sichtbar für Nutzer:innen" "qa-checkbox" {:defaultChecked true}]
   [:div.text-center
    [:> Button {:variant "primary"
                :className "w-75 mt-3 mx-auto d-block"
                :type :submit
                :on-click #(matomo/track-event "Active User" "Action" "Create Q&A Box")}
     "Q&A Box erstellen"]]])

(rf/reg-event-fx
 :qa-box/create
 (fn [{:keys [db]} [_ form]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         label (oget form [:elements "qa-input" :value])
         visible? (oget form [:elements "qa-checkbox" :checked])
         temp-id (- 0 (rand-int 1000000))]
     {:db (assoc-in db [:schnaq :qa-boxes temp-id]
                    {:db/id temp-id
                     :qa-box/visible visible?
                     :qa-box/label (or label "")})
      :fx [(http/xhrio-request db :post "/qa-box" [:qa-box.create/success temp-id]
                               {:share-hash share-hash
                                :label label
                                :visible? visible?}
                               [:qa-box.create/failure temp-id])
           [:form/clear form]]})))

(rf/reg-event-db
 :qa-box.create/success
 (fn [db [_ temp-id response]]
   (when-let [qa-box (:qa-box response)]
     (-> db
         (update-in [:schnaq :qa-boxes] dissoc temp-id)
         (assoc-in [:schnaq :qa-boxes (:db/id qa-box)] qa-box)))))

(rf/reg-event-fx
 :qa-box.create/failure
 (fn [{:keys [db]} [_ temp-id]]
   {:db (update-in db [:schnaq :qa-boxes] dissoc temp-id)
    :fx [[:dispatch [[:notification/add
                      #:notification{:title "QA-Box Fehler"
                                     :body [:<>
                                            "Die QA-Box konnte nicht erstellt werden. Bitte versuche es noch einmal."]
                                     :context :error
                                     :stay-visible? false}]]]]}))

(rf/reg-sub
 :qa-boxes
 (fn [db _]
   (vals (get-in db [:schnaq :qa-boxes]))))

(rf/reg-sub
 :qa-box
 (fn [db [_ qa-box-id]]
   (get-in db [:schnaq :qa-boxes qa-box-id])))

(rf/reg-event-fx
 :qa-boxes/load-from-backend
 (fn [{:keys [db]} _]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:fx [(http/xhrio-request db :get (str "/qa-boxes/" share-hash)
                               [:qa-boxes.load-from-backend/success]
                               {})]})))

(rf/reg-event-db
 :qa-boxes.load-from-backend/success
 (fn [db [_ response]]
   (when-let [qa-boxes (:qa-boxes response)]
     (assoc-in db [:schnaq :qa-boxes] (shared-tools/normalize :db/id qa-boxes)))))