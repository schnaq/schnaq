(ns meetly.meeting.interface.views.feedback
  "Add feedback options to the site."
  (:require ["html2canvas" :as html2canvas]
            [ajax.core :as ajax]
            [clojure.string :as string]
            [goog.dom :as gdom]
            [goog.string :as gstring]
            [meetly.meeting.interface.config :refer [config]]
            [meetly.meeting.interface.text.display-data :refer [labels]]
            [meetly.meeting.interface.views.base :as base]
            [meetly.meeting.interface.views.modals.modal :as modal]
            [meetly.meeting.interface.utils.toolbelt :as toolbelt]
            [oops.core :refer [oget oset!]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]))

(def ^:private screenshot (atom ""))

(defn- screenshot-as-base64-img []
  (.toDataURL @screenshot))

(defn- screenshot! []
  (.then
    (html2canvas (gdom/getElement "app")
                 (clj->js {:letterRendering 1 :allowTaint true}))
    (fn [e]
      (reset! screenshot e)
      (oset! (gdom/getElement "feedback-screenshot") [:src] (.toDataURL e)))))


;; -----------------------------------------------------------------------------
;; Views

(defn- form-input
  "Show a form in a modal, which is presented to the user."
  []
  (let [checked? (reagent/atom false)]
    (fn []
      [:form.form
       {:on-submit
        (fn [e]
          (.preventDefault e)
          (let [contact-name (oget e [:target :elements :contact-name :value])
                contact-mail (oget e [:target :elements :contact-mail :value])
                description (oget e [:target :elements :description :value])
                feedback {:feedback/contact-name contact-name
                          :feedback/contact-mail contact-mail
                          :feedback/description description
                          :feedback/has-image? @checked?}]
            (rf/dispatch [:feedback/new feedback (screenshot-as-base64-img)])))}
       [:div.form-group
        [:label {:for "feedback-contact-name"}
         (labels :feedbacks.modal/contact-name)]
        [:input {:id "feedback-contact-name"
                 :class-name "form-control" :type "text"
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
          :on-click (fn []
                      (toolbelt/add-or-remove-class "feedback-screenshot" @checked? "d-none")
                      (reset!
                        checked?
                        (oget (gdom/getElement "feedback-include-screenshot")
                              [:checked])))
          :type "checkbox"
          :name "screenshot?"}]
        [:label.form-check-label {:for "feedback-include-screenshot"}
         (labels :feedbacks.modal/screenshot)]
        [:img#feedback-screenshot.img-fluid.img-thumbnail.my-2.d-none]]
       [:div.modal-footer
        [:input.btn.btn-primary {:type "submit"}]
        [:small (labels :feedbacks.modal/disclaimer)]]])))

(defn- feedback-modal
  "Create a modal to fetch user's feedback."
  []
  (modal/modal-template
    (labels :feedbacks.overview/header)
    [:div
     [:p (labels :feedbacks.modal/primer)]
     [form-input]]))

(defn button
  "Presenting the feedback button."
  []
  [:div#feedback-wrapper
   {:on-click (fn [_e]
                (screenshot!)
                (rf/dispatch [:modal {:show? true
                                      :child [feedback-modal]}]))}
   [:button.btn.btn-secondary.feedback (labels :feedbacks/button)]])

(defn- list-feedbacks
  "Shows a list of all feedback."
  []
  [:div#feedback-list
   (let [feedbacks @(rf/subscribe [:feedbacks])]
     [:div
      [:h4 (gstring/format "Es gibt %s Rückmeldungen 🥳!" (count feedbacks))]
      [:table.table.table-striped
       [:thead
        [:tr
         [:th {:width "20%"} (labels :feedbacks.overview/contact-name)]
         [:th {:width "60%"} (labels :feedbacks.overview/description)]
         [:th {:width "20%"} (labels :feedbacks/screenshot)]]]
       [:tbody
        (for [feedback feedbacks]
          [:tr {:key (:db/id feedback)}
           [:td (:feedback/contact-name feedback)
            (when-not (string/blank? (:feedback/contact-mail feedback))
              [:a {:href (gstring/format "mailto:%s" (:feedback/contact-mail feedback))}
               [:i.far.fa-envelope.pl-1]])]
           [:td (:feedback/description feedback)]
           [:td.image
            (let [img-src (gstring/format "/feedbacks/screenshots/%s.png" (:db/id feedback))]
              [:a {:href img-src}
               [:img.img-fluid.img-thumbnail {:src img-src}]])]])]]])])

(defn overview
  "Shows the page for an overview of all feedbacks."
  []
  (let [feedbacks @(rf/subscribe [:feedbacks])]
    (if (empty? feedbacks)
      (let [password (js/prompt "Enter password to see all Feedbacks")]
        (rf/dispatch [:feedbacks/fetch password]))
      [:div
       [base/nav-header]
       [base/header
        (labels :feedbacks.overview/header)
        (labels :feedbacks.overview/subheader)]
       [:div.container.py-4 [list-feedbacks]]])))


;; -----------------------------------------------------------------------------

(rf/reg-sub
  :feedbacks
  (fn [db _] (:feedbacks db)))

(rf/reg-event-db
  :feedbacks/store
  (fn [db [_ all-feedbacks]] (assoc db :feedbacks all-feedbacks)))

(rf/reg-event-fx
  :feedbacks/fetch
  (fn [_ [_ password]]
    {:http-xhrio {:method :post
                  :uri (gstring/format "%s/feedbacks" (:rest-backend config))
                  :params {:password password}
                  :format (ajax/transit-request-format)
                  :response-format (ajax/transit-response-format)
                  :on-success [:feedbacks/store]
                  :on-failure [:ajax-failure]}}))

(rf/reg-event-fx
  :feedback/new
  (fn [_ [_ feedback screenshot]]
    (when-not (string/blank? (:feedback/description feedback))
      {:http-xhrio {:method :post
                    :uri (str (:rest-backend config) "/feedback/add")
                    :params {:feedback feedback
                             :screenshot screenshot}
                    :format (ajax/transit-request-format)
                    :response-format (ajax/transit-response-format)
                    :on-success [:modal {:show? false :child nil}]
                    :on-failure [:ajax-failure]}})))