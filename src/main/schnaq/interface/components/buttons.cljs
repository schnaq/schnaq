(ns schnaq.interface.components.buttons)

(defn anchor-big
  "The default big button as an anchor. By default, in primary colors. No target on default."
  ([content]
   [anchor-big content "#"])
  ([content target]
   [anchor-big content target "btn-primary"])
  ([content target classes]
   [anchor-big content target classes nil])
  ([content target classes attrs]
   [:a.btn.btn-lg
    (cond->
      {:href target
       :role "button"
       :class classes}
      attrs (merge attrs))
    content]))
