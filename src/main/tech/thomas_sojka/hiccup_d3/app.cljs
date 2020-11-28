(ns tech.thomas-sojka.hiccup-d3.app
  (:require ["d3" :as d3]
            [cljs.pprint :refer [pprint]]
            [reagent.core :as r]
            [reagent.dom :as dom]))
(def bar
  {:title "Bar Chart"
   :data (r/atom [])
   :chart (fn [data]
            (let [size 300
                  margin {:top 0 :right 0 :left 13 :bottom 0}
                  x (-> (d3/scaleLinear)
                        (.range (clj->js [(:left margin) (- size (:right margin))]))
                        (.domain (clj->js [0 (apply max (map :frequency data))])))
                  y (-> (d3/scaleBand)
                        (.domain (clj->js (range (count data))))
                        (.range (clj->js [(:top margin) (- size (:bottom margin))])))
                  color (d3/scaleOrdinal d3/schemeCategory10)]
              [:svg {:viewBox (str "0 0 " size " " size)}
               (map-indexed
                (fn [idx {:keys [letter frequency]}]
                  [:g {:key idx :transform (str "translate("0 "," (y idx) ")")}
                   [:rect {:x (x 0)
                           :height (.bandwidth y)
                           :fill (color letter)
                           :width (x frequency)}]
                   ])
                data)
               (map-indexed
                (fn [idx {:keys [letter frequency]}]
                  [:g {:key idx :transform (str "translate("0 "," (y idx) ")")}
                   [:text {:style {:font-size 8}
                           :x 17
                           :y (/ (.bandwidth y) 2)
                           :dominant-baseline "middle"} (str frequency)]
                   [:text.current-fill
                    {:x 0 :y (/ (.bandwidth y) 2) :dominant-baseline "middle" :style {:font-size 10}}
                    (str letter)]])
                data)]))
   :code
   '(let [size 300
          margin {:top 0 :right 0 :left 13 :bottom 0}
          x (-> (d3/scaleLinear)
                (.range (clj->js [(:left margin) (- size (:right margin))]))
                (.domain (clj->js [0 (apply max (map :frequency data))])))
          y (-> (d3/scaleBand)
                (.domain (clj->js (range (count data))))
                (.range (clj->js [(:top margin) (- size (:bottom margin))])))
          color (d3/scaleOrdinal d3/schemeCategory10)]
      [:svg {:viewBox (str "0 0 " size " " size)}
       (map-indexed
        (fn [idx {:keys [letter frequency]}]
          [:g {:key idx :transform (str "translate("0 "," (y idx) ")")}
           [:rect {:x (x 0)
                   :height (.bandwidth y)
                   :fill (color letter)
                   :width (x frequency)}]
           ])
        data)
       (map-indexed
        (fn [idx {:keys [letter frequency]}]
          [:g {:key idx :transform (str "translate("0 "," (y idx) ")")}
           [:text {:style {:font-size 8}
                   :x 17
                   :y (/ (.bandwidth y) 2)
                   :dominant-baseline "middle"} (str frequency)]
           [:text.current-fill
            {:x 0 :y (/ (.bandwidth y) 2) :dominant-baseline "middle" :style {:font-size 10}}
            (str letter)]])
        data)])})

(defn chart-container [{:keys [title chart code data]}]
  [:div {:class "w-1/2"}
   [:h3.text-lg.font-bold.mb-2 title]
   [:div.mb-2 [chart @data]]
   [:pre.overflow-scroll.mb-2
    (with-out-str (pprint code))]
   [:pre.overflow-scroll
    (with-out-str (pprint @data))]])

(defn app []
  (-> (js/fetch "data/frequencies.json")
      (.then (fn [res] (.json res)))
      (.then (fn [res] (reset! (:data bar) (js->clj res :keywordize-keys true))))
      (.catch (fn [res] (prn res))))
  (fn []
    [:div
     [:header.border-b
      [:div.px-6.py-4.max-w-7xl.mx-auto
       [:h1.text-xl.font-bold "hiccup-d3"]]]
     [:div.max-w-7xl.mx-auto.p-6
      [chart-container bar]]]))

(dom/render [app] (js/document.getElementById "root"))

(defn init [])

