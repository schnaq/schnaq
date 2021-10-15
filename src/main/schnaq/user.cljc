(ns schnaq.user)

(defn statement-author
  "Returns the display-name of a statement author."
  [statement]
  (or
    (-> statement :statement/author :user.registered/display-name)
    (-> statement :statement/author :user/nickname)))

(defn display-name
  "Returns the correct display name, when input an anonymous or registered user."
  [user]
  (or
    (:user.registered/display-name user)
    (:user/nickname user)))