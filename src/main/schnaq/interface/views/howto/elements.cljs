(ns schnaq.interface.views.howto.elements
  (:require [clojure.set :as cset]
            [hodgepodge.core :refer [local-storage]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [fa labels img-path video]]
            [schnaq.interface.utils.localstorage :as ls]
            [schnaq.interface.views.common :as common]))

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
        hide? (contains? hidden-tags hide-tag)]
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
           [:p (labels :how-to/question-dont-show-again)
            [:btn.btn-link.clickable-no-hover
             {:on-click (fn [] (rf/dispatch [:how-to-visibility/to-localstorage hide-tag]))}
             (labels :how-to/answer-dont-show-again)]]]]
         [:div.col-12.col-lg-4.offset-lg-1
          [:div.text-right
           [:button.btn-rounded-2.btn-outline-secondary.mb-3
            {:on-click (fn [] (rf/dispatch [:how-to-visibility/to-localstorage hide-tag]))}
            [:i {:class (str "m-auto fas " (fa :cross))}]]]
          [:img.taskbar-background {:src (img-path :how-to/taskbar)}]
          [:video.video-scalable {:auto-play true :loop true :muted true :plays-inline true}
           [:source {:src (video video-key-webm) :type "video/webm"}]
           [:source {:src (video vide-key-webm) :type "video/mp4"}]]]]]])))

;; subs

(rf/reg-event-fx
  :how-to-visibility/to-localstorage
  (fn [{:keys [db]} [_ how-to-id]]
    ;; PARTIALLY DEPRECATED FROM 2021-09-22: Remove old add-to-and-build-… stuff and use normal set
    (let [deprecated-set (->> (ls/get-item :how-to/disabled)
                              ls/parse-string-as-set
                              (map #(keyword (if (= ":" (first %))
                                               (subs % 1) %)))
                              (into #{}))
          disabled-opts (conj (set (:how-to/disabled local-storage)) how-to-id)
          merged-opts (cset/union deprecated-set disabled-opts)]
      {:db (assoc-in db [:how-to :disabled] merged-opts)
       :fx [[:localstorage/assoc [:how-to/disabled merged-opts]]]})))

(rf/reg-event-db
  :how-to-visibility/from-localstorage-to-app-db
  (fn [db _]
    (assoc-in db [:how-to :disabled] (or (:how-to/disabled local-storage)
                                         (ls/parse-string-as-set (ls/get-item :how-to/disabled))))))

(rf/reg-sub
  :how-to-visibility/hidden-tags
  (fn [db _]
    (get-in db [:how-to :disabled])))

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