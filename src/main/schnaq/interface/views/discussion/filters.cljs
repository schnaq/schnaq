(ns schnaq.interface.views.discussion.filters
  "Filters are saved and read as maps internaly. e.g.

  ```
  {:type :labels
   :label :check
   :criteria :includes}
  ```

   This filter filters for statements that include the label :check."
  (:require [clojure.set :as cset]
            [goog.dom :as gdom]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [schnaq.config.shared :as shared-config]
            [schnaq.interface.text.display-data :refer [labels fa]]
            [schnaq.interface.utils.toolbelt :as tools]
            [schnaq.interface.utils.tooltip :as tooltip]
            [schnaq.interface.views.discussion.labels :as statement-labels]
            [schnaq.interface.views.discussion.logic :as discussion-logic]))

(defn- set-selected-option
  "Helper function to set the correct temp atom value for a selection."
  [event store]
  (reset! store (tools/get-selection-from-event event)))

(defn- label-selections
  "Selection-options for label-type filters."
  [selected-label]
  [:<>
   [:div.form-row.pb-3
    [:div.col-auto
     [:select#filter-labels-selection.mr-1.form-control
      [:option {:value :includes} (labels :filters.option.labels/includes)]
      [:option {:value :excludes} (labels :filters.option.labels/excludes)]]]
    [:div.col-auto
     [:div.btn-group
      {:role "group"}
      [tooltip/html
       (for [label shared-config/allowed-labels]
         [:span.mr-3
          {:key (str "label-option-" label)
           :on-click #(reset! selected-label label)}
          [statement-labels/build-label label]])
       [:button#filter-labels-label.form-control
        (if (shared-config/allowed-labels @selected-label)
          (with-meta
            [statement-labels/build-label @selected-label]
            {:key (str "label-option-" @selected-label)})
          [:span.badge.badge-pill.badge-transparent "–––"])]]
      [:button.btn.btn-dark
       {:on-click #(reset! selected-label nil)}
       [:span.m-auto "x"]]]]]])

(defn- type-selections
  "Selection-options for type filters."
  []
  [:div.form-row.pb-3
   [:div.col-auto
    [:select#filter-type-selection.mr-1.form-control
     [:option {:value :is} (labels :filters.option.type/is)]
     [:option {:value :is-not} (labels :filters.option.type/is-not)]]]
   [:div.col-auto
    [:select#filter-type-type.mr-1.form-control
     ;; Needs to be string, otherwise ns will be stripped
     [:option {:value "statement.type/neutral"} (labels :discussion.add.button/neutral)]
     [:option {:value "statement.type/attack"} (labels :discussion.add.button/attack)]
     [:option {:value "statement.type/support"} (labels :discussion.add.button/support)]]]])

(defn- vote-selections
  "Selection-options for vote filters."
  []
  [:div.form-row.pb-3
   [:div.col-auto
    [:select#filter-votes-selection.mr-1.form-control
     [:option {:value ">"} (labels :filters.option.vote/bigger)]
     [:option {:value "="} (labels :filters.option.vote/equal)]
     [:option {:value "<"} (labels :filters.option.vote/less)]]]
   [:div.col-auto
    [:input#filter-votes-number.mr-1.form-control
     {:type :number
      :placeholder 0
      :defaultValue 0}]]])

(defn- add-filter-selection
  "A small compontent for adding new filters."
  []
  (let [current-selection (r/atom "labels")
        selected-label (r/atom nil)]
    (fn []
      [:section.border-bottom.pb-2.text-left
       [:div.form-group
        [:label {:for :add-filter-menu}
         (labels :filters.label/filter-for)]
        [:select#add-filter-menu.mr-1.form-control
         {:on-change #(set-selected-option % current-selection)}
         [:option {:value :labels} (labels :filters.option.labels/text)]
         [:option {:value :type} (labels :filters.option.type/text)]
         [:option {:value :votes} (labels :filters.option.votes/text)]]]
       (case @current-selection
         "labels" [label-selections selected-label]
         "type" [type-selections]
         "votes" [vote-selections]
         "")
       [:button.btn.btn-outline-dark.mr-2
        {:on-click #(case @current-selection
                      "labels"
                      (when @selected-label
                        (rf/dispatch [:filters.activate/labels
                                      (tools/get-current-selection (gdom/getElement "filter-labels-selection"))
                                      @selected-label]))
                      "type"
                      (rf/dispatch [:filters.activate/type
                                    (tools/get-current-selection (gdom/getElement "filter-type-selection"))
                                    (tools/get-current-selection (gdom/getElement "filter-type-type"))])
                      "votes"
                      (rf/dispatch [:filters.activate/votes
                                    (tools/get-current-selection (gdom/getElement "filter-votes-selection"))
                                    (.-value (gdom/getElement "filter-votes-number"))]))}
        [:i {:class (fa :plus)}] " " (labels :filters.add/button)]])))

(defn- prettify-filter
  "A helper returning a single filter prettified."
  [{:keys [type criteria label statement-type votes-number] :as filter-data}]
  (let [type-label (labels (keyword :filters.labels.type type))
        criteria-label (labels (keyword :filters.labels.criteria criteria))
        statement-type-label (when statement-type (labels (keyword :filters.stype statement-type)))
        pretty-label (when label [statement-labels/build-label label])]
    [:div.d-flex.justify-content-between
     [:p.d-inline-block.pr-2.my-auto {:key (str filter-data)}
      [:strong type-label] " "
      criteria-label " "
      (or pretty-label statement-type-label votes-number)]
     [:button.btn.btn-outline-primary-small.my-1
      {:on-click #(rf/dispatch [:filters/deactivate filter-data])} "x"]]))

(defn- active-filters
  "A menu showing the currently active filters."
  []
  (let [active @(rf/subscribe [:filters/active])]
    [:section.pt-2.text-left
     [:p (labels :filters.heading/active)]
     (when (seq active)
       (for [filter-data active]
         ;; The key needs to be set because its a list.
         ;; And the key is needed again in prettify-filter or else react does not render.
         (with-meta
           [prettify-filter filter-data]
           {:key (str filter-data)})))]))

(defn- default-menu
  "The default filter menu that is shown to the user."
  []
  [:<>
   [add-filter-selection]
   [active-filters]
   (when (< 1 (count @(rf/subscribe [:filters/active])))
     [:button.btn.btn-outline-secondary.text-center
      {:on-click #(rf/dispatch [:filters/clear])}
      (labels :filters.buttons/clear)])])

(defn filter-button
  "A button opening the default filters on click."
  []
  (let [active-filters? @(rf/subscribe [:filters/active?])]
    [tooltip/html
     [default-menu]
     [:button.btn.btn-outline-primary.mr-2.h-100
      {:class (when active-filters? "btn-outline-secondary active")}
      (labels :badges.filters/button)]
     {:hideOnClick :toggle}]))

(rf/reg-event-db
  :filters.activate/labels
  (fn [db [_ criteria label]]
    (let [new-filter {:type :labels
                      :criteria (keyword criteria)
                      :label label}]
      (update-in db [:discussion :filters] #(cset/union #{new-filter} %)))))

(rf/reg-event-db
  :filters.activate/type
  (fn [db [_ criteria stype]]
    (let [new-filter {:type :type
                      :criteria (keyword criteria)
                      :statement-type (keyword stype)}]
      (update-in db [:discussion :filters] #(cset/union #{new-filter} %)))))

(rf/reg-event-db
  :filters.activate/votes
  (fn [db [_ criteria votes-number]]
    (let [new-filter {:type :votes
                      :criteria criteria
                      :votes-number (js/parseInt votes-number)}]
      (update-in db [:discussion :filters] #(cset/union #{new-filter} %)))))

(rf/reg-event-db
  :filters/deactivate
  (fn [db [_ filter-data]]
    (update-in db [:discussion :filters] disj filter-data)))

(rf/reg-event-db
  :filters/clear
  (fn [db _]
    (assoc-in db [:discussion :filters] #{})))

(rf/reg-sub
  :filters/active
  (fn [db _]
    (get-in db [:discussion :filters] #{})))

(rf/reg-sub
  :filters/active?
  (fn [_]
    (rf/subscribe [:filters/active]))
  (fn [active-filters _]
    (seq active-filters)))

(defn- filter-to-fn
  "Returns the corresponding filter-fn for a data-representation of a filter."
  [{:keys [type criteria label statement-type votes-number]} local-votes]
  (cond
    (= type :labels)
    (let [coll-fn (if (= criteria :includes) filter remove)]
      (fn [statements] (coll-fn #(contains? (set (:statement/labels %)) label) statements)))
    (= type :type)
    (let [coll-fn (if (= criteria :is) filter remove)]
      (fn [statements] (coll-fn #(= (:statement/type %) statement-type) statements)))
    (= type :votes)
    ;; Calling symbol on the string does not help. Other solutions are hacky.
    (let [comp-fn (case criteria ">" > "=" = "<" <)
          ;; The deref needs to happen here, because it cant happen in a lazy seq.
          ;; Derefing in the component does not update the filters, when votes change.
          votes @local-votes]
      (fn [statements] (filter #(comp-fn (discussion-logic/calculate-votes % votes) votes-number) statements)))
    :else identity))

(defn filter-statements
  "Accepts a collection of statements and filters and applies them to the collection."
  [statements filters votes]
  (let [filter-fns (map #(filter-to-fn % votes) filters)]
    ;; Apply every filter-function to the statements before returning them
    (reduce (fn [statements filter-fn] (filter-fn statements)) statements filter-fns)))
