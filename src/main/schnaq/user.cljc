(ns schnaq.user
  (:require #?(:cljs [cljs.spec.alpha :as s]
               :clj [clojure.spec.alpha :as s])
            #?(:cljs [goog.string :refer [format]])
            [com.fulcrologic.guardrails.core :refer [>defn ?]]
            [schnaq.config.shared :refer [feature-limits]]
            [schnaq.database.specs :as specs]
            [schnaq.shared-toolbelt :as shared-tools]
            [taoensso.timbre :as log]))

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
  [(? ::specs/registered-user) ::specs/feature-limits => (? (s/or :boolean boolean? :number number?))]
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
