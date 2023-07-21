(ns schnaq.interface.views.schnaq.qa-box
  (:require ["react-bootstrap/Button" :as Button]
            ["react-bootstrap/Form" :as Form]
            ["react-bootstrap/InputGroup" :as InputGroup]
            [com.fulcrologic.guardrails.core :refer [=> >defn-]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.database.specs :as specs]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.toolbelt :as tools]))

(def ^:private FormGroup (oget Form :Group))
(def ^:private FormControl (oget Form :Control))

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

;; TODO i18n

(>defn- qa-box-card
  "Show a qa box card, where users can ask questions of the presenter."
  [qa-box]
  [::specs/poll => :re-frame/component]
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