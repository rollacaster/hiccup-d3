(ns tech.thomas-sojka.hiccup-d3.app
  (:require [reagent.dom :as dom]))

(defn app []
  [:div "HI!"])

(dom/render [app] (js/document.getElementById "root"))

(defn init [])

