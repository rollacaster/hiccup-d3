(ns tech.thomas-sojka.hiccup-d3.charts.pie
  (:require ["d3" :as d3]
            [tech.thomas-sojka.hiccup-d3.utils :refer [fetch-json]])
  (:require-macros [tech.thomas-sojka.hiccup-d3.macros :as m]))

(def plain
  (m/build-chart
   "plain"
   (fn [data]
     (let [size 300
           pie (-> (d3/pie)
                   (.sort nil)
                   (.value (fn [d] (:value d))))
           arc (-> (d3/arc)
                   (.innerRadius 0)
                   (.outerRadius (/ size 2)))
           color (d3/scaleOrdinal d3/schemeCategory10)
           arcs (pie data)]
       [:svg {:viewBox (str (- (/ size 2)) " " (- (/ size 2)) " " size " " size)}
        (map
         (fn [pie-arc]
           [:g {:key (.-index pie-arc)}
            [:path {:d (arc pie-arc) :fill (color (.-index pie-arc))}]])
         arcs)]))))

(def with-labels
  (m/build-chart
   "with labels"
   (fn [data]
     (let [size 300
           pie (-> (d3/pie)
                   (.sort nil)
                   (.value (fn [d] (:value d))))
           arc (-> (d3/arc)
                   (.innerRadius 0)
                   (.outerRadius (/ size 2)))
           arc-label (let [r (* (/ size 2) 0.8)]
                       (-> (d3/arc)
                           (.innerRadius r)
                           (.outerRadius r)))
           color (d3/scaleOrdinal d3/schemeCategory10)
           arcs (pie data)]
       [:svg {:viewBox (str (- (/ size 2)) " " (- (/ size 2)) " " size " " size)}
        (map
         (fn [pie-arc]
           [:g {:key (.-index pie-arc)}
            [:path {:d (arc pie-arc) :fill (color (.-index pie-arc))}]
            (when (> (- ^js (.-endAngle pie-arc) ^js (.-startAngle pie-arc)) 0.3)
              [:text
               {:transform (str "translate(" (.centroid arc-label pie-arc) ")")
                :text-anchor "middle"
                :dominant-baseline "middle"}
               (:name (.-data pie-arc))])])
         arcs)]))))

(def pie
  {:title "Pie Chart"
   :load (fn []
           (-> (fetch-json "data/population-by-age.json")
               (.then (fn [res] (js->clj res :keywordize-keys true)))))
   :charts [plain with-labels]})
