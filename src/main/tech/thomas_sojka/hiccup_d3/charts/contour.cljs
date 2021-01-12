(ns tech.thomas-sojka.hiccup-d3.charts.contour
  (:require ["d3" :as d3]
            [tech.thomas-sojka.hiccup-d3.utils :refer [fetch-json]])
  (:require-macros [tech.thomas-sojka.hiccup-d3.macros :as m]))

(def plain
  (m/build-chart
   "plain"
   (fn [data]
     (let [path (d3/geoPath)
           color (-> (d3/scaleSequential d3/interpolateTurbo)
                     (.domain (d3/extent (.-values data)))
                     (.nice))
           thresholds (.ticks color 20)
           width (.-width data)
           height (.-height data)
           contours (-> (d3/contours)
                        (.size (into-array [width height])))]
       [:svg {:viewBox (str 0 " " 0 " " width " " height)}
        [:g
         (map
          (fn [threshold]
            [:path {:key threshold
                    :d (path (.contour contours (.-values data) threshold))
                    :fill (color threshold)}])
          thresholds)]]))))

(def contour
  {:title "Contour"
   :load  (fn [] (fetch-json "data/volcano.json"))
   :charts [plain]})
