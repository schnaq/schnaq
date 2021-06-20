(ns schnaq.interface.views.discussion.card-view
  (:require [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [img-path labels fa]]
            [schnaq.interface.utils.http :as http]
            [schnaq.interface.utils.js-wrapper :as jq]
            [schnaq.interface.utils.toolbelt :as toolbelt]
            [schnaq.interface.views.discussion.badges :as badges]
            [schnaq.interface.views.discussion.card-elements :as elements]
            [schnaq.interface.views.discussion.input :as input]
            [schnaq.interface.views.navbar.user-management :as um]
            [schnaq.interface.views.schnaq.admin :as admin]))

(defn- search-bar
  "A search-bar to search inside a schnaq."
  []
  [:form.mx-3
   {:on-submit (fn [e]
                 (jq/prevent-default e)
                 (rf/dispatch [:schnaq/search (oget e [:target :elements "search-input" :value])]))}
   [:div.input-group.search-bar
    [:input.form-control.my-auto
     {:type "text" :aria-label "Search" :placeholder
      (labels :schnaq.search/input) :name "search-input"}]
    [:div.input-group-append
     [:button.btn.btn-secondary.btn-rounded-2
      {:type "submit"}
      [:i {:class (str "m-auto fas " (fa :search))}]]]]])

(rf/reg-event-fx
  :schnaq/search
  (fn [{:keys [db]} [_ search-string]]
    (let [share-hash (get-in db [:current-route :path-params :share-hash])]
      {:db (assoc-in db [:search :schnaq :current :search-string] search-string)
       :fx [(http/xhrio-request db :get "/schnaq/search" [:schnaq.search/success]
                                {:share-hash share-hash
                                 :search-string search-string})
            [:dispatch [:navigation/navigate :routes.search/schnaq {:share-hash share-hash}]]]})))

(rf/reg-event-db
  :schnaq.search/success
  (fn [db [_ {:keys [matching-statements]}]]
    (assoc-in db [:search :schnaq :current :result] matching-statements)))

(defn card-discussion-header
  "Overview header for a discussion."
  [{:discussion/keys [title share-hash] :as discussion}]
  (let [admin-access-map @(rf/subscribe [:schnaqs/load-admin-access])
        edit-hash (get admin-access-map share-hash)
        history @(rf/subscribe [:discussion-history])]
    [:nav.navbar.navbar-expand-lg.py-3.navbar-dark.navbar-primary
     ;; schnaq logo
     [:a.navbar-brand.mr-auto {:href (reitfe/href :routes.schnaqs/personal)}
      [:img.d-inline-block.align-middle.mr-2
       {:src (img-path :logo-white) :width "150" :alt "schnaq logo"}]]
     ;; hamburger
     [:button.navbar-toggler
      {:type "button" :data-toggle "collapse" :data-target "#schnaq-navbar"
       :aria-controls "schnaq-navbar" :aria-expanded "false" :aria-label "Toggle navigation"
       :data-html2canvas-ignore true}
      [:span.navbar-toggler-icon]]
     ;; menu items
     [:div {:id "schnaq-navbar" :class "collapse navbar-collapse"}
      ;; click-able title
      [:div.mr-auto.clickable-no-hover
       {:on-click
        (fn []
          (rf/dispatch [:navigation/navigate :routes.schnaq/start {:share-hash share-hash}])
          (rf/dispatch [:schnaq/select-current discussion]))}
       [toolbelt/desktop-mobile-switch
        [:h3.mx-5 title]
        [:h3.mx-5.display-6 title]]]
      [search-bar]
      [admin/share-link]
      [admin/txt-export share-hash title]
      (when edit-hash
        [admin/admin-center share-hash edit-hash])
      ;; name input
      [um/user-handling-menu "btn-outline-light"]
      [:div.d-md-none
       [:hr]
       [:h6.text-left (labels :history/title)]
       [:div.row.px-3
        [elements/history-view-mobile history]]]]]))

(defn- discussion-start-view
  "The first step after starting a discussion."
  [{:discussion/keys [title author created-at] :as schnaq}]
  (let [current-starting @(rf/subscribe [:discussion.conclusions/starting])
        input-form [input/input-form "statement-text"]
        content {:statement/content title :statement/author author :statement/created-at created-at}
        badges [badges/static-info-badges schnaq]]
    [:<>
     [toolbelt/desktop-mobile-switch
      [elements/discussion-view-desktop
       schnaq content input-form badges nil current-starting nil]
      [elements/discussion-view-mobile
       schnaq content input-form badges nil current-starting nil]]]))

(defn- selected-conclusion-view
  "The first step after starting a discussion."
  [current-discussion]
  (let [current-premises @(rf/subscribe [:discussion.premises/current])
        history @(rf/subscribe [:discussion-history])
        current-conclusion (last history)
        info-content [elements/info-content-conclusion
                      current-conclusion (:discussion/edit-hash current-discussion)]
        badges [badges/extra-discussion-info-badges
                current-conclusion (:discussion/edit-hash current-discussion)]
        input-form [input/input-form "premise-text"]]
    [:<>
     [toolbelt/desktop-mobile-switch
      [elements/discussion-view-desktop
       current-discussion current-conclusion input-form badges info-content current-premises history]
      [elements/discussion-view-mobile
       current-discussion current-conclusion input-form badges info-content current-premises history]]]))

(rf/reg-sub
  :discussion.premises/current
  (fn [db _]
    (get-in db [:discussion :premises :current] [])))

(rf/reg-sub
  :discussion.conclusions/starting
  (fn [db _]
    (get-in db [:discussion :conclusions :starting] [])))

;; -----------------------------------------------------------------------------

(defn- derive-view []
  (let [current-discussion @(rf/subscribe [:schnaq/selected])
        current-route-name @(rf/subscribe [:navigation/current-route-name])]
    [:<>
     [card-discussion-header current-discussion]
     (if (= :routes.schnaq/start current-route-name)
       [discussion-start-view current-discussion]
       [selected-conclusion-view current-discussion])]))

(defn view []
  [derive-view])