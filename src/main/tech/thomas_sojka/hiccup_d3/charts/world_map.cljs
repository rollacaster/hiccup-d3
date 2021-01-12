(ns tech.thomas-sojka.hiccup-d3.charts.world-map
  (:require ["d3" :as d3]
            [tech.thomas-sojka.hiccup-d3.utils :refer [fetch-json]])
  (:require-macros [tech.thomas-sojka.hiccup-d3.macros :as m]))

(def plain
  (m/build-chart
   "plain"
   (fn [data]
     (let [color (d3/scaleOrdinal d3/schemeCategory10)
           path (-> (d3/geoPath)
                    (.projection (d3/geoMercator)))]
       [:svg {:viewBox (str 0 " " 0 " " 1000 " " 650)}
        [:g {:transform (str "translate(" 0 ", " 200 ")")}
         (map
          (fn [country]
            (let [country-name ^js (.-properties.abbrev country)]
              [:path {:key country-name
                      :d (path country)
                      :fill (color country-name)}]))
          ^js (.-features data))]]))))

(def world-map
  {:title "World Map"
   :load  (fn [] (-> (fetch-json "data/countries.json")))
   :charts [plain]})
