(ns tech.thomas-sojka.hiccup-d3.charts.treemap
  (:require ["d3" :as d3]
            [tech.thomas-sojka.hiccup-d3.utils :refer [fetch-json]])
  (:require-macros [tech.thomas-sojka.hiccup-d3.macros :as m]))

(def plain
  (m/build-chart
   "plain"
   (fn [data]
     (let [size 300
           color (d3/scaleOrdinal d3/schemeCategory10)
           root ((-> (d3/treemap)
                     (.tile d3/treemapBinary)
                     (.size (into-array [size size])))
                 (-> (d3/hierarchy data)
                     (.sum (fn [d] (.-value d)))
                     (.sort (fn [a b] (- (.-value b) (.-value a))))))]
       [:svg {:viewBox (str 0 " " 0 " " size " " size)}
        [:g
         (->> (.leaves root)
              (map (fn [d]
                     (let [parent-name (loop [d d]
                                         (if (> (.-depth d) 1)
                                           (recur (.-parent d))
                                           ^js (.-data.name d)))]
                       [:rect {:key ^js (.-data.name d)
                               :x (.-x0 d) :y (.-y0 d)
                               :width (- (.-x1 d) (.-x0 d))
                               :height (- (.-y1 d) (.-y0 d))
                               :stroke "black"
                               :fill (color parent-name)}]))))]]))))

(def treemap
  {:title "Treemap"
   :load  (fn [] (-> (fetch-json "data/flare-2.json")))
   :charts [plain]})
