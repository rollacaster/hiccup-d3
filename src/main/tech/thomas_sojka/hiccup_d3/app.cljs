(ns tech.thomas-sojka.hiccup-d3.app
  (:require ["clipboard" :as clipboard]
            ["d3" :as d3]
            ["d3-sankey" :as d3-sankey]
            [cljs.pprint :refer [pprint]]
            [clojure.string :as str]
            [reagent.core :as r]
            [reagent.dom :as dom]
            [tech.thomas-sojka.hiccup-d3.charts.bar :refer [bar]]
            [tech.thomas-sojka.hiccup-d3.charts.pie :refer [pie]]
            [tech.thomas-sojka.hiccup-d3.utils :refer [fetch-json]])
  (:require-macros [tech.thomas-sojka.hiccup-d3.macros :as m]))

(defn icon [{:keys [name class]}]
  [:svg.fill-current {:viewBox "0 0 24 24" :class class}
   [:path {:d (case name
                :chart "M5 19h-4v-4h4v4zm6 0h-4v-8h4v8zm6 0h-4v-13h4v13zm6 0h-4v-19h4v19zm1 2h-24v2h24v-2z"
                :code "M24 10.935v2.131l-8 3.947v-2.23l5.64-2.783-5.64-2.79v-2.223l8 3.948zm-16 3.848l-5.64-2.783 5.64-2.79v-2.223l-8 3.948v2.131l8 3.947v-2.23zm7.047-10.783h-2.078l-4.011 16h2.073l4.016-16z"
                :data "M13 6c3.469 0 2 5 2 5s5-1.594 5 2v9h-12v-16h5zm.827-2h-7.827v20h16v-11.842c0-2.392-5.011-8.158-8.173-8.158zm.173-2l-3-2h-9v22h2v-20h10z"
                :copy "M22 2v22h-20v-22h3c1.23 0 2.181-1.084 3-2h8c.82.916 1.771 2 3 2h3zm-11 1c0 .552.448 1 1 1 .553 0 1-.448 1-1s-.447-1-1-1c-.552 0-1 .448-1 1zm9 1h-4l-2 2h-3.897l-2.103-2h-4v18h16v-18zm-13 9.729l.855-.791c1 .484 1.635.852 2.76 1.654 2.113-2.399 3.511-3.616 6.106-5.231l.279.64c-2.141 1.869-3.709 3.949-5.967 7.999-1.393-1.64-2.322-2.686-4.033-4.271z")}]])

(defn spinner []
  [:svg {:width "38" :height "38" :viewBox "0 0 38 38"}
   [:defs [:linearGradient#a {:x1 "8.042%" :y1 "0%" :x2 "65.682%" :y2 "23.865%"}
           [:stop {:stop-color "#1F2937" :stop-opacity "0" :offset "0%"}]
           [:stop {:stop-color "#1F2937" :stop-opacity ".631" :offset "63.146%"}]
           [:stop {:stop-color "#1F2937" :offset "100%"}]]]
   [:g {:fill "none" :fill-rule "evenodd"}
    [:g {:transform "translate(1 1)"}
     [:path#Oval-2 {:d "M36 18c0-9.94-8.06-18-18-18" :stroke "url(#a)" :stroke-width "2"}
      [:animateTransform {:attributeName "transform" :type "rotate" :from "0 18 18" :to "360 18 18" :dur "0.9s" :repeatCount "indefinite"}]]
     [:circle {:fill "#1F2937" :cx "36" :cy "18" :r "1"}
      [:animateTransform {:attributeName "transform" :type "rotate" :from "0 18 18" :to "360 18 18" :dur "0.9s" :repeatCount "indefinite"}]]]]])

(defn get-screen-size  []
  (condp #(< %2 %1) js/window.screen.width
    640 "sm"
    768 "md"
    1024 "lg"
    1280 "xl"
    "2xl"))

(def screen-size (r/atom (get-screen-size)))

(defn csv->clj
  [csv]
  (let [[header-line & content-lines] (str/split-lines csv)
        headers (map #(-> %
                          (str/split " ")
                          str/join
                          keyword)
                     (str/split header-line ","))]
    (map
     (fn [line]
       (zipmap headers (str/split line ",")))
     content-lines)))

(def line
  {:title "Line Chart"
   :load  (fn []
            (let [parse-stock-data (fn [stock-data]
                                     (-> stock-data
                                         (update :date #(js/Date. %))
                                         (update :close js/parseFloat)))]
              (-> (js/fetch "data/apple-stock.csv")
                  (.then (fn [res] (.text res)))
                  (.then (fn [res] ((comp #(map parse-stock-data %) csv->clj) res))))))
   :charts
   [(m/build-chart
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
          [:path {:d      (line data)
                  :fill   "transparent"
                  :stroke (first d3/schemeCategory10)}]])))]})

(def pack
  {:title "Circle Packing"
   :load  (fn [] (-> (fetch-json "data/flare-2.json")))
   :charts
   [(m/build-chart
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
           (.descendants root))])))]})

(def tree
  {:title "Tree"
   :load  (fn [] (-> (fetch-json "data/flare-2.json")))
   :charts
   [(m/build-chart
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
            (.links root))]])))]})

(def world-map
  {:title "World Map"
   :load  (fn [] (-> (fetch-json "data/countries.json")))
   :charts
   [(m/build-chart
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
            ^js (.-features data))]])))]})

(def sankey
  {:title "Sankey"
   :load  (fn []
            (let [parse-energy-data (fn [energy-data]
                                      (-> energy-data
                                          (update :value js/parseFloat)))]
              (-> (js/fetch "data/energy.csv")
                  (.then (fn [res] (.text res)))
                  (.then (fn [res] ((comp #(map parse-energy-data %) csv->clj) res)))
                  (.catch (fn [res] (prn res))))))
   :charts
   [(m/build-chart
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
            (.-links sankey-data))]])))]})

(def graph
  {:title "Graph"
   :load  (fn [] (-> (fetch-json "data/miserables.json")))
   :charts
   [(m/build-chart
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
            (.-links data))]])))]})

(def sunburst
  {:title "Sunburst"
   :load (fn [] (fetch-json "data/flare-2.json"))
   :charts
   [(m/build-chart
     "plain"
     (fn [data]
       (let [size 300
             arc (-> (d3/arc)
                     (.startAngle (fn [d] (.-x0 d)))
                     (.endAngle (fn [d] (.-x1 d)))
                     (.innerRadius (fn [d] (.-y0 d)))
                     (.outerRadius (fn [d] (- (.-y1 d) 1))))
             radius (/ size 2)
             color (d3/scaleOrdinal d3/schemeCategory10)
             partition ((-> (d3/partition)
                            (.size (into-array [(* 2 js/Math.PI) radius])))
                        (-> (d3/hierarchy data)
                            (.sum (fn [d] (.-value d)))
                            (.sort (fn [a b] (- (.-value b) (.-value a))))))]
         [:svg {:viewBox (str (- (/ size 2)) " " (- (/ size 2)) " " size " " size)}
          [:g
           (map
            (fn [d]
              [:path {:key  ^js (.-data.name d)
                      :d (arc d)
                      :fill (color ^js (.-data.name d))}])
            (.descendants partition))]])))]})

(def treemap
  {:title "Treemap"
   :load (fn [] (fetch-json "data/flare-2.json"))
   :charts
   [(m/build-chart
     "plain"
     (fn [data]
       (let [size 300
             color (d3/scaleOrdinal d3/schemeCategory10)
             root ((-> (d3/treemap)
                       (.tile d3/treemapBinary)
                       (.size (into-array [size size])))
                   (-> (d3/hierarchy data)
                       (.sum (fn [d] (.-value d)))
                       (.sort (fn [a b] (- (.-value b) (.-value a))))))]
         [:svg {:viewBox (str 0 " " 0 " " size " " size)}
          [:g
           (->> (.leaves root)
                (map (fn [d]
                       (let [parent-name (loop [d d]
                                           (if (> (.-depth d) 1)
                                             (recur (.-parent d))
                                             ^js (.-data.name d)))]
                         [:rect {:key ^js (.-data.name d)
                                 :x (.-x0 d) :y (.-y0 d)
                                 :width (- (.-x1 d) (.-x0 d))
                                 :height (- (.-y1 d) (.-y0 d))
                                 :stroke "black"
                                 :fill (color parent-name)}]))))]])))]})

(def chord
  {:title "Chord"
   :load  (fn [] (js/Promise.resolve
                  (clj->js
                   [[0.096899, 0.008859, 0.000554, 0.004430, 0.025471, 0.024363, 0.005537, 0.025471],
                    [0.001107, 0.018272, 0.000000, 0.004983, 0.011074, 0.010520, 0.002215, 0.004983],
                    [0.000554, 0.002769, 0.002215, 0.002215, 0.003876, 0.008306, 0.000554, 0.003322],
                    [0.000554, 0.001107, 0.000554, 0.012182, 0.011628, 0.006645, 0.004983, 0.010520],
                    [0.002215, 0.004430, 0.000000, 0.002769, 0.104097, 0.012182, 0.004983, 0.028239],
                    [0.011628, 0.026024, 0.000000, 0.013843, 0.087486, 0.168328, 0.017165, 0.055925],
                    [0.000554, 0.004983, 0.000000, 0.003322, 0.004430, 0.008859, 0.017719, 0.004430],
                    [0.002215, 0.007198, 0.000000, 0.003322, 0.016611, 0.014950, 0.001107, 0.054264]])))
   :charts
   [(m/build-chart
     "plain"
     (fn [data]
       (let [size 300
             radius (/ size 2)
             innerRadius (- radius 10)
             color (d3/scaleOrdinal d3/schemeCategory10)
             ribbon (-> (d3/ribbon)
                        (.radius (- innerRadius 1))
                        (.padAngle (/ 1 innerRadius)))
             arc (-> (d3/arc)
                     (.innerRadius innerRadius)
                     (.outerRadius radius))
             chord (-> (d3/chord)
                       (.sortSubgroups d3/descending)
                       (.sortChords d3/descending))]
         [:svg {:viewBox (str (- radius) " " (- radius) " " size " " size)}
          [:g
           (map-indexed
            (fn [idx link]
              [:path {:key idx :d (ribbon link) :fill (color ^js (.-source.index link))}])
            (chord data))]
          [:g
           (map
            (fn [group]
              [:path {:d (arc group) :fill (color (.-index group)) :key (.-index group)}])
            (.-groups (chord data)))]])))]})

(def contour
  {:title "Contour"
   :load (fn [] (fetch-json "data/volcano.json"))
   :charts
   [(m/build-chart
     "plain"
     (fn [data]
       (let [path (d3/geoPath)
             color (-> (d3/scaleSequential d3/interpolateTurbo)
                       (.domain (d3/extent (.-values data)))
                       (.nice))
             thresholds (.ticks color 20)
             width (.-width data)
             height (.-height data)
             contours (-> (d3/contours)
                          (.size (into-array [width height])))]
         [:svg {:viewBox (str 0 " " 0 " " width " " height)}
          [:g
           (map
            (fn [threshold]
              [:path {:key threshold
                      :d (path (.contour contours (.-values data) threshold))
                      :fill (color threshold)}])
            thresholds)]])))]})

(def voronoi
  {:title "Voronoi"
   :load  (fn [] (js/Promise.resolve (map (fn [] [(rand 300) (rand 300)]) (range 100))))
   :charts
   [(m/build-chart
     "plain"
     (fn [data]
       (let [size 300
             delaunay (.from d3/Delaunay (clj->js data))
             voronoi (.voronoi delaunay (into-array [0 0 size size]))]
         [:svg {:viewBox (str 0 " " 0 " " size " " size)}
          [:path {:fill "transparent"
                  :stroke "black"
                  :d (.render voronoi)}]])))]})

(def streamgraph
  {:title "Streamgraph"
   :load (let [parse-unemployment-data (fn [employment-data]
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
                      (.then (fn [res] ((comp #(map parse-unemployment-data %) csv->clj) res))))))
   :charts
   [(m/build-chart
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
           series)])))]})

(defn card [children]
  [:div.shadow-lg.border.md:rounded-xl.bg-white.w-full.mb-2.lg:mr-16.md:mb-2.lg:mb-16 {:class "md:w-1/2 lg:w-5/12"}
   children])

(defn chart-container-button [{:keys [type active-tab]} children]
  [:button.p-5.md:p-6.hover:bg-gray-100.focus:outline-none.focus:ring
   {:class "w-1/3" :on-click (fn [] (reset! active-tab type))}
   [:div.flex.items-center.justify-center
    {:class (if (= @active-tab type) "text-blue-600" "text-gray-600")}
    [:div.w-4.h-4.mr-1 [icon {:name type}]]
    children]])

(defn chart-container [{:keys [load]}]
  (let [copy-id (random-uuid)
        data (r/atom nil)]
    (-> (load)
        (.then (fn [res] (reset! data res)))
        (.catch (fn [e] (prn e))))
    (clipboard. ".copy-button")
    (let [active-tab (r/atom :chart)
          active-variant (r/atom 0)]
      (fn [{:keys [title charts]}]
        (let [chart-height (if (#{"sm" "md" "lg"} @screen-size) "auto" 399)
              height (if (#{"sm" "md" "lg"} @screen-size) "auto" (- chart-height 42))]
          [card
           [:<>
            [:div.p-6.md:pt-14.md:px-14.border-b
             {:style {:height "calc(100% - 72px)"}}
             [:h2.text-3xl.font-semibold.tracking-wide.mb-3
              title]
             (when (> (count charts) 1)
               [:div.pb-3
                [:h3.mb-2.text-sm.font-bold "Variants"]
                [:ul.flex.flex-wrap
                 (doall
                  (map-indexed
                   (fn [idx {:keys [title]}]
                     [:li.mr-2.mb-2 {:key title :class (if (= idx @active-variant) "text-blue-300" "text-white")}
                      [:button.px-3.py-1.rounded-full.bg-gray-700.focus:outline-none.focus:ring
                       {:on-click #(reset! active-variant idx)}
                       title]])
                   charts))]])
             (let [{:keys [d3-apis chart code-formatted]} (nth charts @active-variant)]
               [:div
                [:div
                 {:class
                  (r/class-names (when-not (= @active-tab :chart) "hidden")
                                 (when-not @data "flex justify-center items-center"))
                  :style {:height chart-height}}
                 (if @data
                   [chart @data]
                   [spinner])]
                [:div
                 {:class (r/class-names (when-not (= @active-tab :code) "hidden"))}
                 [:pre.overflow-auto.mb-4
                  {:style {:height height}  :id (str "code" copy-id)}
                  [:div {:dangerouslySetInnerHTML {:__html code-formatted}}]]
                 [:div.flex.justify-center
                  [:button.copy-button.font-bold.border.px-3.focus:outline-none.focus:ring
                   {:data-clipboard-target (str "#code" copy-id)}
                   [:div.flex.items-center.justify-center
                    [:div.w-4.h-4.mr-1 [icon {:name :copy :class "text-gray-600"}]]
                    "copy"]]]]
                [:div
                 {:class (r/class-names (when-not (= @active-tab :data) "hidden"))}
                 [:pre.overflow-auto.mb-4
                  {:style {:height height} :id (str "data" copy-id)}
                  (if (coll? @data)
                    (with-out-str (pprint @data))
                    (.stringify js/JSON @data nil 2))]
                 [:div.flex.justify-center
                  [:button.copy-button.font-bold.border.px-3.focus:outline-none.focus:ring
                   {:data-clipboard-target (str "#data" copy-id)}
                   [:div.flex.items-center.justify-center
                    [:div.w-4.h-4.mr-1
                     [icon {:name :copy :class "text-gray-600"}]]
                    "copy"]]]]
                [:div.pt-3
                 [:h3.mb-2.text-sm.font-bold.pl-3 "d3 APIs"]
                 [:ul.flex.flex-wrap
                  (map (fn [{:keys [fn doc-link]}]
                         [:li.text-white.mr-2.mb-2.rounded-full
                          {:key fn :class (if doc-link "bg-gray-700" "bg-red-400")}
                          [:a.focus:outline-none.focus:ring.rounded-full.px-3.py-1.block
                           {:href doc-link :target "_blank" :rel "noopener"} fn]])
                       d3-apis)]]])]
            [:div.flex.divide-x
             [chart-container-button {:type :chart :active-tab active-tab} "Chart"]
             [chart-container-button {:type :code :active-tab active-tab} "Code"]
             [chart-container-button {:type :data :active-tab active-tab} "Data"]]]])))))

(defn app []
  (r/with-let [_ (js/window.addEventListener "resize" (fn [] (reset! screen-size (get-screen-size))))]
    [:div.text-gray-900.flex.flex-col.h-screen
     [:header.border-b.bg-gradient-to-b.from-gray-600.to-gray-900
      [:div.px-6.py-4.max-w-7xl.mx-auto
       [:h1.text-xl.font-bold.text-white
        [:a {:href "https://rollacaster.github.io/hiccup-d3/"} "hiccup-d3"]]
       [:div.text-white.py-12.flex
        [:div.text-4xl {:class "lg:w-1/2"}
         [:div.max-w-md
          "Ready-made ClojureScript examples for D3"]]
        [:div.text-lg.hidden.md:block {:class "w-1/2"}
         [:div.max-w-md
          "Transforming D3 code to ClojureScript is complex. Use these starting points to create a new chart with "
          [:a.underline {:href "https://github.com/weavejester/hiccup"} "hiccup"]
          ". No functionality was wrapped, access the full "
          [:a.underline {:href "https://github.com/d3/d3/blob/master/API.md"} "D3 API"] "."]]]]]
     [:div.flex-1
      [:div.max-w-7xl.mx-auto.py-2.md:p-6.flex.flex-wrap
       [chart-container bar]
       [chart-container pie]
       [chart-container pack]
       [chart-container tree]
       [chart-container world-map]
       [chart-container sankey]
       [chart-container sunburst]
       [chart-container graph]
       [chart-container line]
       [chart-container treemap]
       [chart-container chord]
       [chart-container contour]
       [chart-container voronoi]
       [chart-container streamgraph]]]
     [:footer.bg-gray-800.flex.justify-center.py-2
      [:a.text-white.underline {:href "https://github.com/rollacaster/hiccup-d3"} "Code"]]]
    (finally
      (js/document.removeEventListener "resize" get-screen-size))))

(dom/render [app] (js/document.getElementById "root"))

(defn init [])

