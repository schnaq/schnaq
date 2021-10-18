(ns schnaq.interface.components.buttons)

(defn anchor
  "Create a `a`-Tag styled as a button. By default, in primary colors."
  ([content]
   [anchor content "#"])
  ([content target]
   [anchor content target "btn-primary"])
  ([content target classes]
   [anchor content target classes nil])
  ([content target classes attrs]
   [:a.btn
    (cond->
      {:href target
       :role "button"
       :class classes}
      attrs (merge attrs))
    content]))

(defn anchor-big
  "The default big button as an anchor. By default, in primary colors. No target on default."
  ([content]
   [anchor-big content "#"])
  ([content target]
   [anchor-big content target "btn-primary"])
  ([content target classes]
   [anchor-big content target classes nil])
  ([content target classes attrs]
   [anchor content target (str "btn-lg " classes) attrs]))
