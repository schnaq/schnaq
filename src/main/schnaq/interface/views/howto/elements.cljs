(ns schnaq.interface.views.howto.elements
  (:require [schnaq.interface.text.display-data :refer [fa labels img-path video]]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.views.common :as common]
            [schnaq.interface.utils.localstorage :as ls]
            [re-frame.core :as rf]))

(defn text-box
  "Text box with title and a body."
  [title body]
  [:article.feature-text-box.pb-5
   [:h5 (labels title)]
   [:p (labels body)]])

(defn feature-row-video-left
  "Feature row where the video is located on the right side."
  [video-key-webm vide-key-webm title body]
  [:div.row.align-items-center.feature-row
   [:div.col-12.col-lg-6
    [:img.taskbar-background {:src (img-path :how-to/taskbar)}]
    [:video.w-100.how-to-animations {:auto-play true :loop true :muted true :plays-inline true}
     [:source {:src (video video-key-webm) :type "video/webm"}]
     [:source {:src (video vide-key-webm) :type "video/mp4"}]]]
   [:div.col-12.col-lg-5.offset-lg-1
    [text-box title body]]])

(defn feature-row-video-right
  "Feature row where the video is located on the right side."
  [video-key-webm vide-key-webm title body]
  [:div.row.align-items-center.feature-row
   [:div.col-12.col-lg-5
    [text-box title body]]
   [:div.col-12.col-lg-6.offset-lg-1
    [:img.taskbar-background {:src (img-path :how-to/taskbar)}]
    [:video.w-100.how-to-animations {:auto-play true :loop true :muted true :plays-inline true}
     [:source {:src (video video-key-webm) :type "video/webm"}]
     [:source {:src (video vide-key-webm) :type "video/mp4"}]]]])

(defn- quick-how-to
  "Feature row where the video is located on the right side."
  [video-key-webm vide-key-webm title body hide-tag]
  (let [hidden-tags @(rf/subscribe [:how-to-visibility/hidden-tags])
        hide?   (contains? hidden-tags (str hide-tag))]
    (when-not hide?
      [common/delayed-fade-in
       [:div.quick-how-to
        [:div.row.align-items-center
         [:div.col-12.col-lg-7
          [:div.mb-2 [:i {:class (str "m-auto fas " (fa :info))}]]
          [text-box title body]
          [:div.feature-text-box
           [:p (labels :how-to/ask-question-2)
            [:a {:href (reitfe/href :routes/how-to)}
             (labels :how-to/answer-question)]]
           [:button.btn.button-secondary-small
            {:on-click (fn [] (rf/dispatch [:how-to-visibility/to-localstorage hide-tag]))}
            [:p.card-text (labels :how-to/answer-dont-show-again)]]]]
         [:div.col-12.col-lg-4.offset-lg-1
          [:img.taskbar-background {:src (img-path :how-to/taskbar)}]
          [:video.video-scalable {:auto-play true :loop true :muted true :plays-inline true}
           [:source {:src (video video-key-webm) :type "video/webm"}]
           [:source {:src (video vide-key-webm) :type "video/mp4"}]]]]]])))

;; subs

(rf/reg-event-fx
  :how-to-visibility/to-localstorage
  (fn [_ [_ how-to-id]]
    {:fx [[:localstorage/write
           [:how-to/disabled
            (ls/add-to-and-build-set-from-local-storage :how-to/disabled how-to-id)]]
          [:dispatch [:how-to-visibility/from-localstorage-to-app-db]]]}))

(rf/reg-event-db
  :how-to-visibility/from-localstorage-to-app-db
  (fn [db _]
    (assoc-in db [:how-to/disabled]
              (ls/parse-string-as-set (ls/get-item :how-to/disabled)))))

(rf/reg-sub
  :how-to-visibility/hidden-tags
  (fn [db _] (get-in db [:how-to/disabled])))

(defn quick-how-to-create []
  [quick-how-to
   :how-to.create/webm
   :how-to.create/mp4
   :how-to.create/title
   :how-to.create/body
   :how-to/create])

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