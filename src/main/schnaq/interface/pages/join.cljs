(ns schnaq.interface.pages.join
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.views.pages :refer [with-nav-and-header]]))

(defn- join
  "Show a form to a user to join a schnaq via access-code."
  []
  (let [valid-form @(rf/subscribe [:schnaq.join/form])]
    [with-nav-and-header
     {:page/title (labels :schnaq.join.access-code/heading)
      :page/vertical-header? true
      :page/classes "base-wrapper bg-typography"
      :page/more-for-heading
      [:section.container.d-flex.justify-content-center.py-5.text-dark.text-center
       {:style {:min-height "50vh"}}
       [:div.card.shadow.w-50
        [:div.card-body
         [:div.card-title
          [:h1 {:style {:font-size "2rem"}}
           (labels :schnaq.join.access-code/heading)]]
         [:form
          {:on-submit (fn [e]
                        (.preventDefault e)
                        (let [access-code (oget e [:target :elements :access-code :value])]
                          (rf/dispatch [:schnaq.join/access-code access-code])))}
          [:div.form-group
           [:label.d-block.display-6
            (labels :schnaq.join.access-code/access-code)
            [:input.form-control.form-control-lg.text-center.my-2.has-validation
             {:class (case valid-form
                       :valid "is-valid"
                       :invalid "is-invalid"
                       nil)
              :type :number
              :required true
              :placeholder "0000 0000"
              :name :access-code
              :aria-describedby "access-code-info"}]]
           (when (= :invalid valid-form)
             [:div.text-danger (labels :schnaq.join.access-code/invalid)])
           [:small#access-code-info.form-text.text-muted
            (labels :schnaq.join.access-code/enter-code)]]
          [:button.btn.btn-lg.btn-secondary.w-75
           {:type :submit}
           (labels :schnaq.join.access-code/join)]]]]]}]))

(defn join-schnaq
  []
  [join])

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :schnaq.join/access-code
 (fn [{:keys [db]} [_ access-code]]
   {:fx [(http/xhrio-request db :get "/schnaq/by-access-code"
                             [:schnaq.join.access-code/success]
                             {:access-code access-code}
                             [:schnaq.join.access-code/error])]}))

(rf/reg-event-fx
 :schnaq.join.access-code/success
 (fn [{:keys [db]} [_ {:keys [share-hash]}]]
   {:db (assoc-in db [:schnaq :join :form] :valid)
    :fx [[:dispatch [:navigation/navigate :routes.schnaq/start {:share-hash share-hash}]]]}))

(rf/reg-event-db
 :schnaq.join.access-code/error
 (fn [db]
   (assoc-in db [:schnaq :join :form] :invalid)))

(rf/reg-sub
 :schnaq.join/form
 (fn [db]
   (get-in db [:schnaq :join :form])))

(rf/reg-event-db
 :schnaq.join.form/clear
 (fn [db]
   (update-in db [:schnaq :join] dissoc :form)))
