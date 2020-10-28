(ns schnaq.interface.views.common
  (:require ["jdenticon" :as jdenticon]
            [cljs.spec.alpha :as s]
            [ghostwheel.core :refer [>defn]]
            [goog.string :as gstring]
            [oops.core :refer [oget]]
            [reitit.frontend.easy :as reitfe]
            [schnaq.interface.text.display-data :refer [labels fa img-path video]]))

(>defn avatar
  "Create an image based on the nickname."
  [display-name size]
  [string? number? :ret vector?]
  [:div.d-flex.flex-row
   [:div.avatar-name.mr-4.align-self-end display-name]
   [:div.avatar-image.img-thumbnail.schnaq-rounded.align-self-end.p-0
    {:dangerouslySetInnerHTML {:__html (jdenticon/toSvg display-name size)}}]])

(>defn add-namespace-to-keyword
  "Prepend a namespace to a keyword. Replaces existing namespace with new
  namespace."
  [prepend-namespace to-keyword]
  [(s/or :keyword keyword? :string string?) keyword? :ret keyword?]
  (keyword (str (name prepend-namespace) "/" (name to-keyword))))

(defn tab-builder
  "Create a tabbed view. Prefix must be unique on this page."
  ([tab-prefix first-tab second-tab]
   (tab-builder tab-prefix first-tab second-tab nil))
  ([tab-prefix first-tab second-tab third-tab]
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
           (:link third-tab)])]]
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
          (:view third-tab)])]])))

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
