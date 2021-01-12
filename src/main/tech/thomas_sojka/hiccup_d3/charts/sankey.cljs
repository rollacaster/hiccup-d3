(ns tech.thomas-sojka.hiccup-d3.charts.sankey
  (:require ["d3" :as d3]
            ["d3-sankey" :as d3-sankey]
            [clojure.string :as str]
            [tech.thomas-sojka.hiccup-d3.utils :refer [csv->clj]])
  (:require-macros [tech.thomas-sojka.hiccup-d3.macros :as m]))

(def plain
  (m/build-chart
   "plain"
   (fn [data]
     (let [size 300
           color (d3/scaleOrdinal d3/schemeCategory10)
           nodes (fn [links]
                   (->> links
                        (mapcat (fn [{:keys [source target]}] [source target]))
                        distinct
                        (map (fn [name] {:name name :category (str/replace name #" .*" "")}))))
           data (clj->js {:links data :nodes (nodes data)})
           compute-sankey (-> (d3-sankey/sankey)
                              (.nodeId (fn [d] (.-name d)))
                              (.size (into-array [size size])))
           sankey-data (compute-sankey data)]
       [:svg {:viewBox (str "0 0 " size " " size)}
        [:g
         (map (fn [node]
                [:rect {:key (.-name node)
                        :height (- (.-y1 node) (.-y0 node))
                        :width (- (.-x1 node) (.-x0 node))
                        :x (.-x0 node)
                        :y (.-y0 node)
                        :fill (color ^js (.-category node))}])
              (.-nodes sankey-data))]
        [:g
         (map
          (fn [link]
            [:path {:key (.-index link)
                    :d ((d3-sankey/sankeyLinkHorizontal) link)
                    :stroke-width (.-width link)
                    :stroke (color (.-source.name ^js link))
                    :opacity 0.5
                    :fill "transparent"}])
          (.-links sankey-data))]]))))

(def sankey
  {:title "Sankey"
   :load  (fn []
            (-> (js/fetch "data/energy.csv")
                (.then (fn [res] (.text res)))
                (.then (fn [res] (->> res
                                      csv->clj
                                      (map #(update % :value js/parseFloat)))))
                (.catch (fn [res] (prn res)))))
   :charts [plain]})
