(ns tech.thomas-sojka.hiccup-d3.charts.streamgraph
  (:require ["d3" :as d3]
            [tech.thomas-sojka.hiccup-d3.utils :refer [csv->clj]])
  (:require-macros [tech.thomas-sojka.hiccup-d3.macros :as m]))

(def plain
  (m/build-chart
   "plain"
   (fn [data]
     (let [size 300
           data-keys (remove #{"date"} (map name (keys (first data))))
           create-series (-> (d3/stack)
                             (.keys data-keys)
                             (.offset d3/stackOffsetWiggle)
                             (.order d3/stackOrderInsideOut))
           series (create-series (clj->js data))
           dates (map :date data)
           x (-> (d3/scaleUtc)
                 (.domain (into-array [(apply min dates) (apply max dates)]))
                 (.range (into-array  [0 size])))
           y (-> (d3/scaleLinear)
                 (.domain
                  (into-array
                   [(apply min (map (fn [d] (apply min (map first d))) series))
                    (apply max (map (fn [d] (apply max (map last d))) series))]))
                 (.range (into-array [0 size])))
           area (-> (d3/area)
                    (.x (fn [d] (x ^js (.-data.date d))))
                    (.y0 (fn [[y0]] (y y0)))
                    (.y1 (fn [[_ y1]] (y y1))))
           color (-> (d3/scaleOrdinal)
                     (.domain (clj->js data-keys))
                     (.range d3/schemeCategory10))]
       [:svg {:viewBox (str 0 " " 0 " " size " " size)}
        (map
         (fn [d] [:path {:key (.-index d) :d (area d) :fill (color (.-key d))}])
         series)]))))

(def streamgraph
  {:title "Streamgraph"
   :load  (let [parse-unemployment-data
                (fn [employment-data]
                  (-> employment-data
                      (update :date #(js/Date. %))
                      (update :MiningandExtraction js/parseFloat)
                      (update :Finance js/parseFloat)
                      (update :Leisureandhospitality js/parseFloat)
                      (update :Businessservices js/parseFloat)
                      (update :WholesaleandRetailTrade js/parseFloat)
                      (update :Construction js/parseFloat)
                      (update :Manufacturing js/parseFloat)
                      (update :Information js/parseFloat)
                      (update :Agriculture js/parseFloat)
                      (update :Other js/parseFloat)
                      (update :EducationandHealth js/parseFloat)
                      (update :TransportationandUtilities js/parseFloat)
                      (update :Self-employed js/parseFloat)
                      (update :Government js/parseFloat)))]
            (fn [] (-> (js/fetch "data/unemployment.csv")
                       (.then (fn [res] (.text res)))
                       (.then (fn [res] (->> res
                                             csv->clj
                                             (map parse-unemployment-data)))))))
   :charts [plain]})
