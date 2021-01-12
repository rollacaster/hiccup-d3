(ns tech.thomas-sojka.hiccup-d3.charts.voronoi
  (:require ["d3" :as d3])
  (:require-macros [tech.thomas-sojka.hiccup-d3.macros :as m]))

(def plain
  (m/build-chart
   "plain"
   (fn [data]
     (let [size 300
           delaunay (.from d3/Delaunay (clj->js data))
           voronoi (.voronoi delaunay (into-array [0 0 size size]))]
       [:svg {:viewBox (str 0 " " 0 " " size " " size)}
        [:path {:fill "transparent"
                :stroke "black"
                :d (.render voronoi)}]]))))

(def voronoi
  {:title "Voronoi"
   :load  (fn [] (js/Promise.resolve (map (fn [] [(rand 300) (rand 300)]) (range 100))))
   :charts [plain]})
