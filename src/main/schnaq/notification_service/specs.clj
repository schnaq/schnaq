(ns schnaq.notification-service.specs
  (:require [clojure.spec.alpha :as s]
            [schnaq.database.specs :as specs])
  (:import (java.time ZonedDateTime)))

(s/def :time/zoned-date-time (partial instance? ZonedDateTime))

(s/def :notification-service/discussions-with-new-statements (s/coll-of ::specs/discussion))
(s/def :notification-service/user-with-changed-discussions
  (s/merge ::specs/registered-user (s/keys :req-un [:notification-service/discussions-with-new-statements])))
(s/def :notification-service/share-hash-to-discussion
  (s/map-of :discussion/share-hash ::specs/discussion))
