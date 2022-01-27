(ns schnaq.notification-service.mail-builder
  (:require [com.fulcrologic.guardrails.core :refer [>defn- >defn]]
            [hiccup.util :as hiccup-util]
            [schnaq.links :as schnaq-links]
            [schnaq.mail.template :as template]
            [schnaq.notification-service.specs]))

(>defn- build-new-statements-content
  "Additional content to display the number of new statements and a navigation button
  to the corresponding schnaq. This functions maps over all schnaqs."
  [user content-fn]
  [:notification-service/user-with-changed-discussions fn? :ret string?]
  (reduce
   str
   (map (fn [{:keys [discussion/share-hash discussion/title new-statements]}]
          (let [discussion-title (hiccup-util/escape-html title)
                new-statements-text (if (= 1 new-statements)
                                      (str new-statements " neuer Beitrag")
                                      (str new-statements " neue Beiträge"))]
            (content-fn discussion-title new-statements-text share-hash)))
        (:discussions-with-new-statements user))))

(>defn build-new-statements-html
  "New statements info as html. Preparation for sending it via mail."
  [user]
  [:notification-service/user-with-changed-discussions :ret string?]
  (build-new-statements-content
   user
   (fn [title text discussion-hash]
     (template/mail-content-left-button-right
      title text "Zum schnaq" (schnaq-links/get-share-link discussion-hash)))))

(>defn build-new-statements-plain
  "New statements info as plain text. Preparation for a standard mail without 
   HTML."
  [user]
  [:notification-service/user-with-changed-discussions :ret string?]
  (build-new-statements-content
   user
   (fn [title text discussion-hash]
     (format "%s in %s: %s\n" text title (schnaq-links/get-share-link discussion-hash)))))

(defn build-personal-greeting
  "Takes the user's display name and creates a salutation."
  [{:user.registered/keys [display-name]}]
  (format "Hallo %s," (hiccup-util/escape-html display-name)))

(>defn build-number-unseen-statements
  "Sum up all new statements over all discussions and put the sum in a text 
   body."
  [total-new-statements]
  [nat-int? :ret string?]
  (let [statements-text (if (= 1 total-new-statements)
                          "einen neuen Beitrag"
                          (str total-new-statements " neue Beiträge"))]
    (format "es gibt %s in deinen besuchten schnaqs!" statements-text)))
