(ns tech.thomas-sojka.hiccup-d3.charts.pack
  (:require ["d3" :as d3]
            [tech.thomas-sojka.hiccup-d3.utils :refer [fetch-json]])
  (:require-macros [tech.thomas-sojka.hiccup-d3.macros :as m]))

(def plain
  (m/build-chart
   "plain"
   (fn [data]
     (let [size 300
           color (d3/scaleOrdinal d3/schemeCategory10)
           margin 7
           root ((-> (d3/pack)
                     (.size (into-array [(- size margin) (- size margin)])))
                 (-> (d3/hierarchy data)
                     (.sum (fn [d] (.-value d)))
                     (.sort (fn [a b] (- (.-value b) (.-value a))))))]
       [:svg {:viewBox (str 0 " " 0 " " size " " size)}
        [:filter {:id "dropshadow" :filterUnits "userSpaceOnUse"}
         [:feGaussianBlur {:in "SourceAlpha" :stdDeviation "3"}]
         [:feOffset {:dx (/ margin 2) :dy (/ margin 2)}]
         [:feMerge
          [:feMergeNode]
          [:feMergeNode {:in "SourceGraphic"}]]]
        (map
         (fn [node]
           [:circle {:key ^js (.-data.name node)
                     :cx (.-x node) :cy (.-y node) :r (.-r node)
                     :fill (color (.-height node))
                     :filter "url(#dropshadow)"}])
         (.descendants root))]))))

(def pack
  {:title "Pack"
   :load  (fn [] (-> (fetch-json "data/flare-2.json")))
   :charts [plain]})
