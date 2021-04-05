(ns schnaq.interface.views.feedback.collect
  "Add feedback options to the site."
  (:require ["html2canvas" :as html2canvas]
            [clojure.string :as string]
            [goog.dom :as gdom]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.feedback.survey :as survey]
            [schnaq.interface.views.modals.modal :as modal]))

(defonce screenshot-url (reagent/atom nil))

(defn- screenshot!
  "Take screenshot of whole page using html2canvas. Errors in rendering SVG
  elements are normal."
  []
  (.then
    (html2canvas (gdom/getElement "app")
                 (clj->js {:letterRendering 1}))
    (fn [e]
      (reset! screenshot-url (.toDataURL e)))))

(defn- form-input
  "Show a form in a modal, which is presented to the user."
  []
  (let [with-screenshot? (reagent/atom false)
        nickname @(rf/subscribe [:user/display-name])]
    (fn []
      [:form.form
       {:on-submit
        (fn [e]
          (js-wrap/prevent-default e)
          (let [contact-name (oget e [:target :elements :contact-name])
                contact-mail (oget e [:target :elements :contact-mail])
                description (oget e [:target :elements :description])
                feedback {:feedback/contact-name (oget contact-name [:value])
                          :feedback/contact-mail (oget contact-mail [:value])
                          :feedback/description (oget description [:value])
                          :feedback/has-image? @with-screenshot?}]
            (rf/dispatch [:feedback/new feedback (when @with-screenshot? @screenshot-url)
                          [contact-name contact-mail description]])))}
       [:div.form-group
        [:label {:for "feedback-contact-name"}
         (labels :feedbacks.modal/contact-name)]
        [:input {:id "feedback-contact-name"
                 :class-name "form-control"
                 :default-value nickname
                 :placeholder (labels :feedbacks.modal/contact-name)
                 :autoFocus true :name "contact-name"}]
        [:small.form-text.text-muted
         (labels :feedbacks.modal/optional)]]
       [:div.form-group
        [:label {:for "feedback-contact-mail"}
         (labels :feedbacks.modal/contact-mail)]
        [:input {:id "feedback-contact-mail" :name "contact-mail"
                 :class-name "form-control" :type "email"
                 :placeholder (labels :feedbacks.modal/contact-mail)}]
        [:small.form-text.text-muted
         (labels :feedbacks.modal/optional)]]
       [:div.form-group
        [:label {:for "feedback-description"}
         (gstring/format "%s *" (labels :feedbacks.modal/description))]
        [:textarea {:id "feedback-description"
                    :class-name "form-control"
                    :rows "3" :name "description"
                    :required true}]]
       [:div.form-check
        [:input.form-check-input
         {:id "feedback-include-screenshot"
          :on-click #(reset! with-screenshot?
                             (oget (gdom/getElement "feedback-include-screenshot") [:checked]))
          :type "checkbox"
          :name "screenshot?"}]
        [:label.form-check-label {:for "feedback-include-screenshot"}
         (labels :feedbacks.modal/screenshot)]]
       (when (and @screenshot-url @with-screenshot?)
         [:img#feedback-screenshot.img-fluid.img-thumbnail.my-2 {:src @screenshot-url}])
       [:div.modal-footer
        [:input.btn.btn-primary.mr-auto {:type "submit"}]
        [:small.text-muted (labels :feedbacks.modal/disclaimer)]]])))

(defn- feedback-modal
  "Create a modal to fetch user's feedback."
  []
  [modal/modal-template
   (labels :feedbacks.overview/header)
   (common/tab-builder
     "feedback-tabs"
     {:link (labels :feedbacks/button)
      :view [:<>
             [:p (labels :feedbacks.modal/primer)]
             [form-input]]}
     {:link (labels :feedbacks.survey/tab)
      :view [survey/view]})])

(defn button
  "Presenting the feedback button."
  []
  [:div#feedback-wrapper.feedback-wrapper
   {:on-click (fn [_e]
                (screenshot!)
                (rf/dispatch [:modal {:show? true :large? true
                                      :child [feedback-modal]}]))}
   [:button.btn.btn-secondary.text-white.feedback (labels :feedbacks/button)]])


;; -----------------------------------------------------------------------------

(rf/reg-sub
  :feedbacks
  (fn [db _] (:feedbacks db)))

(rf/reg-event-fx
  :feedbacks/success
  (fn [_ _]
    {:fx [[:dispatch [:modal {:show? false :child nil}]]
          [:dispatch [:notification/add
                      #:notification{:title (labels :feedbacks.notification/title)
                                     :body (labels :feedbacks.notification/body)
                                     :context :success}]]]}))

(rf/reg-event-fx
  :feedback/new
  (fn [{:keys [db]} [_ feedback screenshot form-elements]]
    (when-not (string/blank? (:feedback/description feedback))
      {:fx [(http/xhrio-request db :post "/feedback/add" [:feedbacks/success]
                                (cond-> {:feedback feedback}
                                        screenshot (assoc :screenshot screenshot))
                                [:ajax.error/as-notification])
            [:form/clear form-elements]]})))
