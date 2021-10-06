(ns schnaq.interface.components.buttons)

(defn a-big
  "The default big button as an anchor. By default in primary colors. No target on default."
  ([content]
   [a-big content "#"])
  ([content target]
   [a-big content target "btn-primary"])
  ([content target classes]
   [a-big content target classes nil])
  ([content target classes attrs]
   [:a.btn.btn-lg
    (cond->
      {:href target
       :type "button"
       :class classes}
      attrs (merge attrs))
    content]))
