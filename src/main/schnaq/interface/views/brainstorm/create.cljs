(ns schnaq.interface.views.brainstorm.create
  (:require [oops.core :refer [oget]]
            [schnaq.interface.text.display-data :refer [labels img-path]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.meeting.create :as meeting-create]
            [schnaq.interface.views.pages :as pages]))

(defn- under-construction
  []
  [:div.icon-bullets-larger
   [:div.p-1.text-center
    [:div.pb-3 [:img {:src (img-path :icon-crane)}]]
    [:<>
     [:p.h4 (labels :startpage.under-construction/heading)]
     [:p (labels :startpage.under-construction/body)]]]])

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
       [:button.btn.button-primary (labels :brainstorm.create.button/save)]]]]
    [:div.py-4
     [under-construction]]]])

(defn create-brainstorm-view []
  [create-brainstorm])