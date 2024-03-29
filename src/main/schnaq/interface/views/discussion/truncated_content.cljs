(ns schnaq.interface.views.discussion.truncated-content
  (:require   [re-frame.core :as rf]
              [schnaq.interface.components.icons :refer [icon]]
              [schnaq.interface.translations :refer [labels]]
              [schnaq.interface.utils.markdown :as md]
              [schnaq.interface.utils.toolbelt :as tools]))

(def ^:private statement-max-char-length 280)

(defn- statement-collapsible-content [statement]
  (let [statement-id (:db/id statement)
        full-content (:statement/content statement)
        short-content (tools/truncate-to-n-chars-string full-content statement-max-char-length)
        collapsed? @(rf/subscribe [:toggle-statement-content/collapsed? statement-id])
        display-content (if collapsed? short-content full-content)
        button-content (if collapsed?
                         [:<> [icon :collapse-up "my-auto me-2"]
                          (labels :qanda.button.show/statement)]
                         [:<> [icon :collapse-down "my-auto me-2"]
                          (labels :qanda.button.hide/statement)])]
    [:<>
     [md/as-markdown display-content]
     [:button.btn.btn-transparent.border-0.p-0.mt-n3
      {:on-click #(rf/dispatch [:toggle-statement-content/collapse!
                                statement-id (not collapsed?)])}
      button-content]]))

(defn statement
  "When a statement's content is more than our defined max char length
   the content is displayed in a truncated view with an option to 
   toggle between the truncated length and full length of its content"
  [statement]
  (let [content (:statement/content statement)]
    (if (> (count content) statement-max-char-length)
      [statement-collapsible-content statement]
      [md/as-markdown content])))

(rf/reg-event-db
 :toggle-statement-content/collapse!
 (fn [db [_ statement-id collapsed?]]
   (assoc-in db [:statements :statement-content-collapsed? statement-id] collapsed?)))

(rf/reg-event-db
 ;; clear the statement-content-collapsed map to reset any collapsed content
 :toggle-statement-content/clear!
 (fn [db _]
   (assoc-in db [:statements :statement-content-collapsed?] {})))

(rf/reg-sub
 :toggle-statement-content/collapsed?
 (fn [db [_ statement-id]]
   (get-in db [:statements :statement-content-collapsed? statement-id] true)))
