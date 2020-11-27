(ns tech.thomas-sojka.hiccup-d3.app
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            ["d3" :as d3]))

(defn bar-chart []
  (let [data (r/atom [])]
    (-> (js/fetch "/data/frequencies.json")
        (.then (fn [res] (.json res)))
        (.then (fn [res] (reset! data (js->clj res :keywordize-keys true))))
        (.catch (fn [res] (prn res))))
    (fn []
      (let [size 300
            margin {:top 0 :right 0 :left 13 :bottom 0}
            x (-> (d3/scaleLinear)
                  (.range (clj->js [(:left margin) (- size (:right margin))]))
                  (.domain (clj->js [0 (apply max (map :frequency @data))])))
            y (-> (d3/scaleBand)
                  (.domain (clj->js (range (count @data))))
                  (.range (clj->js [(:top margin) (- size (:bottom margin))])))
            color (d3/scaleOrdinal d3/schemeCategory10)]
        [:svg {:viewBox (str "0 0 " size " " size)}
         (map-indexed
          (fn [idx {:keys [letter frequency]}]
            [:g {:key idx :transform (str "translate("0 "," (y idx) ")")}
             [:rect {:x (x 0)
                     :height (.bandwidth y)
                     :fill (color letter)
                     :width (x frequency)}]
             ])
          @data)
         (map-indexed
          (fn [idx {:keys [letter frequency]}]
            [:g {:key idx :transform (str "translate("0 "," (y idx) ")")}
             [:text {:style {:font-size 8}
                     :x 17
                     :y (/ (.bandwidth y) 2)
                     :dominant-baseline "middle"} (str frequency)]
             [:text.current-fill
              {:x 0 :y (/ (.bandwidth y) 2) :dominant-baseline "middle" :style {:font-size 10}}
              (str letter)]])
          @data)]))))

(defn app []
  [:div {:class "w-1/2 p-6"}
   [bar-chart]])

(dom/render [app] (js/document.getElementById "root"))

(defn init [])

