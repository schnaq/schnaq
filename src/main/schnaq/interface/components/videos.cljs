(ns schnaq.interface.components.videos)

(defn video
  "Returns a video path."
  [identifier]
  (identifier
   {:animation-discussion/webm "https://s3.schnaq.com/schnaq-how-to/animation_discussion.webm"
    :animation-discussion/mp4 "https://s3.schnaq.com/schnaq-how-to/animation_discussion.mp4"
    :celebration.schnaqqi/webm "https://s3.schnaq.com/schnaq-common/celebration/celebration_confetti.webm"
    :celebration.schnaqqi/mp4 "https://s3.schnaq.com/schnaq-common/celebration/celebration_confetti.mp4"
    :how-to.admin/webm "https://s3.schnaq.com/schnaq-how-to/admin.webm"
    :how-to.admin/mp4 "https://s3.schnaq.com/schnaq-how-to/admin.mp4"
    :how-to.create/webm "https://s3.schnaq.com/schnaq-how-to/create.webm"
    :how-to.create/mp4 "https://s3.schnaq.com/schnaq-how-to/create.mp4"
    :how-to.discussion/webm "https://s3.schnaq.com/schnaq-how-to/discussion.webm"
    :how-to.discussion/mp4 "https://s3.schnaq.com/schnaq-how-to/discussion.mp4"
    :how-to.pro-con/webm "https://s3.schnaq.com/schnaq-how-to/discussion-2.webm"
    :how-to.pro-con/mp4 "https://s3.schnaq.com/schnaq-how-to/discussion-2.mp4"
    :how-to.why/webm "https://s3.schnaq.com/schnaq-how-to/why.webm"
    :how-to.why/mp4 "https://s3.schnaq.com/schnaq-how-to/why.mp4"
    :register.point-right/webm "https://s3.schnaq.com/schnaq-schnaqqifanten/schnaqqi_point_right.webm"
    :register.point-right/mp4 "https://s3.schnaq.com/schnaq-schnaqqifanten/schnaqqi_point_right.mp4"
    :start-page.address-elephant/webm "https://s3.schnaq.com/schnaq-schnaqqifanten/wilfried.webm"
    :start-page.address-elephant/mp4 "https://s3.schnaq.com/schnaq-schnaqqifanten/wilfried.mp4"
    :start-page.questions/webm "https://s3.schnaq.com/startpage/discussion.webm"
    :start-page.questions/mp4 "https://s3.schnaq.com/startpage/discussion.mp4"
    :start-page.work-together/webm "https://s3.schnaq.com/schnaq-how-to/WorkTogether.webm"
    :start-page.work-together/mp4 "https://s3.schnaq.com/schnaq-how-to/WorkTogether.mp4"}))
