(ns tech.thomas-sojka.hiccup-d3.charts.bar
  (:require ["d3" :as d3]
            [tech.thomas-sojka.hiccup-d3.utils :refer [fetch-json]])
  (:require-macros [tech.thomas-sojka.hiccup-d3.macros :as m]))

(def plain
  (m/build-chart
   "plain"
   (fn [data]
     (let [size 400
           x (-> (d3/scaleLinear)
                 (.range (into-array [0 size]))
                 (.domain (into-array [0 (apply max (map :frequency data))])))
           y (-> (d3/scaleBand)
                 (.domain (into-array (map :letter data)))
                 (.range (into-array [0 size])))
           color (d3/scaleOrdinal d3/schemeCategory10)]
       [:svg {:viewBox (str "0 0 " size " " size)}
        (map
         (fn [{:keys [letter frequency]}]
           [:g {:key letter :transform (str "translate(" 0 "," (y letter) ")")}
            [:rect {:x      (x 0)
                    :height (.bandwidth y)
                    :fill   (color letter)
                    :width  (x frequency)}]])
         data)]))))

(def with-labels
  (m/build-chart
   "with labels"
   (fn [data]
     (let [size 400
           margin {:top 0 :right 0 :left 16 :bottom 0}
           x (-> (d3/scaleLinear)
                 (.range (into-array [(:left margin) (- size (:right margin))]))
                 (.domain (into-array [0 (apply max (map :frequency data))])))
           y (-> (d3/scaleBand)
                 (.domain (into-array (map :letter data)))
                 (.range (into-array [(:top margin) (- size (:bottom margin))])))
           color (d3/scaleOrdinal d3/schemeCategory10)]
       [:svg {:viewBox (str "0 0 " size " " size)}
        (map
         (fn [{:keys [letter frequency]}]
           [:g {:key letter :transform (str "translate(" 0 "," (y letter) ")")}
            [:rect {:x      (x 0)
                    :height (.bandwidth y)
                    :fill   (color letter)
                    :width  (x frequency)}]])
         data)
        (map
         (fn [{:keys [letter frequency]}]
           [:g {:key letter :transform (str "translate(" 0 "," (y letter) ")")}
            [:text {:x 20
                    :y (+ (/ (.bandwidth y) 2) 1)
                    :dominant-baseline "middle"}
             frequency]
            [:text.current-fill
             {:x 0 :y (/ (.bandwidth y) 2) :dominant-baseline "middle"}
             letter]])
         data)]))))

(def bar
  {:title "Bar Chart"
   :load (fn []
           (-> (fetch-json "data/frequencies.json")
               (.then (fn [res] (js->clj res :keywordize-keys true)))))
   :charts [plain with-labels]})
