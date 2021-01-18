(ns schnaq.interface.views.brainstorm.create
  (:require [oops.core :refer [oget]]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.meeting.create :as meeting-create]
            [schnaq.interface.views.pages :as pages]))

(defn- create-brainstorm []
  [pages/with-nav-and-header
   {:page/heading (labels :brainstorm/heading)}
   [:div.container
    [:div.py-3.mt-3
     [:form
      {:on-submit (fn [e]
                    (let [title (oget e [:target :elements :meeting-title :value])
                          public? (oget e [:target :elements :public-discussion? :checked])]
                      (js-wrap/prevent-default e)
                      (meeting-create/new-meeting-helper title public? :meeting.type/brainstorm)))}
      [:div.agenda-meeting-container.shadow-straight.p-3
       [meeting-create/meeting-title-input]]
      [:div.form-check.pt-2.text-center
       [:input.form-check-input.big-checkbox {:type :checkbox
                                              :id :public-discussion?
                                              :defaultChecked true}]
       [:label.form-check-label.display-6 {:for :public-discussion?} (labels :discussion.create.public-checkbox/label)]]
      [:div.pt-3.text-center
       [:button.btn.button-primary (labels :brainstorm.create.button/save)]]]]]])

(defn create-brainstorm-view []
  [create-brainstorm])