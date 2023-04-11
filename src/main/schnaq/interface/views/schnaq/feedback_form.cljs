(ns schnaq.interface.views.schnaq.feedback-form
  (:require ["react-bootstrap/Button" :as Button]
            ["react-bootstrap/Form" :as Form]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.views.pages :as pages]))

(def ^:private FormGroup (oget Form :Group))
(def ^:private FormControl (oget Form :Control))
(def ^:private FormLabel (oget Form :Label))
(def ^:private FormCheck (oget Form :Check))

(defn- scala-input
  [question-ordinal]
  [:div.border.rounded.p-3.text-center
   [:> FormCheck
    {:inline true :type "radio" :name (str "feedback-item-" question-ordinal) :label "1"}]
   [:> FormCheck
    {:inline true :type "radio" :name (str "feedback-item-" question-ordinal) :label "2"}]
   [:> FormCheck
    {:inline true :type "radio" :name (str "feedback-item-" question-ordinal) :label "3"}]
   [:> FormCheck
    {:inline true :type "radio" :name (str "feedback-item-" question-ordinal) :label "4"}]
   [:> FormCheck
    {:inline true :type "radio" :name (str "feedback-item-" question-ordinal) :label "5"}]])

(defn- feedback-form []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])
        feedback (:discussion/feedback current-discussion)]
    ;; TODO on submit send data to backend and set a flag that the user already participated
    ;; TODO maybe direct them back to the overview
    [pages/with-discussion-header
     {:page/heading (:discussion/title current-discussion)}
     [:div.panel-white.p-4.text-center
      [:h1 "Feedback"]
      [:p.text-muted "The feedback collected here is anonymous and will be shown to the moderator of this schnaq"]
      [:div.text-start.centered-form
       [:> Form
        (for [question (sort-by :feedback.item/ordinal (:feedback/items feedback))]
          [:> FormGroup {:controlId (str "feedback-item-" (:feedback.item/ordinal question)) :class "my-4"}
           [:> FormLabel (:feedback.item/label question)]
           (if (= (:feedback.item/type question) :feedback.item.type/text)
             [:> FormControl {:placeholder "Feedback…"}]
             ;; Scala
             [scala-input (:feedback.item/ordinal question)])])
        [:div.text-center
         [:> Button {:variant "primary" :type "submit" :class "mt-4"} "Submit Feedback"]]]]]]))

(defn feedback-form-view []
  [feedback-form])