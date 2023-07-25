(ns schnaq.interface.views.schnaq.qa-box
  (:require ["react-bootstrap/Button" :as Button]
            ["react-bootstrap/Form" :as Form]
            ["react-bootstrap/InputGroup" :as InputGroup]
            [cljs.spec.alpha :as s]
            [com.fulcrologic.guardrails.core :refer [=> >defn- >defn]]
            [oops.core :refer [oget oget+]]
            [re-frame.core :as rf]
            [schnaq.database.specs :as specs]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.inputs :as inputs]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.matomo :as matomo]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.localstorage :refer [from-localstorage]]
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
        [dropdown-menu/item (if visible :eye-slash :eye)
         (if visible :qa-boxes.dropdown/hide :qa-boxes.dropdown/show)
         #(rf/dispatch [:qa-box/update-visibility qa-box-id (not visible)])]
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
    (when (not (:qa-box/visible qa-box))
      [:div.text-center
       [:p.text-muted (labels :qa-boxes.card/invisible)]])
    (let [input-name (str "question-" (:db/id qa-box))
          question-dispatch #(rf/dispatch [:qa-box.question/add (:db/id qa-box) (oget % [:target]) input-name])
          partitioned-questions (group-by :qa-box.question/answered (:qa-box/questions qa-box))
          sorted-questions (sort-by :qa-box.question/upvotes > (concat [] (get partitioned-questions false) (get partitioned-questions true)))
          cast-upvotes @(rf/subscribe [:qa-box/cast-upvotes (:db/id qa-box)])]
      [:<>
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
          [:> Button {:variant "primary" :type :submit} [icon :plane]]]]]
       [motion/animated-list
        (for [question sorted-questions]
          (with-meta
            [motion/animated-list-item
             [:div.border.rounded.mb-2.p-2.d-flex.flex-row.justify-content-between.align-items-center
              {:className (when (:qa-box.question/answered question) "bg-success bg-opacity-25")}
              [:p.d-inline-block.mb-0 (:qa-box.question/value question)]
              [:div.flex-shrink-0.ms-1
               (if (cast-upvotes (:db/id question))
                 [icon :smile-beam "mx-1 text-gray"]
                 [icon :arrow-up "mx-1 text-primary clickable" {:on-click #(rf/dispatch [:qa-box.question/upvote (:db/id qa-box) (:db/id question)])}])
               [:span (or (:qa-box.question/upvotes question) 0)]]]]
            {:key (str "question-" (:db/id question))}))]])]])

(>defn qa-box-list
  "Displays all qa-boxes of the current schnaq excluding the one in `exclude-id`."
  [exclude-id]
  [:db/id :ret (s/coll-of :re-frame/component)]
  (let [qa-boxes @(rf/subscribe [:qa-boxes])]
    (for [question-box-data (remove #(= exclude-id (:db/id %)) qa-boxes)]
      (do
        (println "question-box-data: " question-box-data)
        [:section
         {:key (str "qa-box-" (:db/id question-box-data))}
         [qa-box-card question-box-data]]))))

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
   (println "qa-boxes sub:" (vals (get-in db [:schnaq :qa-boxes] {})))
   (vals (get-in db [:schnaq :qa-boxes] {}))))

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
   (when-let [qa-boxes (shared-tools/normalize :db/id (:qa-boxes response))]
     (println "success: " qa-boxes)
     (assoc-in db [:schnaq :qa-boxes] qa-boxes))))

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
   (let [question (oget+ input-form [:elements input-name :value])
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

(rf/reg-event-fx
 :qa-box.question/upvote
 (fn [{:keys [db]} [_ qa-box-id question-id]]
   {:db (-> db
            (update-in [:schnaq :qa-boxes qa-box-id :qa-box/questions]
                       (fn [qs]
                         (map #(if (= (:db/id %) question-id)
                                 (assoc % :qa-box.question/upvotes ((fnil inc 0) (:qa-box.question/upvotes %)))
                                 %)
                              qs)))
            (update-in [:qa-boxes :upvotes qa-box-id] (fnil conj #{}) question-id))
    :fx [(http/xhrio-request db :post (str "/qa-box/" qa-box-id "/question/upvote")
                             [:qa-box.question.upvote/success]
                             {:question-id question-id
                              :share-hash (get-in db [:schnaq :selected :discussion/share-hash])}
                             [:qa-box.question.upvote/failure qa-box-id question-id])]}))

(rf/reg-event-fx
 :qa-box.question.upvote/success
 (fn [{:keys [db]} _]
   {:fx [[:localstorage/assoc [:qa-box/upvotes (get-in db [:qa-boxes :upvotes])]]]}))

(rf/reg-event-db
 :qa-box.question.upvote/failure
 (fn [db [_ qa-box-id question-id]]
   (update-in db [:qa-boxes :upvotes qa-box-id] disj question-id)))

(rf/reg-event-db
 :qa-box/load-upvotes
 ;; Load qa-box upvotes from localstorage
 (fn [db _]
   (when-let [cast-upvotes (from-localstorage :qa-box/upvotes)]
     (assoc-in db [:qa-boxes :upvotes] cast-upvotes))))

(rf/reg-sub
 :qa-box/cast-upvotes
 (fn [db [_ qa-box-id]]
   (get-in db [:qa-boxes :upvotes qa-box-id] #{})))

(rf/reg-event-fx
 :qa-box/update-visibility
 (fn [{:keys [db]} [_ qa-box-id make-visible?]]
   (let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:db (update-in db [:schnaq :qa-boxes qa-box-id :qa-box/visible] not)
      :fx [(http/xhrio-request db :patch (str "/qa-box/" qa-box-id "/visibility")
                               [:no-op]
                               {:share-hash share-hash
                                :make-visible? make-visible?}
                               [:qa-box.update-visibility/failure qa-box-id])]})))

(rf/reg-event-fx
 :qa-box.update-visibility/failure
 (fn [{:keys [db]} [_ qa-box-id]]
   {:db (update-in db [:schnaq :qa-boxes qa-box-id :qa-box/visible] not)
    :fx [[:dispatch [:notification/add
                     #:notification{:title (labels :qa-boxes.question.visibility.error/heading)
                                    :body [:<> (labels :qa-boxes.question.visibility.error/body)]
                                    :context :warning
                                    :stay-visible? false}]]]}))