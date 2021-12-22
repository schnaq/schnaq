(ns schnaq.interface.components.videos)

(defn video
  "Returns a video path."
  [identifier]
  (identifier
   {:how-to.discussion/webm "https://s3.schnaq.com/schnaq-how-to/discussion.webm"
    :how-to.discussion/mp4 "https://s3.schnaq.com/schnaq-how-to/discussion.mp4"
    :how-to.pro-con/webm "https://s3.schnaq.com/schnaq-how-to/discussion-2.webm"
    :how-to.pro-con/mp4 "https://s3.schnaq.com/schnaq-how-to/discussion-2.mp4"
    :register.point-right/webm "https://s3.schnaq.com/schnaq-schnaqqifanten/schnaqqi_point_right.webm"
    :register.point-right/mp4 "https://s3.schnaq.com/schnaq-schnaqqifanten/schnaqqi_point_right.mp4"
    :register.point-right-christmas/webm "https://s3.schnaq.com/schnaq-schnaqqifanten/events/schnaqqi_point_right_christmas.webm"
    :register.point-right-christmas/mp4 "https://s3.schnaq.com/schnaq-schnaqqifanten/events/schnaqqi_point_right_christmas.mp4"
    :startpage.above-the-fold/webm "https://s3.schnaq.com/startpage/videos/above_the_fold.webm"
    :startpage.above-the-fold/mp4 "https://s3.schnaq.com/startpage/videos/above_the_fold.mp4"}))
