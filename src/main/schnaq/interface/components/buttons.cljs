(ns schnaq.interface.components.buttons)

(defn big
  "The default big button. By default in primary colors. And with the submit type without on-click."
  ([content]
   [big content "btn-primary"])
  ([content classes]
   [big content classes nil])
  ([content classes attrs]
   [big content classes attrs nil])
  ([content classes attrs on-click-fn]
   [:button.btn.btn-lg
    (cond->
      {:type "submit"
       :class classes}
      on-click-fn (assoc :on-click on-click-fn)
      attrs (merge attrs))
    content]))
