(ns schnaq.interface.views.schnaq.activation
  (:require   ["framer-motion" :refer [motion]]
              [re-frame.core :as rf]
              [schnaq.interface.translations :refer [labels]]
              [schnaq.interface.utils.http :as http]))

(defn- schnaqqis
  "Walking schnaqqis with varying x and y positions."
  []
  (let [activation @(rf/subscribe [:schnaq/activation])
        tmp-count @(rf/subscribe [:schnaq.activation/temp-counter])
        new-elephants (- (:activation/count activation) tmp-count)]
    [:<>
     ;; schnaqqis background
     (for [x (range 1 new-elephants 2)]
       (with-meta
         [:span.schnaqqi-walk
          {:style {:left (str (* -75 x) "px")
                   :top (str (* -5 x) "px")}}]
         {:key (str "schnaqqi-" x)}))
     ;; schnaqqis foreground
     (for [x (range 2 new-elephants 2)]
       (with-meta
         [:span.schnaqqi-walk
          {:style {:left (str (* -75 x) "px")
                   :top (str (* 2 x) "px")}}]
         {:key (str "schnaqqi-" x)}))
     ;; leading schnaqqi
     [:span.schnaqqi-walk
      {:style {:left "30px"
               :top  "10px"}}]]))

(defn- schnaqqi-walk-motion
  "A basic move-in animation. Pass any transition you like."
  []
  [:> (.-div motion)
   {:initial {:x "-50%"}
    :animate {:x ["-50%" "0%" "50%" "100%" "150%"]
              :opacity [1 1 1 1 0]}
    :exit {:opacity 0}
    :transition {:ease :linear
                 :duration [10]}
    :on-animation-complete (fn [_]
                             (rf/dispatch [:schnaq.activation/finish-animation]))}
   [:div.schnaqqi-walk-container
    (schnaqqis)]])

(defn- schnaqqi-walk []
  (let [walk? @(rf/subscribe [:schnaq.activation/walk?])]
    [:div.activation-schnaqqi-space
     (when walk?
       [schnaqqi-walk-motion])]))

(defn activation-card []
  (let [activation @(rf/subscribe [:schnaq/activation])]
    (when activation
      [:section.statement-card.p-3.activation-background.text-white.overflow-hidden
       [:h4.mx-auto.mt-3 (labels :schnaq.activation/title)]
       [:div.mx-auto.display-3 (:activation/count activation)]
       [schnaqqi-walk]
       [:div.text-center
        [:button.btn.btn-secondary.w-75
         {:on-click (fn [_e] (rf/dispatch [:activation/activate]))}
         (labels :schnaq.activation/activation-button)]]])))

(defn activation-tab []
  (let [activation @(rf/subscribe [:schnaq/activation])]
    [:div.pt-2
     [:div.text (labels :schnaq.activation.create/label)]
     [:div.text-center.pt-2
      (if activation
        [:button.btn.btn-dark.w-75
         {:on-click (fn [_e] (rf/dispatch [:activation/reset]))}
         (labels :schnaq.activation.create/reset-button)]
        [:button.btn.btn-secondary.w-75
         {:on-click (fn [_e] (rf/dispatch [:activation/start]))}
         (labels :schnaq.activation.create/start-button)])]]))

;; events and subscriptions

(rf/reg-sub
 :schnaq/activation
 ;; Returns the activation of the selected schnaq.
 (fn [db _]
   (get-in db [:schnaq :current :activation])))

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
                             [:schnaq.activation.load-from-backend/success]
                             {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])
                              :edit-hash (get-in db [:schnaq :selected :discussion/edit-hash])})]}))

(rf/reg-event-fx
 :schnaq.activation/load-from-backend
 (fn [{:keys [db]} _]
   {:fx [(http/xhrio-request db :get "/activation/by-share-hash"
                             [:schnaq.activation.load-from-backend/success]
                             {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])})]}))

(rf/reg-event-db
 :schnaq.activation.load-from-backend/success
 (fn [db [_ response]]
   (let [activation (:activation response)
         ;; when previous count is larger than the current count, the counter has been reset
         current-count (:activation/count activation)
         previous-count (get-in db [:schnaq :current :activation :activation/count] 0)
         current-activation #(update-in % [:schnaq :current :activation] merge activation)
         temp-counter #(assoc-in % [:schnaq :current :activation :temp-counter] 0)]
     (if (> previous-count current-count)
       (-> db current-activation
           temp-counter)
       (-> db current-activation)))))

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
   {:fx [(http/xhrio-request db :put "/activation/increment"
                             [:schnaq.activation.load-from-backend/success]
                             {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])})]}))
