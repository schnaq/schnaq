(ns schnaq.interface.views.startpage.features
  (:require [ajax.core :as ajax]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels img-path]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.modals.modal :as modal]))

(defn- build-feature-text-box
  "Composing the text-part of a feature-row. Takes a `text-namespace` which
  looks up the corresponding text entries, which are then rendered."
  [text-namespace]
  (let [prepend-namespace (partial common/add-namespace-to-keyword text-namespace)]
    [:article.feature-text-box.pb-5
     [:p.lead.mb-1 (labels (prepend-namespace :lead))]
     [:h5 (labels (prepend-namespace :title))]
     [:p (labels (prepend-namespace :body))]]))

(defn- feature-row-image-left
  "Build a feature row, where the image is located on the left side."
  [image-key text-namespace]
  [:div.row.align-items-center.feature-row
   [:div.col-12.col-lg-5
    [:img.img-fluid {:src (img-path image-key)}]]
   [:div.col-12.col-lg-6.offset-lg-1
    [build-feature-text-box text-namespace]]])

(defn- feature-row-image-right
  "Build a feature row, where the image is located on the right side."
  [image-key text-namespace]
  [:div.row.align-items-center.feature-row
   [:div.col-12.col-lg-6
    [build-feature-text-box text-namespace]]
   [:div.col-12.col-lg-5.offset-lg-1
    [:img.img-fluid {:src (img-path image-key)}]]])

(defn- request-demo-modal
  "A modal which the user can use to request a demo"
  []
  [modal/modal-template
   "Persönliche Demo anfordern"
   [:form.form
    {:on-submit
     (fn [e]
       (js-wrap/prevent-default e)
       (rf/dispatch [:startpage.demo.request/send
                     (oget e [:target :elements :requester-name :value])
                     (oget e [:target :elements :requester-contact :value])
                     (oget e [:target :elements :requester-company :value])
                     (oget e [:target :elements :requester-phone :value])]))}
    [:div.form-group
     [:label {:for "demo-requester-name"}
      (labels :startpage.demo.request.modal.name/label)]
     [:input {:id "demo-requester-name"
              :class-name "form-control"
              :placeholder (labels :startpage.demo.request.modal.name/placeholder)
              :required true
              :autoFocus true :name "requester-name"}]]
    [:div.form-group.pb-2
     [:label {:for "demo-requester-contact"}
      (labels :startpage.demo.request.modal.email/label)]
     [:input {:id "demo-requester-contact" :name "requester-contact"
              :class-name "form-control" :type "email"
              :required true
              :placeholder (labels :startpage.demo.request.modal.email/placeholder)}]]
    [:div.form-group
     [:label {:for "demo-requester-company"}
      (labels :startpage.demo.request.modal.company/label)]
     [:input {:id "demo-requester-company" :name "requester-company"
              :class-name "form-control"
              :placeholder (labels :startpage.demo.request.modal.company/placeholder)}]
     [:small.form-text.text-muted
      (labels :feedbacks.modal/optional)]]
    [:div.form-group
     [:label {:for "demo-requester-phone"}
      (labels :startpage.demo.request.modal.phone/label)]
     [:input {:id "demo-requester-phone" :name "requester-phone"
              :class-name "form-control" :type "tel"
              :placeholder (labels :startpage.demo.request.modal.phone/placeholder)}]
     [:small.form-text.text-muted
      (labels :feedbacks.modal/optional)]]
    [:div.modal-footer
     [:input.btn.btn-primary.mr-auto {:type "submit"}]
     [:small.text-muted (labels :feedbacks.modal/disclaimer)]]]])

(rf/reg-event-fx
  :startpage.demo.request/send
  (fn [_ [_ name email company phone]]
    {:fx [[:http-xhrio {:method :post
                        :uri (str (:rest-backend config) "/emails/request-demo")
                        :params {:name name
                                 :email email
                                 :company company
                                 :phone phone}
                        :format (ajax/transit-request-format)
                        :response-format (ajax/transit-response-format)
                        :on-success [:startpage.demo.request/send-success]
                        :on-failure [:ajax-failure]}]]}))

(rf/reg-event-fx
  :startpage.demo.request/send-success
  (fn [_ _]
    {:fx [[:dispatch [:modal {:show? false :child nil}]]
          [:dispatch [:notification/add
                      #:notification{:title (labels :startpage.demo.request.send.notification/title)
                                     :body (labels :startpage.demo.request.send.notification/body)
                                     :context :success}]]]}))

(defn- request-demo-section
  "A button and some text to request a personal demo"
  []
  [:div.row.align-items-center.feature-row
   [:div.col-12.col-lg-5.text-center
    [:button.btn.button-secondary.font-150.mb-5
     {:on-click #(rf/dispatch [:modal {:show? true :large? false
                                       :child [request-demo-modal]}])}
     (labels :startpage.demo.request/button)]]
   [:div.col-12.col-lg-6.offset-lg-1
    [build-feature-text-box :startpage.demo.request]]])

(defn- meeting-organisation
  "Featuring meeting-organisation with an image."
  []
  [feature-row-image-right
   :startpage.features/meeting-organisation
   :startpage.features.meeting-organisation])

(defn- structured-discussions
  "Overview of structured discussions."
  []
  [feature-row-image-left
   :startpage.features/sample-discussion
   :startpage.features.discussion])

(defn- graph-visualization
  "Feature box showcasing the graph."
  []
  [feature-row-image-right
   :startpage.features/discussion-graph
   :startpage.features.graph])


;; -----------------------------------------------------------------------------

(defn feature-rows
  "Collection of feature rows."
  []
  [:section.pt-5
   [request-demo-section]
   [meeting-organisation]
   [structured-discussions]
   [graph-visualization]])
