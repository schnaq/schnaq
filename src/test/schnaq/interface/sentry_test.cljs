(ns schnaq.interface.sentry-test
  (:require [cljs.test :refer [deftest testing is are]]
            [schnaq.interface.sentry :as sentry]))

(deftest request-path-test
  (testing "The query string is dropped, it carries share hashes and access codes."
    (are [uri path] (= path (sentry/request-path uri))
      "https://api.schnaq.com/schnaq/by-hash?share-hash=abc-123" "https://api.schnaq.com/schnaq/by-hash"
      "https://api.schnaq.com/schnaq/by-access-code?code=42" "https://api.schnaq.com/schnaq/by-access-code"
      "https://api.schnaq.com/discussion/graph?share-hash=a&display-name=Christian" "https://api.schnaq.com/discussion/graph"
      "https://api.schnaq.com/schnaq/by-hash" "https://api.schnaq.com/schnaq/by-hash"))
  (testing "Missing uris do not blow up."
    (are [uri] (nil? (sentry/request-path uri))
      nil
      ""
      "   ")))

(deftest scrub-url-test
  (testing "Share hashes in the path are masked, the query string is dropped."
    (are [uri scrubbed] (= scrubbed (sentry/scrub-url uri))
      "https://app.schnaq.com/schnaq/2d31377c-dc37-4ec8-a0a6-326aed49e35b" "https://app.schnaq.com/schnaq/2d31…e35b"
      "https://api.app.schnaq.com/discussion/graph?share-hash=a&display-name=Christian" "https://api.app.schnaq.com/discussion/graph"
      nil nil)))

(deftest scrub-breadcrumb-test
  (testing "Console breadcrumbs are dropped."
    (is (nil? (sentry/scrub-breadcrumb #js {:category "console" :message "secret"}))))
  (testing "URLs in breadcrumbs are scrubbed."
    (let [crumb (sentry/scrub-breadcrumb
                 #js {:category "navigation"
                      :data #js {:from "/schnaq/2d31377c-dc37-4ec8-a0a6-326aed49e35b"
                                 :to "/schnaq/2d31377c-dc37-4ec8-a0a6-326aed49e35b?code=42"}})]
      (is (= "/schnaq/2d31…e35b" (.. crumb -data -from)))
      (is (= "/schnaq/2d31…e35b" (.. crumb -data -to)))))
  (testing "Breadcrumbs without data pass through."
    (is (some? (sentry/scrub-breadcrumb #js {:category "ui.click"})))))

(deftest scrub-event-test
  (let [event (sentry/scrub-event
               #js {:request #js {:url "https://app.schnaq.com/schnaq/2d31377c-dc37-4ec8-a0a6-326aed49e35b"
                                  :headers #js {"Referer" "https://app.schnaq.com/schnaq/2d31377c-dc37-4ec8-a0a6-326aed49e35b"
                                                "User-Agent" "Safari"}}})]
    (is (= "https://app.schnaq.com/schnaq/2d31…e35b" (.. event -request -url)))
    (is (nil? (.. event -request -headers -Referer)))
    (is (= "Safari" (aget event "request" "headers" "User-Agent")))))

(deftest report-http-failure?-test
  (testing "Server errors are defects and get reported."
    (are [status] (sentry/report-http-failure? {:failure :error :status status})
      500 502 503))
  (testing "Client errors are answers, not defects."
    (are [status] (not (sentry/report-http-failure? {:failure :error :status status}))
      400 401 403 404 409))
  (testing "An unparseable response is a defect, even behind a 200."
    (is (sentry/report-http-failure? {:failure :parse :status 200})))
  (testing "An exception while building the request is a defect."
    (is (sentry/report-http-failure? {:failure :exception :status 0})))
  (testing "The client's network conditions are not reported."
    (are [failure] (not (sentry/report-http-failure? {:failure failure :status 0}))
      :aborted
      :timeout
      :failed))
  (testing "An unknown or missing failure type is not reported."
    (is (not (sentry/report-http-failure? {})))
    (is (not (sentry/report-http-failure? {:failure :something-new :status 0})))))
