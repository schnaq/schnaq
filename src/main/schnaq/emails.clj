(ns schnaq.emails
  (:require [postal.core :refer [send-message]]
            [schnaq.config :as config]
            [taoensso.timbre :refer [info error]]))

(def ^:private mail-template "body der mail - sicher kein Spam!")
(def conn {:host (:sender-host config/email)
           :ssl true
           :user (:sender-address config/email)
           :pass (:sender-password config/email)})

(defn valid-mail
  "Check valid mail"
  [mail]
  (when (re-matches #"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,10}" mail)
    mail))

(defn send-one-mail [recipient]
  (if (valid-mail recipient)
    (try
      (send-message conn {:from (:sender-address config/email)
                          :to recipient
                          :subject "EIngeladen zum lullzen - vom Lellinger"
                          :body [{:type "text/html; charset=utf-8"
                                  :content mail-template}]})
      (info "Sent mail to" recipient)
      (Thread/sleep 500)
      (catch Exception _
        (error "Failed to send mail to" recipient)))
    :invalid-mail))

(defn send-all-mails [recipients]
  (run! send-one-mail recipients))

(comment
  (send-all-mails ["alexander@schneider.gg" "cmeter@gmail.com"])
  :end)