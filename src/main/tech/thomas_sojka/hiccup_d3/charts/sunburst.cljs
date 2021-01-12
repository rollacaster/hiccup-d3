(ns tech.thomas-sojka.hiccup-d3.charts.sunburst
  (:require ["d3" :as d3]
            [tech.thomas-sojka.hiccup-d3.utils :refer [fetch-json]])
  (:require-macros [tech.thomas-sojka.hiccup-d3.macros :as m]))

(def plain
  (m/build-chart
   "plain"
   (fn [data]
     (let [size 300
           arc (-> (d3/arc)
                   (.startAngle (fn [d] (.-x0 d)))
                   (.endAngle (fn [d] (.-x1 d)))
                   (.innerRadius (fn [d] (.-y0 d)))
                   (.outerRadius (fn [d] (- (.-y1 d) 1))))
           radius (/ size 2)
           color (d3/scaleOrdinal d3/schemeCategory10)
           partition ((-> (d3/partition)
                          (.size (into-array [(* 2 js/Math.PI) radius])))
                      (-> (d3/hierarchy data)
                          (.sum (fn [d] (.-value d)))
                          (.sort (fn [a b] (- (.-value b) (.-value a))))))]
       [:svg {:viewBox (str (- (/ size 2)) " " (- (/ size 2)) " " size " " size)}
        [:g
         (map
          (fn [d]
            [:path {:key  ^js (.-data.name d)
                    :d (arc d)
                    :fill (color ^js (.-data.name d))}])
          (.descendants partition))]]))))

(def sunburst
  {:title "Sunburst"
   :load (fn [] (fetch-json "data/flare-2.json"))
   :charts [plain]})
