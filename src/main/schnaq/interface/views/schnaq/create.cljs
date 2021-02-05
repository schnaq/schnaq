(ns schnaq.interface.views.schnaq.create
  (:require [oops.core :refer [oget]]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.pages :as pages]
            [re-frame.core :as rf]))

(defn title-input
  "The input and label for a new schnaq."
  []
  [:<>
   [:input#schnaq-title.form-control.form-title.form-border-bottom.mb-2
    {:type "text"
     :autoComplete "off"
     :required true
     :placeholder (labels :schnaq.create.input/placeholder)}]])

(defn- create-brainstorm []
  ;; todo relabel
  [pages/with-nav-and-header
   {:page/heading (labels :brainstorm/heading)}
   [:div.container
    [:div.py-3.mt-3
     [:form
      {:on-submit (fn [e]
                    (let [title (oget e [:target :elements :schnaq-title :value])
                          public? (oget e [:target :elements :public-discussion? :checked])]
                      (js-wrap/prevent-default e)
                      (rf/dispatch [:schnaq.create/new {:discussion/title title} public?])))}
      [:div.agenda-meeting-container.shadow-straight.p-3
       [title-input]]
      [:div.form-check.pt-2.text-center
       [:input.form-check-input.big-checkbox {:type :checkbox
                                              :id :public-discussion?
                                              :defaultChecked true}]
       [:label.form-check-label.display-6.pl-1 {:for :public-discussion?}
        (labels :discussion.create.public-checkbox/label)]]
      [:div.pt-3.text-center
       [:button.btn.button-primary (labels :brainstorm.create.button/save)]]]]]])

(defn create-brainstorm-view []
  ;; todo relabel
  [create-brainstorm])