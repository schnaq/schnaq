(ns schnaq.interface.navigation
  (:require [goog.string :as gstring]
            [oops.core :refer [oset!]]
            [re-frame.core :as rf]
            [reitit.frontend.controllers :as reitit-front-controllers]
            [reitit.frontend.easy :as reitit-front-easy]))

(rf/reg-sub
 :navigation/current-route
 (fn [db]
   (:current-route db)))

(rf/reg-sub
 :navigation/current-route-name
 (fn [db]
   (get-in db [:current-route :data :name])))

(rf/reg-event-fx
 :navigation/navigate
 (fn [_cofx [_ & route]]
   {:fx [[:navigation/navigate! route]]}))

(rf/reg-fx
 :navigation/navigate!
 (fn [route]
   (apply reitit-front-easy/push-state route)))

(rf/reg-event-db
 :navigation/navigated
 (fn [db [_ new-match]]
   (let [old-match (:current-route db)
         controllers (reitit-front-controllers/apply-controllers (:controllers old-match) new-match)]
     (assoc db :current-route (assoc new-match :controllers controllers)))))

(rf/reg-fx
 :navigation.redirect/follow!
 (fn [redirect-url]
   (oset! js/window [:location :href] redirect-url)))

(rf/reg-event-fx
 :navigation.redirect/follow
 (fn [_ [_ {:keys [redirect]}]]
   {:fx [[:navigation.redirect/follow! redirect]]}))

(defn href
  "A drop-in replacement for `reitit.frontend.easy/href` that is aware of schnaqs language path-prefix.
  Looks up the language set and always adds the prefix to the path."
  ([route-name]
   (href route-name nil nil))
  ([route-name params]
   (href route-name params nil))
  ([route-name params query]
   (let [route-match (reitit-front-easy/href route-name params query)
         language-prefix (str (name @(rf/subscribe [:current-locale])))
         prefixed-path (gstring/format "/%s%s" language-prefix route-match)]
     prefixed-path)))
