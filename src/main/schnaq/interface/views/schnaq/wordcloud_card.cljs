(ns schnaq.interface.views.schnaq.wordcloud-card
  (:require
   [cljs.spec.alpha :as s]
   [clojure.string :as str]
   [com.fulcrologic.guardrails.core :refer [>defn >defn- =>]]
   [oops.core :refer [oget oget+]]
   [re-frame.core :as rf]
   [schnaq.database.specs :as specs]
   [schnaq.export :as export]
   [schnaq.interface.components.common :as common]
   [schnaq.interface.components.icons :refer [icon]]
   [schnaq.interface.components.inputs :as inputs]
   [schnaq.interface.components.motion :as motion]
   [schnaq.interface.components.wordcloud :as wordcloud]
   [schnaq.interface.matomo :as matomo]
   [schnaq.interface.translations :refer [labels]]
   [schnaq.interface.utils.http :as http]
   [schnaq.interface.utils.toolbelt :as tools]
   [schnaq.interface.views.schnaq.dropdown-menu :as dropdown-menu]
   [schnaq.shared-toolbelt :as stools]))

(defn- global-wordcloud
  "Shows the controls for the global word cloud"
  []
  (let [display-wordcloud? @(rf/subscribe [:schnaq.wordcloud/show?])]
    [:div.text-center.pt-5
     [:div.text-start.pb-2 (labels :schnaq.wordcloud/label)]
     (if display-wordcloud?
       [:button.btn.btn-dark.w-75
        {:on-click #(rf/dispatch [:schnaq.wordcloud/toggle false])}
        (labels :schnaq.wordcloud/hide)]
       [:button.btn.btn-primary.w-75
        {:on-click (fn [_e]
                     (rf/dispatch [:schnaq.wordcloud/toggle true])
                     (matomo/track-event "Active User" "Action" "Create Wordcloud"))}
        (labels :schnaq.wordcloud/show)])]))

(defn- local-wordcloud
  "The controls for local, interactive word clouds."
  []
  [:form.pt-2
   {:on-submit (fn [event]
                 (.preventDefault event)
                 (let [form (oget event [:target :elements])]
                   (rf/dispatch [:schnaq.wordcloud.local/create (oget form [:wordcloud-title :value])])
                   (rf/dispatch [:form/should-clear form])))}
   [:div.mb-3
    [:p (labels :schnaq.wordcloud.local.create/heading)]
    [inputs/floating (labels :schnaq.wordcloud.local.create/label) :wordcloud-title {:required true :autoFocus true}]]
   [:div.text-center.pt-2
    [:button.btn.btn-secondary.w-75
     {:type "submit"
      :on-click #(matomo/track-event "Active User" "Action" "Create Wordcloud")}
     (labels :schnaq.wordcloud.local.create/button)]]])

(defn wordcloud-tab
  "Wordcloud tab menu to hide and show a wordcloud."
  []
  [:div.pt-2
   [local-wordcloud]
   [global-wordcloud]])

(defn wordcloud-card
  "Displays a wordcloud in a card."
  []
  (when @(rf/subscribe [:schnaq.wordcloud/show?])
    [motion/fade-in-and-out
     [:section.activation-card
      [:div.d-flex.mt-3
       [:h4.mx-auto.mt-3
        (labels :schnaq.wordcloud/title)]
       [dropdown-menu/moderator
        {:id "wordcloud-dropdown-id"}
        [:<>
         [dropdown-menu/item :bullseye
          :schnaq.admin.focus/button
          #(rf/dispatch [:schnaq.admin.focus/entity (:db/id @(rf/subscribe [:schnaq/wordcloud]))])]
         [dropdown-menu/item :trash
          :schnaq.wordcloud/hide
          #(rf/dispatch [:schnaq.wordcloud/toggle])]]]]
      [wordcloud/wordcloud @(rf/subscribe [:wordcloud/words])]]]))

(defn- dropdown-menu
  "Dropdown menu for wordcloud configuration."
  [wordcloud-id]
  [dropdown-menu/moderator
   {:id (str "wordcloud-dropdown-id-" wordcloud-id)}
   [:<>
    [dropdown-menu/item :bullseye
     :schnaq.admin.focus/button
     #(rf/dispatch [:schnaq.admin.focus/entity wordcloud-id])]
    [dropdown-menu/item :trash
     :schnaq.wordcloud.local/delete-button
     #(when (js/confirm (labels :schnaq.wordcloud.local/delete-confirmation))
        (rf/dispatch [:schnaq.wordcloud.local/delete wordcloud-id]))]]])

(>defn- local-wordcloud-card
  "A single wordcloud with possibility for inputs."
  [{:keys [wordcloud/title wordcloud/words db/id]}]
  [::specs/wordcloud => :re-frame/component]
  (let [formatted-words (map #(hash-map :text (first %)
                                        :value (second %))
                             words)
        input-id (str "wordcloud-" id "-input")]
    [:div.text-center.pt-4.activation-card
     [:div.d-flex
      [:h4.text-center.mx-auto title]
      [dropdown-menu id]]
     [wordcloud/wordcloud formatted-words]
     [:form.form
      {:on-submit (fn [event]
                    (.preventDefault event)
                    (let [form (oget event [:target :elements])]
                      (rf/dispatch [:schnaq.wordcloud.local/send-words id (oget+ form [input-id :value])])
                      (rf/dispatch [:form/should-clear form])))}
      [:div.text-start.px-2.pb-2
       [:div.input-group
        [inputs/text (labels :schnaq.wordcloud.local.add-words/label) {:id input-id}]
        [:button.btn.btn-dark.my-1
         {:type "submit"}
         [icon :plane "m-auto"]]]
       [common/hint-text (labels :schnaq.wordcloud.local.add-words/hint)]]]]))

(>defn wordcloud-list
  "Displays all wordclouds of the current schnaq excluding the one in `exclude`."
  [exclude]
  [::specs/wordcloud :ret (s/coll-of :re-frame/component)]
  (for [wordcloud (remove #(= (:db/id exclude) (:db/id %)) @(rf/subscribe [:schnaq.wordclouds/local]))]
    [motion/fade-in-and-out
     [:article
      {:key (str "wordcloud-card-" (:db/id wordcloud))}
      [local-wordcloud-card wordcloud]]]))

;; -----------------------------------------------------------------------------

(rf/reg-sub
 :schnaq/wordcloud
 (fn [db _]
   (get-in db [:schnaq :selected :discussion/wordcloud])))

(rf/reg-sub
 :schnaq.wordcloud/show?
 :<- [:schnaq/wordcloud]
 (fn [wordcloud]
   (:wordcloud/visible? wordcloud)))

(rf/reg-event-fx
 :schnaq.wordcloud/toggle
 (fn [{:keys [db]} [_]]
   {:fx [(http/xhrio-request db :put "/wordcloud/discussion"
                             [:schnaq.wordcloud.toggle/success]
                             {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])
                              :edit-hash (get-in db [:schnaq :selected :discussion/edit-hash])})]}))

(rf/reg-event-fx
 :schnaq.wordcloud.toggle/success
 (fn [{:keys [db]} [_ {{:keys [db/id wordcloud/visible?] :as wordcloud} :wordcloud}]]
   {:db (cond-> (assoc-in db [:schnaq :selected :discussion/wordcloud] wordcloud)
          visible? (tools/new-activation-focus id))
    :fx [(when (:wordcloud/visible? wordcloud) [:dispatch [:schnaq.wordcloud/from-current-premises]])]}))

(rf/reg-event-fx
 :schnaq.wordcloud/from-current-premises
 (fn [{:keys [db]}]
   (let [all-premises (get-in db [:schnaq :statements])
         current-premise-ids (get-in db [:schnaq :statement-slice :current-level])
         premises (stools/select-values all-premises current-premise-ids)
         children-ids (flatten (map :statement/children premises))
         premises-with-children (remove nil? (concat premises (stools/select-values all-premises children-ids)))
         locked-statements-removed (remove :statement/locked? premises-with-children)]
     {:fx [[:dispatch [:wordcloud/store-words
                       {:string-representation (export/generate-fulltext locked-statements-removed)}]]]})))

(rf/reg-sub
 :schnaq.wordcloud/focus?
 (fn [db _]
   (let [{:keys [db/id]} (get-in db [:schnaq :selected :discussion/wordcloud])
         focus-id (get-in db [:schnaq :selected :discussion/activation-focus])]
     (= id focus-id))))

(rf/reg-event-db
 :schnaq.wordcloud/from-backend
 (fn [db [_ {:keys [wordcloud]}]]
   (when wordcloud
     (assoc-in db [:schnaq :selected :discussion/wordcloud] wordcloud))))

(rf/reg-event-fx
 :schnaq.wordcloud.local/create
 (fn [{:keys [db]} [_ title]]
   {:fx [(http/xhrio-request db :post "/wordcloud/local"
                             [:schnaq.wordcloud.local.create/success]
                             {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])
                              :edit-hash (get-in db [:schnaq :selected :discussion/edit-hash])
                              :title title})]}))

(rf/reg-event-db
 :schnaq.wordcloud.local.create/success
 (fn [db [_ {:keys [wordcloud]}]]
   (assoc-in db [:schnaq :wordclouds (:db/id wordcloud)] wordcloud)))

(rf/reg-sub
 :schnaq.wordclouds/local
 :-> #(vals (get-in % [:schnaq :wordclouds] {})))

(rf/reg-event-fx
 :schnaq.wordclouds/load-from-backend
 (fn [{:keys [db]} _]
   {:fx [(http/xhrio-request db :get "/wordcloud/local"
                             [:schnaq.wordclouds.local.load/success]
                             {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])})]}))

(rf/reg-event-db
 :schnaq.wordclouds.local.load/success
 (fn [db [_ return]]
   (when-let [wordclouds (:wordclouds return)]
     (assoc-in db [:schnaq :wordclouds] (stools/normalize :db/id wordclouds)))))

(rf/reg-event-fx
 :schnaq.wordcloud.local/send-words
 (fn [{:keys [db]} [_ wordcloud-id unprocessed-words]]
   (let [words (->> (str/split (wordcloud/remove-md-links unprocessed-words) #"\s")
                    (map wordcloud/extract-link-text-from-md)
                    (map #(str/replace % #"[^A-z0-9äöüÄÖÜß]" "")) ;; remove all non-word characters
                    (map str/lower-case)
                    distinct
                    ;; Experimentally deactivated stopwords,
                    ;; since people are following prompts (for performance reasons)
                    #_(remove #(wordcloud/stopwords %))
                    (remove empty?))]
     {:fx [(http/xhrio-request db :put "/wordcloud/local/words"
                               [:schnaq.wordcloud.local.send-words/success]
                               {:wordcloud-id wordcloud-id
                                :share-hash (get-in db [:schnaq :selected :discussion/share-hash])
                                :words words})]})))

(rf/reg-event-db
 :schnaq.wordcloud.local.send-words/success
 (fn [db [_ {:keys [wordcloud]}]]
   (assoc-in db [:schnaq :wordclouds (:db/id wordcloud)] wordcloud)))

(rf/reg-event-fx
 :schnaq.wordcloud.local/delete
 (fn [{:keys [db]} [_ wordcloud-id]]
   {:fx [(http/xhrio-request db :delete "/wordcloud/local"
                             [:schnaq.wordcloud.local.delete/success wordcloud-id]
                             {:share-hash (get-in db [:schnaq :selected :discussion/share-hash])
                              :edit-hash (get-in db [:schnaq :selected :discussion/edit-hash])
                              :wordcloud-id wordcloud-id})]}))

(rf/reg-event-db
 :schnaq.wordcloud.local.delete/success
 (fn [db [_ wordcloud-id return]]
   (when (:deleted? return)
     (update-in db [:schnaq :wordclouds] dissoc wordcloud-id))))

(rf/reg-sub
 :schnaq.wordcloud/local
 (fn [db [_ id]]
   (get-in db [:schnaq :wordclouds id])))
