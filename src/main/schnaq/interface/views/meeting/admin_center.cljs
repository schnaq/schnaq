(ns schnaq.interface.views.meeting.admin-center
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [ghostwheel.core :refer [>defn-]]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.config :refer [config]]
            [schnaq.interface.text.display-data :refer [labels img-path fa]]
            [schnaq.interface.utils.clipboard :as clipboard]
            [schnaq.interface.utils.js-wrapper :as js-wrap]
            [schnaq.interface.views.notifications :refer [notify!]]
            [schnaq.interface.views.base :as base]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.utils.localstorage :as ls]))

(>defn- get-share-link
  [current-route]
  [map? :ret string?]
  (let [share-hash (-> current-route :path-params :share-hash)
        path (reitfe/href :routes.meeting/show {:share-hash share-hash})
        location (oget js/window :location)]
    (gstring/format "%s//%s%s" (oget location :protocol) (oget location :host) path)))

(>defn- get-admin-center-link
  "Building the current URL with validated path, and without extra-stuff, like
  internal hashtag-routing."
  [current-route]
  [map? :ret string?]
  (let [{:keys [share-hash edit-hash]} (:path-params current-route)
        path (reitfe/href :routes.meeting/admin-center {:share-hash share-hash
                                                        :edit-hash edit-hash})
        location (oget js/window :location)]
    (gstring/format "%s//%s%s" (oget location :protocol) (oget location :host) path)))


;; -----------------------------------------------------------------------------

(defn- copy-link-form
  "A form that displays the link the user can copy. Form is read-only."
  [create-link-fn id-extra]
  (reagent/create-class
    {:component-did-mount
     (fn [_]
       (js-wrap/tooltip (str "#meeting-link-form-" id-extra)))
     :component-will-unmount
     (fn [_]
       (js-wrap/tooltip (str "#meeting-link-form-" id-extra) "disable")
       (js-wrap/tooltip (str "#meeting-link-form-" id-extra) "dispose"))
     :reagent-render
     (fn []
       (let [display-content (create-link-fn @(rf/subscribe [:navigation/current-route]))
             meeting-link-id (str "meeting-link" id-extra)]
         [:div.pb-4
          [:form.form.create-meeting-form.d-flex
           {:id (str "meeting-link-form-" id-extra)
            :on-click (fn [e]
                        (js-wrap/prevent-default e)
                        (clipboard/copy-to-clipboard! display-content)
                        (notify! (labels :meeting/link-copied-heading)
                                 (labels :meeting/link-copied-success)
                                 :info))
            :data-toggle "tooltip"
            :data-placement "bottom"
            :title (labels :meeting/copy-link-tooltip)}
           [:input.form-control.form-round.copy-link-form.clickable-no-hover
            {:id meeting-link-id
             :type "text"
             :value display-content
             :readOnly true}]
           [:label.clickable-no-hover.align-right.ml-4.d-flex.justify-content-center {:for meeting-link-id}
            [:div {:class (str "m-auto far fa-lg " (fa :copy))}]]]]))}))

(defn img-text
  "Create one icon in a grid"
  [path-to-img heading]
  [:<>
   [:img {:src path-to-img}]
   [:h5 heading]])

(defn- educate-element []
  [:div.row.mb-3
   [:div.col-12.col-md-6.share-link-icons
    [img-text (img-path :elephant-share)
     (labels :meeting/educate-on-link-text)]]
   [:div.col-12.col-md-6.share-link-icons
    [img-text (img-path :elephant-talk)
     (labels :meetings/educate-on-link-text-subtitle)]]])

(defn- educate-admin-element
  "Present edit-functions to user."
  [share-hash edit-hash]
  [:<>
   [:section.row.mb-3
    ;; edit
    [:div.col-md-6
     [:div.share-link-icons
      [img-text (img-path :elephant-erase)
       (labels :meeting/educate-on-edit)]]
     [:button.btn.button-secondary.float-left.my-2.w-100
      {:role "button"
       :on-click #(rf/dispatch [:navigation/navigate
                                :routes.meeting/edit
                                {:share-hash share-hash :edit-hash edit-hash}])}
      (labels :meetings/edit-schnaq-button)]]
    ;; admin hash
    [:div.col-md-6.share-link-icons
     [img-text (img-path :elephant-admin)
      (labels :meeting/educate-on-admin)]
     [:div.py-3
      [copy-link-form get-admin-center-link "admin-center"]]]]])


;; -----------------------------------------------------------------------------

(>defn- invite-participants-form
  "A form which allows the sending of the invitation-link to several participants via E-Mail."
  []
  [:ret :re-frame/component]
  (let [input-id "participant-email-addresses"]
    [:<>
     [:h4.mt-4 (labels :meeting.admin/send-invites-heading)]
     [:form.form.text-left.mb-5
      {:on-submit (fn [e]
                    (js-wrap/prevent-default e)
                    (rf/dispatch [:meeting.admin/send-email-invites
                                  (oget e [:target :elements])]))}
      [:div.form-group
       [:label.m-1 {:for input-id} (labels :meeting.admin/addresses-label)]
       [:textarea.form-control.m-1.input-rounded
        {:id input-id
         :name "participant-addresses" :wrap "soft" :rows 3
         :auto-complete "off"
         :required true
         :placeholder (labels :meeting.admin/addresses-placeholder)}]
       [:small.form-text.text-muted.float-right
        (labels :meeting.admin/addresses-privacy)]]
      [:button.btn.btn-outline-primary
       (labels :meeting.admin/send-invites-button-text)]]]))

(defn- invite-participants-tabs
  "Share link and invite via mail in a tabbed view."
  []
  (common/tab-builder
    "invite-participants"
    {:link (labels :meeting.admin-center.invite/via-link)
     :view [:<>
            [educate-element]
            [copy-link-form get-share-link "share-hash"]]}
    {:link (labels :meeting.admin-center.invite/via-mail)
     :view [invite-participants-form]}))

(rf/reg-event-fx
  :meeting.admin/send-admin-center-link
  (fn [{:keys [db]} [_ form]]
    (let [current-route (:current-route db)
          {:keys [share-hash edit-hash]} (:path-params current-route)]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/emails/send-admin-center-link")
                          :format (ajax/transit-request-format)
                          :params {:recipient (oget form ["admin-center-recipient" :value])
                                   :share-hash share-hash
                                   :edit-hash edit-hash
                                   :admin-center (get-admin-center-link current-route)}
                          :response-format (ajax/transit-response-format)
                          :on-success [:meeting-admin/send-email-success form]
                          :on-failure [:ajax-failure]}]]})))

(rf/reg-event-fx
  :meeting.admin/send-email-invites
  (fn [{:keys [db]} [_ form]]
    (let [raw-emails (oget form ["participant-addresses" :value])
          recipients (string/split raw-emails #"\s+")
          current-route (:current-route db)
          {:keys [share-hash edit-hash]} (:path-params current-route)]
      {:fx [[:http-xhrio {:method :post
                          :uri (str (:rest-backend config) "/emails/send-invites")
                          :format (ajax/transit-request-format)
                          :params {:recipients recipients
                                   :share-hash share-hash
                                   :edit-hash edit-hash
                                   :share-link (get-share-link current-route)}
                          :response-format (ajax/transit-response-format)
                          :on-success [:meeting-admin/send-email-success form]
                          :on-failure [:ajax-failure]}]]})))

(rf/reg-event-fx
  :meeting-admin/send-email-success
  (fn [_ [_ form {:keys [failed-sendings]}]]
    {:fx [[:dispatch [:notification/add
                      #:notification{:title (labels :meeting.admin.notifications/emails-successfully-sent-title)
                                     :body (labels :meeting.admin.notifications/emails-successfully-sent-body-text)
                                     :context :success}]]
          [:form/clear form]
          (when (seq failed-sendings)
            [:dispatch [:notification/add
                        #:notification{:title (labels :meeting.admin.notifications/sending-failed-title)
                                       :body [:<>
                                              (labels :meeting.admin.notifications/sending-failed-lead)
                                              [:ul
                                               (for [failed-sending failed-sendings]
                                                 [:li {:key failed-sending} failed-sending])]]
                                       :context :warning
                                       :stay-visible? true}]])]}))

(defn- send-admin-center-link
  "Send admin link via mail to the creator."
  []
  [:section
   [:h4 (labels :meeting.admin-center.edit.link/header)]
   [:p.lead (labels :meeting.admin-center.edit.link/primer)]
   (let [input-id "admin-link-mail-address"]
     [:form.form.text-left.mb-5
      {:on-submit (fn [e]
                    (js-wrap/prevent-default e)
                    (rf/dispatch [:meeting.admin/send-admin-center-link
                                  (oget e [:target :elements])]))}
      [:div.form-group
       [:label {:for input-id} (labels :meeting.admin-center.edit.link.form/label)]
       [:input.form-control.m-1.input-rounded
        {:id input-id
         :name "admin-center-recipient"
         :auto-complete "off"
         :required true
         :placeholder (labels :meeting.admin-center.edit.link.form/placeholder)}]
       [:small.form-text.text-muted.float-right
        (labels :meeting.admin/addresses-privacy)]]
      [:button.btn.btn-outline-primary
       (labels :meeting.admin-center.edit.link.form/submit-button)]])])

(defn- educate-admin-element-tabs
  "Composing admin-related sections."
  [share-hash edit-hash]
  (common/tab-builder
    "edit"
    {:link (labels :meeting.admin-center.edit/heading)
     :view [educate-admin-element share-hash edit-hash]}
    {:link (labels :meeting.admin-center.edit/send-link)
     :view [send-admin-center-link share-hash edit-hash]}))


;; -----------------------------------------------------------------------------

(defn- admin-center
  "This view is presented to the user after they have created a new meeting."
  []
  (let [{:meeting/keys [share-hash edit-hash title]} @(rf/subscribe [:meeting/last-added])
        spacer [:div.pb-5.mt-3]]
    ;; display admin center
    [:<>
     [base/nav-header]
     [base/header
      (labels :meeting.admin-center/heading)
      (gstring/format (labels :meeting.admin-center/subheading) title)]
     [:div.container.px-3.px-md-5.py-3.text-center
      [invite-participants-tabs]
      spacer
      [educate-admin-element-tabs share-hash edit-hash]
      spacer
      ;; stop image and hint to copy the link
      [:div.single-image [:img {:src (img-path :elephant-stop)}]]
      [:h4.mb-4 (labels :meetings/continue-with-schnaq-after-creation)]
      ;; go to meeting button
      [:button.btn.button-primary.btn-lg.center-block.mb-5
       {:role "button"
        :on-click #(rf/dispatch [:navigation/navigate :routes.meeting/show {:share-hash share-hash}])}
       (labels :meetings/continue-to-schnaq-button)]]]))

(defn admin-center-view []
  [admin-center])

;; #### Storage Helper ####

(def ^:private tuple-separator ",")
(def ^:private tuple-data #"\[(.*?)\]")
(def ^:private hash-separator " ")

(defn- parse-admin-access-string
  "Read previously visited meetings from localstorage. E.g (ls/get-item :meetings/admin-access)
  The string must obey the following convention '[share-1 edit-1],[share-2 edit-2]'
  "
  [hash-map-string]
  (let [hashes (remove empty? (string/split hash-map-string (re-pattern tuple-separator)))
        hashes-unbox (map (fn [tuple] (second (re-find tuple-data tuple))) hashes)
        hashes-vector (map (fn [tuple] (string/split tuple (re-pattern hash-separator))) hashes-unbox)
        hashes-map (into {} hashes-vector)]
    hashes-map))

(defn- add-admin-access-to-local-hashmap [hash-map-string share-hash edit-hash]
  (let [admin-access (parse-admin-access-string hash-map-string)
        new-admin-access (conj admin-access {(str share-hash) (str edit-hash)})]
    ;; check if share hash has already admin access
    (if-not (some #{share-hash} admin-access)
      new-admin-access
      admin-access)))

(defn- build-admin-access-from-localstorage [share-hash edit-hash]
  (let [local-hashes (ls/get-item :meetings/admin-access)
        new-hashes (add-admin-access-to-local-hashmap local-hashes share-hash edit-hash)
        hashes-tuple (map (fn [[val key]] (str "[" val " " key "]")) (seq new-hashes))
        hashes-as-string (string/join "," hashes-tuple)]
    hashes-as-string))

;; #### Events ####

(rf/reg-sub
  :meetings/load-admin-access
  (fn [db [_]]
    (get-in db [:meetings :admin-access])))

(rf/reg-event-db
  :meetings.save-admin-access/store-hashes-from-localstorage
  (fn [db _]
    (assoc-in db [:meetings :admin-access]
              (parse-admin-access-string (ls/get-item :meetings/admin-access)))))

(rf/reg-event-fx
  :meetings.save-admin-access/to-localstorage
  (fn [_ [_ share-hash edit-hash]]
    {:fx [[:localstorage/write
           [:meetings/admin-access
            (build-admin-access-from-localstorage share-hash edit-hash)]]
          [:dispatch [:meetings.save-admin-access/store-hashes-from-localstorage]]]}))