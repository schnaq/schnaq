(ns schnaq.interface.views.brainstorm.tools)

(defn is-brainstorm?
  "Check if current schnaq is a brainstorm."
  [{:keys [meeting/type]}]
  (= :meeting.type/brainstorm type))