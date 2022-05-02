(ns schnaq.interface.components.tiptap
  (:require ["@tiptap/react" :refer [useEditor EditorContent]]
            ["@tiptap/starter-kit" :as StarterKit]))

;; import React from 'react'
;; import {useEditor, EditorContent} from '@tiptap/react'
;; import StarterKit from '@tiptap/starter-kit'
;; import './styles.scss'

(defn TipTap []
  (let
   [editor (useEditor
            {:extensions #js [StarterKit],
             :content "<p>Hello World!</p>"})]
    #_[:div #_[MenuBar {:editor editor}]]
    [:> EditorContent {:editor editor}]))

;; -----------------------------------------------------------------------------

(comment

  (r/as-element)

  nil)

(defn- build-page []
  [:h1 "TipTap"
   [:f> TipTap]])

(defn page []
  [build-page])
