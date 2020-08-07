(ns meetly.meeting.interface.views.feedback
  "Add feedback options to the site."
  (:require [goog.string :as gstring]
            ["@ivoviz/feedback.js" :as feedbackjs]
            [meetly.meeting.interface.config :as config]))

(def ^:private endpoint
  (gstring/format "%s/feedback/add" (:rest-backend config/config)))

(def feedback (new feedbackjs/Feedback (clj->js {:endpoint endpoint})))

(defn view []
  [:div#feedback-wrapper {:on-click #(.open feedback)}
   [:button.btn.btn-secondary.feedback "Feedback"]])

(comment
  (.open feedback)
  :end)