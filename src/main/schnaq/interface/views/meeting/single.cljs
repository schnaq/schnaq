(ns schnaq.interface.views.meeting.single
  (:require [re-frame.core :as rf]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.markdown-parser :as markdown-parser]
            [schnaq.interface.views.meeting.admin-buttons :as admin-buttons]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.views.meeting.calendar-invite :as calendar-invite]
            [schnaq.interface.utils.localstorage :as ls]))

(defn- navigate-to-discussion
  "Load the discussion to the currently selected agenda."
  [agenda meeting]
  (when-not (nil? agenda)
    (rf/dispatch [:navigation/navigate :routes.discussion/start
                  {:id (-> agenda :agenda/discussion :db/id)
                   :share-hash (:meeting/share-hash meeting)}])
    (rf/dispatch [:agenda/choose agenda])))

(defn control-buttons []
  (let [share-hash (get-in @(rf/subscribe [:navigation/current-route])
                           [:parameters :path :share-hash])
        admin-access-map @(rf/subscribe [:meetings/load-admin-access])
        edit-hash (get admin-access-map share-hash)]
    [:div.text-center
     ;; check for admin privileges
     (when edit-hash
       [:<>
        [admin-buttons/admin-center share-hash edit-hash]
        [admin-buttons/edit share-hash edit-hash]
        [admin-buttons/calendar-invite share-hash edit-hash]])
     ;; suggestion button
     [admin-buttons/provide-suggestion share-hash]]))

(defn meeting-entry
  "Non wavy header with an optional back button.
  'title-on-click-function' is triggered when header is clicked
  'on-click-back-function' is triggered when back button is clicked,when no on-click-back-function is provided the back button will not be displayed"
  [title subtitle on-click-back-function]
  [:div.row.meeting-header.shadow-straight.m-0
   ;; arrow column
   [:div.col-md-1.back-arrow
    (when on-click-back-function
      [:p {:on-click on-click-back-function}
       [:i.arrow-icon {:class (str "m-auto fas " (fa :arrow-left))}]])]
   [:div.col-md-10
    [:div.container.px-4
     [:h1 title]
     [:hr]
     ;; mark down
     [markdown-parser/markdown-to-html subtitle]]]

   ;; button column
   [:div.col-md-1
    [control-buttons]]])

(defn- agenda-entry [agenda meeting]
  (let [old-statements-nums-map @(rf/subscribe [:agendas/load-statement-nums])
        statements-nums-map @(rf/subscribe [:agenda.meta/statement-num])
        old-statement-num (get old-statements-nums-map (str (:db/id agenda)) 0)
        statement-num (get statements-nums-map (:db/id agenda))
        new? (not (= old-statement-num statement-num))]
    [:div.card.meeting-entry-no-hover
     ;; title
     [:div.meeting-entry-title
      [:h4 (:agenda/title agenda)]]
     ;; description
     [:div.meeting-entry-desc
      [:hr]
      [markdown-parser/markdown-to-html (:agenda/description agenda)]]
     [:<>
      [:button.button-secondary-b-1.button-md
       {:title (labels :discussion/discuss-tooltip)
        :on-click #(navigate-to-discussion agenda meeting)}
       [:span.pr-2 (labels :discussion/discuss)]
       [:span.badge.badge-pill.badge-transparent statement-num " "
        (if new?
          [:i.primary-light-color {:class (str "m-auto fas " (fa :comment))}]
          [:i {:class (str "m-auto fas " (fa :comment))}])]]]]))

(defn agenda-in-meeting-view
  "The view of an agenda which gets embedded inside a meeting view."
  [meeting]
  [:<>
   (let [agendas @(rf/subscribe [:current-agendas])]
     (if (= :meeting.type/brainstorm (:meeting/type meeting))
       (navigate-to-discussion (first agendas) meeting)
       (for [agenda agendas]
         [:div.py-3 {:key (:db/id agenda)}
          [agenda-entry agenda meeting]])))])

(defn- meeting-title [current-meeting]
  ;; meeting header
  [meeting-entry
   (:meeting/title current-meeting)
   (:meeting/description current-meeting)
   (when-not toolbelt/production?                           ;; when in dev display back button
     (fn []
       (rf/dispatch [:navigation/navigate :routes/meetings])))])

(defn- single-meeting []
  (let [current-meeting @(rf/subscribe [:meeting/selected])]
    ;; meeting header
    [:div
     [base/meeting-header current-meeting]
     [meeting-title current-meeting]
     [:div.container.py-2
      [:div.meeting-single-rounded
       ;; list agendas
       [agenda-in-meeting-view current-meeting]]]]))

(defn single-meeting-view
  "Show a single meeting and all its Agendas."
  []
  [single-meeting])

;; subs

(rf/reg-sub
  :agendas/load-statement-nums
  (fn [db [_]]
    (let [string-value-hash-map (get-in db [:agendas :statement-nums])
          int-values (map (fn [[key value]] {key (js/parseInt value)}) string-value-hash-map)]
      (into {} int-values))))

(rf/reg-event-db
  :agendas.save-statement-nums/store-hashes-from-localstorage
  (fn [db _]
    (assoc-in db [:agendas :statement-nums]
              (ls/parse-hash-map-string (ls/get-item :agendas/statement-nums)))))

(rf/reg-event-fx
  :agenda.statement-nums/to-localstorage
  (fn [_ [_]]
    (let [statements-nums-map @(rf/subscribe [:agenda.meta/statement-num])]
      {:fx [[:localstorage/write
             [:agendas/statement-nums
              (ls/add-hash-map-and-build-map-from-localstorage statements-nums-map :agendas/statement-nums)]]
            [:dispatch [:agendas.save-statement-nums/store-hashes-from-localstorage]]]})))