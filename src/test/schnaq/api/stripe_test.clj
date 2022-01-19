(ns schnaq.api.stripe-test
  (:require [clojure.test :refer [deftest are testing]]
            [schnaq.api.subscription.stripe :as stripe]))

(def too-old-signature "t=1642094299,v1=4dbf7b73c15197e2e8c7d4b8be81e2b78cb41c83d9169b5062b05b195d692ba9,v0=ac62e25b8377b61abc0fd2f66490470c1cde384d699f927c0bc83cf51d291ca7")
(def valid-body "{\n  \"id\": \"evt_3KHWwFFrKCGqvoMo09zWACso\",\n  \"object\": \"event\",\n  \"api_version\": \"2020-08-27\",\n  \"created\": 1642094295,\n  \"data\": {\n    \"object\": {\n      \"id\": \"pi_3KHWwFFrKCGqvoMo0WZz3a05\",\n      \"object\": \"payment_intent\",\n      \"amount\": 2000,\n      \"amount_capturable\": 0,\n      \"amount_received\": 0,\n      \"application\": null,\n      \"application_fee_amount\": null,\n      \"automatic_payment_methods\": null,\n      \"canceled_at\": null,\n      \"cancellation_reason\": null,\n      \"capture_method\": \"automatic\",\n      \"charges\": {\n        \"object\": \"list\",\n        \"data\": [\n\n        ],\n        \"has_more\": false,\n        \"total_count\": 0,\n        \"url\": \"/v1/charges?payment_intent=pi_3KHWwFFrKCGqvoMo0WZz3a05\"\n      },\n      \"client_secret\": \"pi_3KHWwFFrKCGqvoMo0WZz3a05_secret_LJMx5vKZdxn9LG33ymICgjlf6\",\n      \"confirmation_method\": \"automatic\",\n      \"created\": 1642094295,\n      \"currency\": \"usd\",\n      \"customer\": \"cus_KxRppMPnSP3VPL\",\n      \"description\": \"Subscription creation\",\n      \"invoice\": \"in_1KHWwFFrKCGqvoMobs1tOcIo\",\n      \"last_payment_error\": null,\n      \"livemode\": false,\n      \"metadata\": {\n      },\n      \"next_action\": null,\n      \"on_behalf_of\": null,\n      \"payment_method\": null,\n      \"payment_method_options\": {\n        \"card\": {\n          \"installments\": null,\n          \"network\": null,\n          \"request_three_d_secure\": \"automatic\"\n        }\n      },\n      \"payment_method_types\": [\n        \"card\"\n      ],\n      \"processing\": null,\n      \"receipt_email\": null,\n      \"review\": null,\n      \"setup_future_usage\": \"off_session\",\n      \"shipping\": null,\n      \"source\": null,\n      \"statement_descriptor\": null,\n      \"statement_descriptor_suffix\": null,\n      \"status\": \"requires_payment_method\",\n      \"transfer_data\": null,\n      \"transfer_group\": null\n    }\n  },\n  \"livemode\": false,\n  \"pending_webhooks\": 2,\n  \"request\": {\n    \"id\": \"req_8I7obnsHybTkbz\",\n    \"idempotency_key\": \"55532db7-035c-4e3d-b5b4-706e40cb9c61\"\n  },\n  \"type\": \"payment_intent.created\"\n}")

(deftest verify-signature-test
  (testing "We can't create valid signatures from stripe. But we can check that we catch invalid requests."
    (let [verify-signature #'stripe/verify-signature]
      (are [request] (= :stripe.verification/invalid-signature (:error (verify-signature request)))
        {:body valid-body
         :headers {"stripe-signature" too-old-signature}}
        {:body ""
         :headers {"stripe-signature" too-old-signature}}
        {:body valid-body
         :headers {"stripe-signature" (str too-old-signature "broken")}}))))
