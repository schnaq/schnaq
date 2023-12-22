(ns schnaq.interface.views.presentation
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [schnaq.interface.components.common :refer [schnaq-logo]]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.components.schnaq :as sc]
            [schnaq.interface.navigation :as navigation]
            [schnaq.interface.translations :refer [labels]]
            [schnaq.interface.views.loading :as loading]
            [schnaq.interface.views.pages :as pages]
            [schnaq.interface.views.schnaq.poll :as poll]))

(defn- footer
  "Add footer links."
  []
  [:div.text-center.statement-card.p-3
   [:hr.w-50.mx-auto]
   [:a.btn.btn-sm.btn-link.text-dark
    {:href "https://schnaq.com/legal-note"}
    (labels :footer.buttons/legal-note)]
   [:a.btn.btn-sm.btn-link.text-dark
    {:href "https://schnaq.com/privacy"}
    (labels :router/privacy)]])

(defn- share-options
  "Show share-options, e.g. link and QR code."
  []
  [:section.text-center.statement-card.p-3
   [:div.display-6.text-center.pb-3
    (labels :presentation.access/code)
    [:span.h1 [sc/access-code]]]
   [:p.mb-0 (labels :presentation.access/qr-alternative)]
   [sc/qr-code (oget js/window :location :href) 250 {:bgColor "transparent"}]])

(defn- present-poll-controls
  "The controls allowing the user to switch between polls in presentation mode."
  [current-poll-id all-poll-ids]
  (let [polls-with-before-after (map vec (partition 3 1 (concat [nil] all-poll-ids [nil])))
        matching-poll-triple (first (filter #(= current-poll-id (second %)) polls-with-before-after))]
    [motion/fade-in-and-out
     [:div.d-flex.justify-content-between.panel-white-sm
      [:a.btn.btn-transparent.ms-1
       {:href (navigation/href :routes.present/entity
                               {:share-hash @(rf/subscribe [:schnaq/share-hash])
                                :entity-id (or (first matching-poll-triple) (last all-poll-ids))})}
       [icon :chevron/left]]
      [:div.d-flex.align-items-center
       (for [poll-id all-poll-ids
             :let [default-classes "tiny me-1"]]
         (with-meta
           [icon :circle (if (= current-poll-id poll-id) (str default-classes " text-primary") default-classes)]
           {:key (str "index-activation-" poll-id)}))]
      [:a.btn.btn-transparent.me-1
       {:href (navigation/href :routes.present/entity
                               {:share-hash @(rf/subscribe [:schnaq/share-hash])
                                :entity-id (or (last matching-poll-triple) (first all-poll-ids))})}
       [icon :chevron/right]]]]))

(defn- poll-presentation
  "Full screen view of a component."
  []
  (let [{:poll/keys [title] :as poll} @(rf/subscribe [:present/poll])
        all-poll-ids (map :db/id @(rf/subscribe [:schnaq/polls]))
        admin-access? @(rf/subscribe [:user/moderator?])]
    [pages/fullscreen
     {:page/title title}
     [:div.container.pt-5
      [:div.d-flex.flex-row.p-2.mb-5.statement-card
       [:h1 title]
       [schnaq-logo {:style {:width "200px"}
                     :class "pb-3 ms-auto"}]]
      (if poll
        (if admin-access?
          [:section.row
           [:div.col-12.col-md-3 [share-options]]
           [:div.offset-1.col-md-8.d-flex.flex-column.justify-content-between.min-h-100
            [:div.statement-card.p-3.ps-5
             [poll/ranking-results poll]]
            (when (> (count all-poll-ids) 1)
              [present-poll-controls (:db/id poll) all-poll-ids])]]
          [:section
           [:div.d-xl-none.w-100 [poll/input-or-results poll]]
           [:div.d-none.d-xl-block.w-50.mx-auto [poll/input-or-results poll]]])
        [:div.text-center [loading/spinner-icon]])
      [footer]]]))

;; -----------------------------------------------------------------------------

(defn view []
  [poll-presentation])

;; -----------------------------------------------------------------------------

(rf/reg-sub
 :present/poll
 (fn [db]
   (when-let [poll-id (get-in db [:present :poll])]
     (get-in db [:schnaq :polls poll-id]))))
