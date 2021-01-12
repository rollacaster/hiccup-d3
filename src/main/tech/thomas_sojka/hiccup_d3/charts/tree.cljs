(ns tech.thomas-sojka.hiccup-d3.charts.tree
  (:require ["d3" :as d3]
            [tech.thomas-sojka.hiccup-d3.utils :refer [fetch-json]])
  (:require-macros [tech.thomas-sojka.hiccup-d3.macros :as m]))

(def plain
  (m/build-chart
   "plain"
   (fn [data]
     (let [size 300
           r 2
           root ((-> (d3/tree)
                     (.size (into-array [(- size (* 2 r)) (- size (* 2 r))])))
                 (-> (d3/hierarchy data)))
           draw-link (-> (d3/linkVertical)
                         (.x (fn [d] (.-x d)))
                         (.y (fn [d] (.-y d))))]
       [:svg {:viewBox (str (- r) " " (- r) " " size " " size)}
        [:g
         (map
          (fn [node]
            [:circle {:key ^js (.-data.name node)
                      :cx (.-x node)
                      :cy (.-y node)
                      :r r}])
          (.descendants root))]
        [:g
         (map-indexed
          (fn [idx link]
            [:path {:key idx
                    :fill "transparent"
                    :stroke "black"
                    :d (draw-link link)}])
          (.links root))]]))))

(def tree
  {:title "Tree"
   :load  (fn [] (-> (fetch-json "data/flare-2.json")))
   :charts [plain]})
