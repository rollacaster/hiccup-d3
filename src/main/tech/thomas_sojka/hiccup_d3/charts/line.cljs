(ns tech.thomas-sojka.hiccup-d3.charts.line
  (:require ["d3" :as d3]
            [tech.thomas-sojka.hiccup-d3.utils :refer [csv->clj]])
  (:require-macros [tech.thomas-sojka.hiccup-d3.macros :as m]))

(def plain
  (m/build-chart
   "plain"
   (fn [data]
     (let [size 300
           dates (map :date data)
           values (map :close data)
           x (-> (d3/scaleUtc)
                 (.domain (into-array [(apply min dates) (apply max dates)]))
                 (.range (into-array [0 size])))
           y (-> (d3/scaleLinear)
                 (.domain (into-array [0 (apply max values)]))
                 (.range (into-array [size 0])))
           line (-> (d3/line)
                    (.x (fn [d] (x (:date d))))
                    (.y (fn [d] (y (:close d)))))]
       [:svg {:viewBox (str 0 " " 0 " " size " " size)}
        [:path {:d (line data)
                :fill "transparent"
                :stroke (first d3/schemeCategory10)}]]))))

(def line
  {:title "Line"
   :load  (fn []
            (let [parse-stock-data (fn [stock-data]
                                     (-> stock-data
                                         (update :date #(js/Date. %))
                                         (update :close js/parseFloat)))]
              (-> (js/fetch "data/apple-stock.csv")
                  (.then (fn [res] (.text res)))
                  (.then (fn [res] (->> res
                                        csv->clj
                                        (map parse-stock-data)))))))
   :charts [plain]})
