(ns schnaq.interface.views.common
  (:require ["jdenticon" :as jdenticon]
            [cljs.spec.alpha :as s]
            [ghostwheel.core :refer [>defn]]
            [goog.string :as gstring]
            [oops.core :refer [oget oset!]]
            [reitit.frontend.easy :as reitfe]))

(>defn avatar
  "Create an image based on the nickname."
  [display-name size]
  [string? number? :ret vector?]
  [:div.text-center
   [:div.avatar-image.m-auto.schnaq-rounded.p-0
    {:dangerouslySetInnerHTML {:__html (jdenticon/toSvg display-name size (clj->js {:backColor "#fff"}))}}]
   [:p.small.mt-1 display-name]])

(>defn add-namespace-to-keyword
  "Prepend a namespace to a keyword. Replaces existing namespace with new
  namespace."
  [prepend-namespace to-keyword]
  [(s/or :keyword keyword? :string string?) keyword? :ret keyword?]
  (keyword (str (name prepend-namespace) "/" (name to-keyword))))

(defn tab-builder
  "Create a tabbed view. Prefix must be unique on this page."
  ([tab-prefix first-tab second-tab]
   (tab-builder tab-prefix first-tab second-tab nil nil))
  ([tab-prefix first-tab second-tab third-tab fourth-tab]
   (let [tab-prefix# (str "#" tab-prefix)]
     [:<>
      [:nav.nav-justified
       [:div.nav.nav-tabs {:role "tablist"}
        [:a.nav-item.nav-link.active {:data-toggle "tab"
                                      :href (str tab-prefix# "-home")
                                      :role "tab"
                                      :aria-controls (str tab-prefix "-home")
                                      :aria-selected "true"}
         (:link first-tab)]
        [:a.nav-item.nav-link {:data-toggle "tab"
                               :href (str tab-prefix# "-link")
                               :role "tab"
                               :aria-controls (str tab-prefix "-link")
                               :aria-selected "false"}
         (:link second-tab)]
        (when third-tab
          [:a.nav-item.nav-link {:data-toggle "tab"
                                 :href (str tab-prefix# "-link-3")
                                 :role "tab"
                                 :aria-controls (str tab-prefix "-link-3")
                                 :aria-selected "false"}
           (:link third-tab)])
        (when fourth-tab
          [:a.nav-item.nav-link {:data-toggle "tab"
                                 :href (str tab-prefix# "-link-4")
                                 :role "tab"
                                 :aria-controls (str tab-prefix "-link-4")
                                 :aria-selected "false"}
           (:link fourth-tab)])]]
      [:div.tab-content.mt-5
       [:div.tab-pane.fade.show.active
        {:id (str tab-prefix "-home")
         :role "tabpanel" :aria-labelledby (str tab-prefix "-home-tab")}
        (:view first-tab)]
       [:div.tab-pane.fade
        {:id (str tab-prefix "-link")
         :role "tabpanel" :aria-labelledby (str tab-prefix "-link-tab")}
        (:view second-tab)]
       (when third-tab
         [:div.tab-pane.fade
          {:id (str tab-prefix "-link-3")
           :role "tabpanel" :aria-labelledby (str tab-prefix "-link-tab-3")}
          (:view third-tab)])
       (when fourth-tab
         [:div.tab-pane.fade
          {:id (str tab-prefix "-link-4")
           :role "tabpanel" :aria-labelledby (str tab-prefix "-link-tab-4")}
          (:view fourth-tab)])]])))

(>defn get-share-link
  [current-route]
  [map? :ret string?]
  (let [share-hash (-> current-route :path-params :share-hash)
        path (reitfe/href :routes.meeting/show {:share-hash share-hash})
        location (oget js/window :location)]
    (gstring/format "%s//%s%s" (oget location :protocol) (oget location :host) path)))

(>defn get-admin-center-link
  "Building the current URL with validated path, and without extra-stuff, like
  internal hashtag-routing."
  [current-route]
  [map? :ret string?]
  (let [{:keys [share-hash edit-hash]} (:path-params current-route)
        path (reitfe/href :routes.meeting/admin-center {:share-hash share-hash
                                                        :edit-hash edit-hash})
        location (oget js/window :location)]
    (gstring/format "%s//%s%s" (oget location :protocol) (oget location :host) path)))

(>defn set-website-title!
  "Set a document's website title."
  [title]
  [string? :ret nil?]
  (let [new-title (gstring/format "schnaq - %s" title)]
    (oset! js/document [:title] new-title)))
