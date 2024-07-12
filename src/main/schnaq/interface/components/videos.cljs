(ns schnaq.interface.components.videos)

(defn video
  "Returns a video path."
  [identifier]
  (identifier
   {:register.point-right/webm "https://snq-common.s3.nl-ams.scw.cloud/schnaqqifanten/schnaqqi_point_right.webm"
    :register.point-right/mp4 "https://snq-common.s3.nl-ams.scw.cloud/schnaqqifanten/schnaqqi_point_right.mp4"
    :register.point-right-christmas/webm "https://snq-common.s3.nl-ams.scw.cloud/schnaqqifanten/events/schnaqqi_point_right_christmas.webm"
    :register.point-right-christmas/mp4 "https://snq-common.s3.nl-ams.scw.cloud/schnaqqifanten/events/schnaqqi_point_right_christmas.mp4"}))
