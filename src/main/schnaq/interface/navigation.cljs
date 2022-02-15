(ns schnaq.interface.navigation
  (:require [goog.dom :as gdom]
            [goog.string :as gstring]
            [oops.core :refer [oget oset!]]
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

(rf/reg-event-fx
 :navigation/navigated
 (fn [{:keys [db]} [_ new-match]]
   {:db (let [old-match (:current-route db)
              controllers (reitit-front-controllers/apply-controllers (:controllers old-match) new-match)]
          (assoc db :current-route (assoc new-match :controllers controllers)))
    :fx [[:navigation.navigated/write-hreflang]]}))

(rf/reg-fx
 :navigation.redirect/follow!
 (fn [redirect-url]
   (oset! js/window [:location :href] redirect-url)))

(rf/reg-event-fx
 :navigation.redirect/follow
 (fn [_ [_ {:keys [redirect]}]]
   {:fx [[:navigation.redirect/follow! redirect]]}))

(defn- replace-language-in-path
  "Side effect free replacement of language prefix in paths."
  [path locale]
  (let [current-url (new js/URL path)
        full-path (gstring/format "%s%s%s"
                                  (.-pathname current-url)
                                  (.-search current-url)
                                  (.-hash current-url))
        locale-clean-path (if (or (gstring/startsWith full-path "/en/")
                                  (gstring/startsWith full-path "/de/")
                                  (gstring/startsWith full-path "/pl/"))
                            (subs full-path 3)
                            full-path)]
    (if locale
      (gstring/format "/%s%s" (str (name locale)) locale-clean-path)
      locale-clean-path)))

(defn switch-language-href
  "Take the current path and return it with the desired locale.
  If no locale is provided the default local-less default path is returned.
  Is not pure, since it always depends on current URL."
  ([]
   (replace-language-in-path (oget js/window [:location :href]) nil))
  ([locale]
   (replace-language-in-path (oget js/window [:location :href]) locale)))

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

(rf/reg-fx
 :navigation.navigated/write-hreflang
 (fn []
   (let [origin (.-origin (new js/URL (oget js/window [:location :href])))
         head (first (gdom/getElementsByTagName "head"))
         all-links (gdom/getElementsByTagName "link" head)
         path-format "%s%s"
         existing-german-alternative (first (filter #(= "de" (.-hreflang %)) all-links))
         german-alternative
         (gdom/createDom "link" (clj->js {:rel "alternative"
                                          :hreflang "de"
                                          :href (gstring/format path-format origin (switch-language-href :de))}))
         existing-english-alternative (first (filter #(= "en" (.-hreflang %)) all-links))
         english-alternative
         (gdom/createDom "link" (clj->js {:rel "alternative"
                                          :hreflang "en"
                                          :href (gstring/format path-format origin (switch-language-href :en))}))
         existing-default-alternative (first (filter #(= "x-default" (.-hreflang %)) all-links))
         default-alternative
         (gdom/createDom "link" (clj->js {:rel "alternative"
                                          :hreflang "x-default"
                                          :href (gstring/format path-format origin (switch-language-href))}))]
     (if existing-german-alternative
       (gdom/replaceNode german-alternative existing-german-alternative)
       (gdom/appendChild head german-alternative))
     (if existing-english-alternative
       (gdom/replaceNode english-alternative existing-english-alternative)
       (gdom/appendChild head english-alternative))
     (if existing-default-alternative
       (gdom/replaceNode default-alternative existing-default-alternative)
       (gdom/appendChild head default-alternative)))))
