(ns schnaq.interface.components.inputs
  "A number of schnaq-typical inputs.")

(defn text
  ([placeholder]
   [text placeholder nil])
  ([placeholder id]
   [:input.form-control.my-1
    (cond-> {:type "text"
             :placeholder placeholder}
      id (assoc :id id))]))
