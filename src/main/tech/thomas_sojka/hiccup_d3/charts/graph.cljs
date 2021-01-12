(ns tech.thomas-sojka.hiccup-d3.charts.graph
  (:require ["d3" :as d3]
            [tech.thomas-sojka.hiccup-d3.utils :refer [fetch-json]])
  (:require-macros [tech.thomas-sojka.hiccup-d3.macros :as m]))

(def plain
  (m/build-chart
   "plain"
   (fn [data]
     (let [size 600]
       (-> (d3/forceSimulation (.-nodes data))
           (.force "link" (-> (d3/forceLink (.-links data))
                              (.id (fn [d] (.-id d)))))
           (.force "charge" (d3/forceManyBody))
           (.force "center" (d3/forceCenter (/ size 2) (/ size 2)))
           .stop
           (.tick 1500))
       [:svg {:viewBox (str "0 0 " size " " size)}
        [:g
         (map (fn [node]
                [:circle {:key (.-id node)
                          :cx (.-x node)
                          :cy (.-y node)
                          :r 5}])
              (.-nodes data))]
        [:g
         (map
          (fn [link]
            [:line {:key (.-index link)
                    :x1 (.-x (.-source link))
                    :y1 (.-y (.-source link))
                    :x2 (.-x (.-target link))
                    :y2 (.-y (.-target link))
                    :stroke "black"}])
          (.-links data))]]))))

(def graph
  {:title "Graph"
   :load  (fn [] (-> (fetch-json "data/miserables.json")))
   :charts [plain]})
