(ns tech.thomas-sojka.hiccup-d3.app
  (:require ["d3" :as d3]
            [cljs.pprint :refer [pprint]]
            [reagent.core :as r]
            [reagent.dom :as dom]
            ["clipboard" :as clipboard]))
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
   '(fn [data]
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
          data)]))})

(def code (:code bar))
(defn chart-container []
  (new clipboard "#copy-code-button")
  (let [active-tab (r/atom :chart)]
    (fn [{:keys [title chart code data]}]
      (let [height (- 502 42)]
        [:div.shadow-lg.border.rounded-xl.bg-white {:class "w-1/2"}
         [:div.p-14.border-b
          [:h3.text-3xl.mb-7.font-semibold.tracking-wide
           title]
          [:div
           [:div
            {:class (r/class-names (when-not (= @active-tab :chart) "hidden"))}
            [chart @data]]
           [:div
            {:class (r/class-names (when-not (= @active-tab :code) "hidden"))}
            [:pre#code.overflow-auto.mb-4
             {:style {:height height}}
             (with-out-str (pprint code))]
            [:div.flex.justify-center
             [:button#copy-code-button.font-bold.border.px-3
              {:data-clipboard-target "#code"}
              "copy"]]]
           [:div
            {:class (r/class-names (when-not (= @active-tab :data) "hidden"))}
            [:pre#data.overflow-auto.mb-4 {:style {:height height}}
             (with-out-str (pprint @data))]
            [:div.flex.justify-center
             [:button#copy-code-button.font-bold.border.px-3
              {:data-clipboard-target "#data"}
              "copy"]]]]]
         [:div.flex.divide-x
          [:button.p-6.hover:bg-gray-100
           {:class "w-1/3" :on-click (fn [] (reset! active-tab :chart))}
           "Chart"]
          [:button.p-6.hover:bg-gray-100
           {:class "w-1/3" :on-click (fn [] (reset! active-tab :code))}
           "Code"]
          [:button.p-6.hover:bg-gray-100
           {:class "w-1/3" :on-click (fn [] (reset! active-tab :data))}
           "Data"]]]))))

(defn app []
  (-> (js/fetch "data/frequencies.json")
      (.then (fn [res] (.json res)))
      (.then (fn [res] (reset! (:data bar) (js->clj res :keywordize-keys true))))
      (.catch (fn [res] (prn res))))
  (fn []
    [:div.text-gray-900
     [:header.border-b.bg-gradient-to-b.from-gray-600.to-gray-900
      [:div.px-6.py-4.max-w-7xl.mx-auto
       [:h1.text-xl.font-bold.text-white
        [:a {:href "https://rollacaster.github.io/hiccup-d3/"} "hiccup-d3"]]
       [:div.text-white.py-12.flex
        [:div.text-4xl {:class "w-1/2"}
         [:div.max-w-md
          "Ready-made ClojureScript examples for D3"]]
        [:div.text-lg {:class "w-1/2"}
         [:div.max-w-md
          "Transforming D3 code to ClojureScript is complex. Use these starting points to create a new chart with "
          [:a.underline {:href "https://github.com/weavejester/hiccup"} "hiccup"]
          ". No functionality was wrapped access the full "
          [:a.underline {:href "https://github.com/d3/d3/blob/master/API.md"} "D3 API"] "."]]]]]
     [:div.max-w-7xl.mx-auto.p-6
      [chart-container bar]]]))

(dom/render [app] (js/document.getElementById "root"))

(defn init [])

