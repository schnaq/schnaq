(ns schnaq.interface.views.discussion.input
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.components.icons :refer [fa]]
            [schnaq.interface.components.videos :refer [video]]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.utils.js-wrapper :as jq]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.views.discussion.logic :as logic]))

(defn- statement-type-button
  "Button to select the attitude of a statement. Current attitude is subscribed via get-subscription.
  On-Click triggers the set-event with statement-type as last parameter."
  [statement-type label tooltip get-subscription set-event]
  (let [current-attitude @(rf/subscribe get-subscription)
        checked? (= statement-type current-attitude)]
    [:label.btn.btn-outline-dark.shadow-sm.py-2
     (when checked? {:class "active"})
     [:input {:type "radio" :name "options" :autoComplete "off"
              :defaultChecked checked?
              :title (labels tooltip)
              :on-click (fn [e] (jq/prevent-default e)
                          (rf/dispatch (conj set-event statement-type)))}]
     (labels label)]))

(defn statement-type-choose-button
  "Button group to differentiate between the statement types. The button with a matching get-subscription will be checked.
  Clicking a button will dispatch the set-subscription with the button-type as parameter."
  [get-subscription set-event]
  [:div.btn-group.btn-group-toggle {:data-toggle "buttons"}
   [statement-type-button :statement.type/support
    :discussion.add.button/support :discussion/add-premise-against
    get-subscription set-event]
   [statement-type-button :statement.type/neutral
    :discussion.add.button/neutral :discussion/add-premise-neutral
    get-subscription set-event]
   [statement-type-button :statement.type/attack
    :discussion.add.button/attack :discussion/add-premise-supporting
    get-subscription set-event]])

(defn- textarea-for-statements
  "Input, where users provide (starting) conclusions."
  [textarea-name]
  (let [pro-con-disabled? @(rf/subscribe [:schnaq.selected/pro-con?])
        current-route-name @(rf/subscribe [:navigation/current-route-name])
        starting-route? (= :routes.schnaq/start current-route-name)
        user @(rf/subscribe [:user/current])
        statement-type @(rf/subscribe [:form/statement-type])
        attitude (if starting-route? "neutral"
                                     (case statement-type
                                       :statement.type/support "support"
                                       :statement.type/attack "attack"
                                       :statement.type/neutral "neutral"))]
    [:<>
     [:div.d-flex.flex-row.discussion-input-content.rounded-1.mb-3
      [:div {:class (str "highlight-card-" attitude)}]
      [:div.w-100
       (when-not shared-config/embedded?
         [:div.d-flex.flex-row.justify-content-end.pr-lg-2
          ;; hide 'new post from you' text on mobile
          [:small.d-none.d-md-block.text-muted.mr-2 (labels :discussion.add.statement/new)]
          [common/avatar #:user.registered{:profile-picture (get-in user [:profile-picture :display])
                                           :display-name (get-in user [:names :display])} 32]])
       [:div.form-group
        [:textarea.form-control.discussion-text-input-area
         {:name textarea-name :wrap "soft" :rows 2
          :auto-complete "off"
          :autofocus "true"
          :onInput #(toolbelt/height-to-scrollheight! (oget % :target))
          ;; first reset input then set height +1px in order to prevent scrolling
          :required true
          :data-dynamic-height true
          :placeholder (labels :discussion/add-argument-conclusion-placeholder)}]]]]
     [:div.d-flex.flex-row.flex-wrap.justify-content-between.mt-1.justify-content-md-end.mt-md-0.w-100
      (when-not (or starting-route? pro-con-disabled?)
        [:div.input-group-prepend.mt-1
         [statement-type-choose-button [:form/statement-type] [:form/statement-type!]]])
      [:div.input-group-append
       [:button.btn.btn-dark-highlight.shadow-sm.ml-2.mt-1.py-2
        {:type "submit" :title (labels :discussion/create-argument-action)}
        [:div.d-flex.flex-row
         [:div.d-none.d-md-block.mr-1 (labels :statement.edit.button/submit)]
         [:i {:class (str "m-auto fas " (fa :plane))}]]]]]]))

(defn input-celebration-first
  "Show an celebration animation on each first post."
  []
  (when @(rf/subscribe [:celebrate/state? :first-post])
    [:div.d-flex
     [:div.mt-auto
      [common/delayed-fade-in
       [:video.video-scalable
        {:auto-play true :loop false :muted true :plays-inline true
         :onEnded (fn [] (rf/dispatch [:celebrate/state :first-post false]))}
        [:source {:src (video :celebration.schnaqqi/webm) :type "video/webm"}]
        [:source {:src (video :celebration.schnaqqi/mp4) :type "video/mp4"}]]]]]))

(defn input-form
  "Form to collect the user's statements."
  [textarea-name]
  (let [current-route-name @(rf/subscribe [:navigation/current-route-name])
        when-starting (fn [e] (jq/prevent-default e)
                        (rf/dispatch [:discussion.add.statement/starting
                                      (oget e [:currentTarget :elements])]))
        when-deeper-in-discussion (fn [e]
                                    (jq/prevent-default e)
                                    (logic/submit-new-premise (oget e [:currentTarget :elements])))
        event-to-send (if (= :routes.schnaq/start current-route-name)
                        when-starting when-deeper-in-discussion)]
    [:form.my-2
     {:on-submit #(event-to-send %)
      :on-key-down #(when (jq/ctrl-press % 13)
                      (event-to-send %))}
     [textarea-for-statements textarea-name]]))

(rf/reg-event-db
  :form/statement-type!
  (fn [db [_ statement-type]]
    (assoc-in db [:form :statement-type] statement-type)))

(rf/reg-sub
  :form/statement-type
  (fn [db]
    (get-in db [:form :statement-type] :statement.type/neutral)))
