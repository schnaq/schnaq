(ns schnaq.interface.components.buttons)

(defn big
  "The default big button. By default in primary colors. And with the submit type without on-click."
  ([content]
   [big content "btn-primary"])
  ([content classes]
   [big content classes "submit"])
  ([content classes btn-type]
   [:button.btn.btn-lg
    {:type btn-type
     :class classes}
    content]))
