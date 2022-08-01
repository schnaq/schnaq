(ns schnaq.user
  (:require #?(:cljs [cljs.spec.alpha :as s]
               :clj [clojure.spec.alpha :as s])
            #?(:cljs [goog.string :refer [format]])
            [com.fulcrologic.guardrails.core :refer [>defn ?]]
            [schnaq.database.specs :as specs]
            [schnaq.shared-toolbelt :as shared-tools]
            [taoensso.timbre :as log]))

(def ^:private feature-limits
  "Describe default tier limits. If no value is provided, the user has unlimited
  access to this feature."
  {:free {:wordcloud? false
          :rankings? false
          :embeddings? false
          :theming? false
          :polls 0
          :concurrent-users 100
          :total-schnaqs 10
          :posts-per-schnaq 30}
   :pro {:concurrent-users 250}})

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

(>defn feature-limit
  "Lookup if there are limits for a user and a given feature."
  [{:user.registered/keys [roles] :as user} feature]
  [(? ::specs/any-user) ::specs/feature-limits => (s/or :boolean boolean? :number number? :nil nil?)]
  (when user
    (let [admin? (shared-tools/admin? roles)
          pro? (shared-tools/pro-user? roles)
          fq-feature (keyword "user.registered.features" (str (name feature)))]
      (if (s/valid? ::specs/feature-limits feature)
        (cond
          admin? nil
          pro? (or (get user fq-feature) (get-in feature-limits [:pro feature]))
          :else (or (get user fq-feature) (get-in feature-limits [:free feature])))
        (throw
         (let [valid-values (s/form ::specs/feature-limits)
               error-msg (format "Your queried feature is not defined. Queried: %s, valid values: %s" feature valid-values)]
           (log/error error-msg)
           (ex-info (format "Your queried feature is not defined. Queried: %s, valid values: %s" feature valid-values)
                    {:valid valid-values :queried feature})))))))

(>defn usage-warning-level
  "Calculate when which warning level should be displayed to the user."
  [user feature current]
  [(? ::specs/registered-user) ::specs/feature-limits (? nat-int?) => (? ::specs/warning-levels)]
  (when user
    (when-let [limit (feature-limit user feature)]
      (when-not (zero? limit)
        (let [used (/ current limit)]
          (cond
            (<= 0.75 used) :danger
            (<= 0.5 used) :warning))))))

(>defn warning-level-class [level]
  [(? ::specs/warning-levels) => (? string?)]
  (case level
    :warning "text-warning"
    :danger "text-danger"
    ""))

(>defn posts-limit-reached?
  "Check if the user's posts limit is reached for the provided schnaq."
  [author schnaq]
  [::specs/registered-user ::specs/discussion => boolean?]
  (let [statement-count (get-in schnaq [:meta-info :all-statements])
        limit (feature-limit author :posts-per-schnaq)]
    (if limit
      (>= statement-count limit)
      false)))

(>defn total-schnaqs-reached?
  "Check if the user's schnaq limit is reached."
  [author total-schnaqs]
  [(? ::specs/registered-user) (? nat-int?) => (? boolean?)]
  (when (and author total-schnaqs)
    (let [limit (feature-limit author :total-schnaqs)]
      (if limit
        (>= total-schnaqs limit)
        false))))
