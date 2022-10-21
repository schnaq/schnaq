(ns schnaq.interface.views.modal
  (:require ["react-bootstrap" :refer [Modal]]
            [com.fulcrologic.guardrails.core :refer [>defn => ?]]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.translations :refer [labels]]))

(>defn modal
  "Create a modal and takes an optional `toggle-element`, e.g. a button, which
  opens the modal when clicked. `toggle-element` must be a function/1 returning
  a component."
  ([props title body]
   [map? any? any? => :re-frame/component]
   [modal props nil title body])
  ([props _toggle-element _title _body]
   [map? (? :re-frame/component) any? any? => :re-frame/component]
   (let [show (r/atom (or (:show props) false))]
     (fn [props toggle-element title body]
       [:<>
        (when toggle-element
          [toggle-element {:onClick #(reset! show true)}])
        [:> Modal (merge {:show @show
                          :onHide (fn [_e]
                                    (reset! show false)
                                    (rf/dispatch [:modal/dissoc]))}
                         (dissoc props :show))
         [:> (oget Modal :Header) {:closeButton true}
          [:> (oget Modal :Title) title]]
         [:> (oget Modal :Body)
          body]]]))))

(defn modal-view
  "Include modal in view."
  []
  (when-let [modal @(rf/subscribe [:modal])]
    modal))

;; -----------------------------------------------------------------------------
;; Enter Name Modal

(defn anonymous-modal
  "Basic modal which is presented to anonymous users trying to alter statements."
  [header-label shield-label info-label]
  [modal {:show true}
   (labels header-label)
   [:<>
    [:p [icon :shield "m-auto" {:size "lg"}] " " (labels shield-label)]
    [:p (labels :discussion.anonymous-edit.modal/persuade)]
    [:button.btn.btn-primary.mx-auto.d-block
     {:on-click #(rf/dispatch [:keycloak/login])}
     (labels info-label)]]])

;; -----------------------------------------------------------------------------

(rf/reg-sub
 :modal
 :modal)

(rf/reg-event-db
 :modal
 (fn [db [_ data]]
   (assoc db :modal data)))

(rf/reg-event-db
 :modal/dissoc
 (fn [db _]
   (dissoc db :modal)))
