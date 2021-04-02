(ns schnaq.interface.utils.file-reader
  "Functions based on https://github.com/jtkDvlp/re-frame-readfile-fx"
  (:require
    [cljs.core.async :as async :refer [go]]
    [re-frame.core :as re-frame]))

(defn- do-readfile!
  [file]
  (let [result (async/promise-chan)]
    (try
      (let [meta
            {:name (.-name file)
             :size (.-size file)
             :type (.-type file)
             :last-modified (.-lastModified file)}

            reader (js/FileReader.)

            on-loaded
            (fn [_]
              (->> (.-result reader)
                   (assoc meta :content)
                   (async/put! result))
              (async/close! result))]

        (.addEventListener reader "load" on-loaded)

        (.readAsDataURL reader file))

      (catch js/Object error
        (async/put! result {:error error :file file})
        (async/close! result)))

    result))

(re-frame/reg-fx
  :readfile
  (fn readfile-fx
    [{:keys [files on-success on-error]}]
    (go
      (let [contents
            (->> (mapv do-readfile! files)
                 (async/map vector)
                 (async/<!))

            errors
            (filter :error contents)]

        (if (seq errors)
          (re-frame/dispatch (conj on-error contents))
          (re-frame/dispatch (conj on-success contents)))))))