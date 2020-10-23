(ns schnaq.interface.views.brainstorm.create
  (:require [oops.core :refer [oget]]
            [schnaq.interface.text.display-data :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.meeting.create :as meeting-create]
            [schnaq.interface.views.pages :as pages]))

(defn- create-brainstorm []
  (pages/with-nav-and-header
    {:page/heading (labels :brainstorm/heading)}
    [:div.container.py-3.mt-3
     [:form
      {:on-submit (fn [e]
                    (let [title (oget e [:target :elements :meeting-title :value])]
                      (js-wrap/prevent-default e)
                      (meeting-create/new-meeting-helper title nil :meeting.type/brainstorm)))}
      [:div.agenda-meeting-container.shadow-straight.p-3
       [meeting-create/meeting-title-input]]
      [:div.pt-3.text-center
       [meeting-create/submit-meeting-button]]]]))

(defn create-brainstorm-view []
  [create-brainstorm])