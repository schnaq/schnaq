(ns schnaq.websockets.handler-test
  (:require [clojure.test :refer [deftest testing is]]
            [schnaq.websockets.handler :as handler]
            [taoensso.timbre :as log]))

(defmethod handler/handle-message ::exploding [_message]
  (throw (ex-info "Boom" {:reason :test})))

(defmethod handler/handle-message ::silent [_message] nil)

(defmethod handler/handle-message ::answering [{:keys [?data]}]
  [::answer ?data])

(defn- receive-quietly
  "Call `receive-message!` without the expected error being logged."
  [message]
  (log/with-min-level :fatal
    (handler/receive-message! message)))

(deftest receive-message-swallows-exceptions-test
  (testing "A throwing handler must not kill sente's router loop."
    (let [replies (atom [])]
      (is (nil? (receive-quietly {:id ::exploding
                                  :?reply-fn #(swap! replies conj %)})))
      (is (empty? @replies) "No reply is sent when the handler blew up.")))
  (testing "Exceptions are also swallowed when no reply-fn is present."
    (is (nil? (receive-quietly {:id ::exploding})))))

(deftest receive-message-replies-test
  (testing "The handler's response is passed to the reply-fn."
    (let [replies (atom [])]
      (handler/receive-message! {:id ::answering
                                 :?data "huhu"
                                 :?reply-fn #(swap! replies conj %)})
      (is (= [[::answer "huhu"]] @replies))))
  (testing "A nil response does not trigger a reply."
    (let [replies (atom [])]
      (handler/receive-message! {:id ::silent
                                 :?reply-fn #(swap! replies conj %)})
      (is (empty? @replies))))
  (testing "A missing reply-fn is tolerated."
    (is (nil? (handler/receive-message! {:id ::answering :?data "huhu"})))))
