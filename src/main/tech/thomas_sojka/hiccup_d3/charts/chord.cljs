(ns tech.thomas-sojka.hiccup-d3.charts.chord
  (:require ["d3" :as d3])
  (:require-macros [tech.thomas-sojka.hiccup-d3.macros :as m]))

(def plain
  (m/build-chart
   "plain"
   (fn [data]
     (let [size 300
           radius (/ size 2)
           innerRadius (- radius 10)
           color (d3/scaleOrdinal d3/schemeCategory10)
           ribbon (-> (d3/ribbon)
                      (.radius (- innerRadius 1))
                      (.padAngle (/ 1 innerRadius)))
           arc (-> (d3/arc)
                   (.innerRadius innerRadius)
                   (.outerRadius radius))
           chord (-> (d3/chord)
                     (.sortSubgroups d3/descending)
                     (.sortChords d3/descending))]
       [:svg {:viewBox (str (- radius) " " (- radius) " " size " " size)}
        [:g
         (map-indexed
          (fn [idx link]
            [:path {:key idx :d (ribbon link) :fill (color ^js (.-source.index link))}])
          (chord data))]
        [:g
         (map
          (fn [group]
            [:path {:d (arc group) :fill (color (.-index group)) :key (.-index group)}])
          (.-groups (chord data)))]]))))

(def chord
  {:title "Chord"
   :load  (fn [] (js/Promise.resolve
                  (clj->js
                   [[0.096899, 0.008859, 0.000554, 0.004430, 0.025471, 0.024363, 0.005537, 0.025471],
                    [0.001107, 0.018272, 0.000000, 0.004983, 0.011074, 0.010520, 0.002215, 0.004983],
                    [0.000554, 0.002769, 0.002215, 0.002215, 0.003876, 0.008306, 0.000554, 0.003322],
                    [0.000554, 0.001107, 0.000554, 0.012182, 0.011628, 0.006645, 0.004983, 0.010520],
                    [0.002215, 0.004430, 0.000000, 0.002769, 0.104097, 0.012182, 0.004983, 0.028239],
                    [0.011628, 0.026024, 0.000000, 0.013843, 0.087486, 0.168328, 0.017165, 0.055925],
                    [0.000554, 0.004983, 0.000000, 0.003322, 0.004430, 0.008859, 0.017719, 0.004430],
                    [0.002215, 0.007198, 0.000000, 0.003322, 0.016611, 0.014950, 0.001107, 0.054264]])))
   :charts [plain]})
