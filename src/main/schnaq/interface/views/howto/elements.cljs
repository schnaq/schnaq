(ns schnaq.interface.views.howto.elements
  (:require [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]
            [schnaq.interface.components.icons :refer [icon]]
            [schnaq.interface.components.images :refer [img-path]]
            [schnaq.interface.components.motion :as motion]
            [schnaq.interface.components.videos :refer [video]]
            [schnaq.interface.translations :refer [labels]]))

(defn text-box
  "Text box with title and a body."
  [title body]
  [:article.feature-text-box.pb-3
   [:h5 (labels title)]
   (labels body)])

(defn- quick-how-to
  "Feature row where the video is located on the right side."
  [video-key-webm vide-key-webm title body hide-tag]
  (let [hidden-tags @(rf/subscribe [:how-to-visibility/hidden-tags])
        hide? (contains? hidden-tags hide-tag)]
    (when-not hide?
      [motion/fade-in-and-out
       [:article.quick-how-to
        [:div.row.align-items-center
         [:div.col-12.col-lg-7
          [:div.mb-2 [icon :info-question "m-auto"]]
          [text-box title body]
          [:div.feature-text-box
           [:p (labels :how-to/question-dont-show-again)
            [:button.btn.btn-link
             {:on-click (fn [] (rf/dispatch [:how-to-visibility/to-localstorage hide-tag]))}
             (labels :how-to/answer-dont-show-again)]]]]
         [:div.col-12.col-lg-4.offset-lg-1
          [:div.text-right
           [:button.btn.btn-outline-dark.mb-3
            {:on-click (fn [] (rf/dispatch [:how-to-visibility/to-localstorage hide-tag]))}
            [icon :cross "m-auto"]]]
          [:img.taskbar-background {:src (img-path :how-to/taskbar)}]
          [:video.video-scalable-with-shadow-and-border {:auto-play true :loop true :muted true :plays-inline true}
           [:source {:src (video video-key-webm) :type "video/webm"}]
           [:source {:src (video vide-key-webm) :type "video/mp4"}]]]]]])))

;; -----------------------------------------------------------------------------

(rf/reg-event-fx
 :how-to-visibility/to-localstorage
 (fn [{:keys [db]} [_ how-to-id]]
   (let [disabled-opts (conj (set (:how-to/disabled local-storage)) how-to-id)]
     {:db (assoc-in db [:how-to :disabled] disabled-opts)
      :fx [[:localstorage/assoc [:how-to/disabled disabled-opts]]]})))

(rf/reg-event-db
 :how-to-visibility/from-localstorage-to-app-db
 (fn [db _]
   (assoc-in db [:how-to :disabled] (:how-to/disabled local-storage))))

(rf/reg-sub
 :how-to-visibility/hidden-tags
 (fn [db _]
   (get-in db [:how-to :disabled])))

(defn quick-how-to-schnaq []
  [quick-how-to
   :how-to.discussion/webm
   :how-to.discussion/mp4
   :how-to.schnaq/title
   :how-to.schnaq/body
   :how-to/schnaq])

(defn quick-how-to-pro-con []
  [quick-how-to
   :how-to.pro-con/webm
   :how-to.pro-con/mp4
   :how-to.pro-con/title
   :how-to.pro-con/body
   :how-to/pro-con])
