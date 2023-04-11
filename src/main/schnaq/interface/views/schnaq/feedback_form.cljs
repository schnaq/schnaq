(ns schnaq.interface.views.schnaq.feedback-form
  (:require [re-frame.core :as rf]
            [schnaq.interface.views.pages :as pages]))


(defn- feedback-form []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])]
    [pages/with-discussion-header
     {:page/heading (:discussion/title current-discussion)}
     [:h1 (:discussion/title current-discussion)]]))

(defn feedback-form-view []
  [feedback-form] )