(ns schnaq.interface.views.schnaq.activation
  (:require ["framer-motion" :refer [motion]]
            [goog.string :as gstring]
            [re-frame.core :as rf]
            [schnaq.interface.components.motion :as motion-comp]
            [schnaq.interface.matomo :as matomo]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.views.schnaq.dropdown-menu :as dropdown-menu]))

(def ^:private default-activation-background
  "https://s3.schnaq.com/schnaq-common/background/layered_background_square.svg")

(defn- schnaqqis
  "Walking schnaqqis with varying x and y positions."
  []
  (let [activation @(rf/subscribe [:schnaq/activation])
        tmp-count @(rf/subscribe [:schnaq.activation/temp-counter])
        new-elephants (min 33 (- (:activation/count activation) tmp-count))]
    [:<>
     ;; schnaqqis background
     (for [x (range 1 new-elephants 2)]
       (with-meta
         [:span.schnaqqi-walk
          {:style {:left (str (* -75 x) "px")
                   :top (str (max -50 (* -5 x)) "px")}}]
         {:key (str "schnaqqi-" x)}))
     ;; schnaqqis foreground
     (for [x (range 2 new-elephants 2)]
       (with-meta
         [:span.schnaqqi-walk
          {:style {:left (str (* -75 x) "px")
                   :top (str (min (+ 20 (* 30 (Math/sin x))) (* 2 x)) "px")}}]
         {:key (str "schnaqqi-" x)}))
     ;; leading schnaqqi
     [:span.schnaqqi-walk
      {:style {:left "30px"
               :top "10px"}}]]))

(defn- schnaqqi-walk-motion
  "A basic move-in animation. Pass any transition you like."
  []
  [:> (.-div motion)
   {:initial {:x "-50%"}
    :animate {:x ["-25%" "0%" "25%" "50%" "75%" "100%" "125%" "150%"]
              :opacity [0 1 1 1 1 1 1 1 0]}
    :exit {:opacity 0}
    :transition {:ease :linear
                 :duration [10]}
    :on-animation-complete (fn [_]
                             (rf/dispatch [:schnaq.activation/finish-animation]))}
   [:div.schnaqqi-walk-container
    [schnaqqis]]])

(defn- schnaqqi-walk []
  [:div.activation-schnaqqi-space
   (when @(rf/subscribe [:schnaq.activation/walk?])
     [schnaqqi-walk-motion])])

(defn- activation-dropdown-menu
  "Dropdown menu for activation containing reset and delete."
  []
  [dropdown-menu/moderator
   {:id "activation-dropdown-id"
    :class "text-white"}
   [:<>
    [dropdown-menu/item :reset
     :schnaq.activation/reset-button
     #(rf/dispatch [:activation/reset])]
    [dropdown-menu/item :bullseye
     :schnaq.admin.focus/button
     (fn []
       (rf/dispatch [:activation/start])
       (rf/dispatch [:schnaq.moderation.focus.entity/success]))]
    [dropdown-menu/item :trash
     :schnaq.activation/delete-button
     #(rf/dispatch [:activation/delete])]]])

(defn- activation-view [background-class button-class]
  (when-let [activation @(rf/subscribe [:schnaq/activation])]
    (let [theme @(rf/subscribe [:schnaq.selected/theme])
          activation-phrase (or (:theme.texts/activation theme)
                                (labels :schnaq.activation/phrase))
          background-image-url (or (:theme.images/header theme) default-activation-background)
          read-only? @(rf/subscribe [:schnaq.selected/read-only?])]
      [motion-comp/fade-in-and-out
       [:section.activation-card.blue-card
        {:class background-class
         :style (when-not (= "bg-transparent" background-class) {:background-image (gstring/format "url('%s')" background-image-url)})}
        [:div.d-flex
         [:h4.mx-auto.mt-3
          (gstring/format (labels :schnaq.activation/title)
                          activation-phrase)]
         [activation-dropdown-menu]]
        [:div.mx-auto.display-3 (:activation/count activation)]
        [schnaqqi-walk]
        (when-not read-only?
          [:div.text-center
           [:button.btn.btn-lg.btn-secondary
            {:class button-class
             :on-click (fn [_e]
                         (rf/dispatch [:activation/activate])
                         (matomo/track-event "Active User", "Action", "Use Quick-Activation"))}
            activation-phrase
            "!"]])]
       motion-comp/card-fade-in-time])))

(defn activation-event-view
  "Activation card for q-and-a view."
  []
  [activation-view
   "bg-transparent"
   "btn-lg activation-button-rounded"])

(defn activation-card
  "Activation card for the discussion-view."
  []
  [activation-view
   nil
   "w-75"])

(defn activation-tab
  "Activation menu to create and reset the current activation."
  []
  (let [activation @(rf/subscribe [:schnaq/activation])]
    [:div.pt-2
     [:div.text (labels :schnaq.activation.create/label)]
     [:div.text-center.pt-2
      (if activation
        [:<>
         [:button.btn.btn-dark.w-75
          {:on-click #(rf/dispatch [:activation/reset])}
          (labels :schnaq.activation.create/reset-button)]
         [:button.btn.btn-outline-dark.w-75.mt-1
          {:on-click #(rf/dispatch [:activation/delete])}
          (labels :schnaq.activation.create/delete-button)]]
        [:button.btn.btn-secondary.w-75
         {:on-click (fn [_e]
                      (rf/dispatch [:activation/start])
                      (matomo/track-event "Active User", "Action", "Create Quick-Activation"))}
         (labels :schnaq.activation.create/start-button)])]]))

;; events and subscriptions

(rf/reg-sub
 :schnaq/activation
 ;; Returns the activation of the selected schnaq.
 (fn [db _]
   (get-in db [:schnaq :current :activation])))

(rf/reg-event-db
 :schnaq.activation/dissoc
 ;; Remove current activation
 (fn [db]
   (update-in db [:schnaq :current] dissoc :activation)))

(rf/reg-sub
 :schnaq.activation/temp-counter
 ;; Returns the current temp counter to count how many schnaqqis shall walk.
 (fn [db _]
   (get-in db [:schnaq :current :activation :temp-counter] 0)))

(rf/reg-event-db
 :schnaq.activation/temp-counter
 (fn [db [_ count-inc]]
   (let [temp-count (get-in db [:schnaq :current :activation :temp-counter] 0)]
     (assoc-in db [:schnaq :current :activation :temp-counter] (+ count-inc temp-count)))))

(rf/reg-event-db
 :schnaq.activation/finish-animation
 (fn [db]
   (let [counter (get-in db [:schnaq :current :activation :activation/count] 0)]
     (assoc-in db [:schnaq :current :activation :temp-counter] counter))))

(rf/reg-sub
 :schnaq.activation/walk?
 ;; Return wether or not a walking schnaqqi shall be displayed
 (fn [db _]
   (let [temp-activations (get-in db [:schnaq :current
                                      :activation :temp-counter] 0)
         current-activations (get-in db [:schnaq :current
                                         :activation :activation/count] 0)]
     (< temp-activations current-activations))))

(rf/reg-event-fx
 :activation/start
 (fn [{:keys [db]} _]
   {:fx [(http/xhrio-request db :put "/activation"
                             [:schnaq.activation.created/success]
                             {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])})]}))

(rf/reg-event-fx
 :schnaq.activation/load-from-backend
 (fn [{:keys [db]} _]
   {:fx [(http/xhrio-request db :get "/activation/by-share-hash"
                             [:schnaq.activation.load-from-backend/success]
                             {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])})]}))

(rf/reg-event-fx
 :schnaq.activation.created/success
 (fn [{:keys [db]} [_ response]]
   {:db (tools/new-activation-focus db (get-in response [:activation :db/id]))
    :fx [[:dispatch [:schnaq.activation.load-from-backend/success response]]]}))

(rf/reg-event-db
 :schnaq.activation.load-from-backend/success
 (fn [db [_ {:keys [activation]}]]
   (when activation
     (let [;; when previous count is larger than the current count, the counter has been reset
           current-count (:activation/count activation)
           previous-count (get-in db [:schnaq :current :activation :activation/count] 0)
           current-activation #(update-in % [:schnaq :current :activation] merge activation)
           temp-counter #(assoc-in % [:schnaq :current :activation :temp-counter] 0)]
       (if (> previous-count current-count)
         (-> db current-activation
             temp-counter)
         (-> db current-activation))))))

(rf/reg-event-fx
 :activation/reset
 (fn [{:keys [db]} _]
   {:fx [(http/xhrio-request db :put "/activation/reset"
                             [:schnaq.activation.load-from-backend/success]
                             {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])
                              :edit-hash (get-in db [:schnaq :selected :discussion/edit-hash])})]}))

(rf/reg-event-fx
 :activation/activate
 (fn [{:keys [db]} _]
   (when-let [share-hash (get-in db [:schnaq :selected :discussion/share-hash])]
     {:fx [(http/xhrio-request db :put "/activation/increment"
                               [:schnaq.activation.load-from-backend/success]
                               {:share-hash share-hash})]})))

(rf/reg-event-fx
 :activation/delete
 (fn [{:keys [db]} _]
   {:fx [(http/xhrio-request db :delete "/activation/delete"
                             [:schnaq.activation.delete/success]
                             {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])})]}))

(rf/reg-event-db
 :schnaq.activation.delete/success
 (fn [db _]
   (update-in db [:schnaq :current] dissoc :activation)))
