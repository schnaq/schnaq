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
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.schnaq.dropdown-menu :as dropdown-menu]
            [schnaq.shared-toolbelt :as shared-tools]))

;; TODO show invisible boxes, if user is moderator
;; TODO edit of boxes
;; TODO show delete / answered controlls for moderators

(def ^:private FormGroup (oget Form :Group))
(def ^:private FormControl (oget Form :Control))

(>defn- dropdown-menu
  "Dropdown menu for poll configuration."
  [qa-box]
  [::specs/qa-box => :re-frame/component]
  (when (pos? (:db/id qa-box))
    (let [qa-box-id (:db/id qa-box)
          {:qa-box/keys [visible]} qa-box]
      [dropdown-menu/moderator
       {:id (str "qa-dropdown-id-" qa-box-id)}
       [:<>
        [dropdown-menu/item :trash
         :qa-boxes.dropdown/delete
         #(when (js/confirm (labels :qa-boxes.dropdown/delete-confirmation))
            (rf/dispatch [:qa-box/delete qa-box-id]))]]])))

(>defn- qa-box-card
  "Show a qa box card, where users can ask questions of the presenter."
  [qa-box]
  [::specs/qa-box => :re-frame/component]
  [:section.statement-card.activation-card
   [:div.mx-4.my-2
    [:div.d-flex
     [:h6.pb-2.text-center.mx-auto (:qa-box/label qa-box)]
     [dropdown-menu qa-box]]
    (let [input-name (str "question-" (:db/id qa-box))
          question-dispatch #(rf/dispatch [:qa-box.question/add (:db/id qa-box) (oget % [:target]) input-name])]
      [:> Form {:className "mb-3"
                :on-submit (fn [e]
                             (.preventDefault e)
                             (question-dispatch e))
                :on-key-down (fn [e]
                               (when (tools/ctrl-press? e "Enter")
                                 (question-dispatch e)))}
       [:> FormGroup
        [:> InputGroup
         [:> FormControl {:name input-name :placeholder (labels :qa-boxes.question-input/placeholder)}]
         [:> Button {:variant "primary" :type :submit} [icon :plane]]]]])
    (for [question (sort-by :qa-box.question/value > (:qa-box/questions qa-box))]
      [:div.border.rounded.mb-2.p-2.d-flex.flex-row.justify-content-between.align-items-center
       {:key (str "question-" (:db/id question))
        :className (when (:qa-box.question/answered question) "bg-success bg-opacity-25")}
       [:p.d-inline-block.mb-0 (:qa-box.question/value question)]
       [:div.flex-shrink-0.ms-2
        [icon :arrow-up] ;; TODO make clickable for upvotes
        [:span.ms-1 (or (:qa-box.question/upvotes question) 0)]]])]])

(defn qa-box-tab
  "QA box tab menu to create a qa-box."
  []
  [:> Form {:className "mt-4"
            :on-submit (fn [event]
                         (.preventDefault event)
                         (rf/dispatch [:qa-box/create (oget event [:target])]))}
   [inputs/floating (labels :qa-boxes.label-input/placeholder) "qa-input"]
   [inputs/checkbox (labels :qa-boxes.visibility-checkbox/label) "qa-checkbox" {:defaultChecked true}]
   [:div.text-center
    [:> Button {:variant "primary"
                :className "w-75 mt-3 mx-auto d-block"
                :type :submit
                :on-click #(matomo/track-event "Active User" "Action" "Create Q&A Box")}
     (labels :qa-boxes.create-button/label)]]])

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
    :fx [[:dispatch [:notification/add
                     #:notification{:title (labels :qa-boxes.create.error/heading)
                                    :body [:<> (labels :qa-boxes.create.error/body)]
                                    :context :error
                                    :stay-visible? false}]]]}))

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

(rf/reg-event-fx
 :qa-box/delete
 (fn [{:keys [db]} [_ qa-box-id]]
   (let [qa-box (get-in db [:schnaq :qa-boxes qa-box-id])
         share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:db (update-in db [:schnaq :qa-boxes] dissoc qa-box-id)
      :fx [(http/xhrio-request db :delete (str "/qa-box/" qa-box-id)
                               [:no-op]
                               {:share-hash share-hash}
                               [:qa-box.delete/failure qa-box])]})))

(rf/reg-event-db
 :qa-box.delete/failure
 (fn [{:keys [db]} [_ qa-box]]
   {:db (assoc-in db [:schnaq :qa-boxes (:db/id qa-box)] qa-box)
    :fx [[:dispatch [:notification/add
                     #:notification{:title (labels :qa-boxes.delete.error/heading)
                                    :body [:<> (labels :qa-boxes.delete.error/body)]
                                    :context :error
                                    :stay-visible? false}]]]}))

(rf/reg-event-fx
 :qa-box.question/add
 (fn [{:keys [db]} [_ qa-box-id input-form input-name]]
   (let [question (oget input-form [:elements input-name :value])
         share-hash (get-in db [:schnaq :selected :discussion/share-hash])
         temp-id (- (rand-int 10000000))]
     {:db (update-in db [:schnaq :qa-boxes qa-box-id :qa-box/questions] conj {:db/id temp-id
                                                                              :qa-box.question/value question
                                                                              :qa-box.question/answered false
                                                                              :qa-box.question/upvotes 0})
      :fx [(http/xhrio-request db :post (str "/qa-box/" qa-box-id "/questions")
                               [:no-op]
                               {:question question
                                :share-hash share-hash}
                               [:qa-box.question.add/failure qa-box-id temp-id])
           [:form/clear input-form]]})))

(rf/reg-event-fx
 :qa-box.question.add/failure
 (fn [{:keys [db]} [_ qa-box-id temp-id]]
   {:db (update-in db [:schnaq :qa-boxes qa-box-id :qa-box/questions] (fn [qs]
                                                                        (remove #(= (:db/id %) temp-id) qs)))
    :fx [[:dispatch [:notification/add
                     #:notification{:title (labels :qa-boxes.question.add.error/heading)
                                    :body [:<> (labels :qa-boxes.question.add.error/body)]
                                    :context :warning
                                    :stay-visible? false}]]]}))