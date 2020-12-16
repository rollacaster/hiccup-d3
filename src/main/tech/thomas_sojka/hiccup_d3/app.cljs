(ns tech.thomas-sojka.hiccup-d3.app
  (:require ["d3" :as d3]
            ["d3-sankey" :as d3-sankey]
            [cljs.pprint :refer [pprint]]
            [clojure.string :as str]
            [reagent.core :as r]
            [reagent.dom :as dom]
            ["clipboard" :as clipboard])
  (:require-macros [tech.thomas-sojka.hiccup-d3.macros :as m]))

(defn icon [{:keys [name class]}]
  [:svg.fill-current {:viewBox "0 0 24 24" :class class}
   [:path {:d (case name
                :chart "M5 19h-4v-4h4v4zm6 0h-4v-8h4v8zm6 0h-4v-13h4v13zm6 0h-4v-19h4v19zm1 2h-24v2h24v-2z"
                :code "M24 10.935v2.131l-8 3.947v-2.23l5.64-2.783-5.64-2.79v-2.223l8 3.948zm-16 3.848l-5.64-2.783 5.64-2.79v-2.223l-8 3.948v2.131l8 3.947v-2.23zm7.047-10.783h-2.078l-4.011 16h2.073l4.016-16z"
                :data "M13 6c3.469 0 2 5 2 5s5-1.594 5 2v9h-12v-16h5zm.827-2h-7.827v20h16v-11.842c0-2.392-5.011-8.158-8.173-8.158zm.173-2l-3-2h-9v22h2v-20h10z"
                :copy "M22 2v22h-20v-22h3c1.23 0 2.181-1.084 3-2h8c.82.916 1.771 2 3 2h3zm-11 1c0 .552.448 1 1 1 .553 0 1-.448 1-1s-.447-1-1-1c-.552 0-1 .448-1 1zm9 1h-4l-2 2h-3.897l-2.103-2h-4v18h16v-18zm-13 9.729l.855-.791c1 .484 1.635.852 2.76 1.654 2.113-2.399 3.511-3.616 6.106-5.231l.279.64c-2.141 1.869-3.709 3.949-5.967 7.999-1.393-1.64-2.322-2.686-4.033-4.271z")}]])
(def bar
  (m/build-chart
   {:title "Bar Chart"
    :data  (r/atom [])
    :code  (fn [data]
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
                 data)]))}))

(def pie
  (m/build-chart
   {:title "Pie Chart"
    :data  (r/atom [])
    :code  (fn [data]
             (let [size 300
                   pie (-> (d3/pie)
                           (.sort nil)
                           (.value #(:value %)))
                   arc (-> (d3/arc)
                           (.innerRadius 0)
                           (.outerRadius (/ size 2)))
                   arc-label (let [r (* (/ size 2) 0.8)]
                               (-> (d3/arc)
                                   (.innerRadius r)
                                   (.outerRadius r)))
                   color (d3/scaleOrdinal d3/schemeCategory10)
                   arcs (pie data)]
               [:svg {:viewBox (str (- (/ size 2)) " " (- (/ size 2)) " " size " " size)}
                (map
                 (fn [pie-arc]
                   [:g {:key (.-index pie-arc)}
                    [:path {:d (arc pie-arc) :fill (color (.-index pie-arc))}]
                    (when (> (- ^js (.-endAngle pie-arc) ^js (.-startAngle pie-arc)) 0.3)
                      [:text
                       {:transform (str "translate(" (.centroid arc-label pie-arc) ")")
                        :text-anchor "middle"
                        :dominant-baseline "middle"}
                       (:name (.-data pie-arc))])])
                 arcs)]))}))

(def line
  (m/build-chart
   {:title "Line Chart"
    :data  (r/atom [])
    :code  (fn [data]
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
                            (.x #(x (:date %)))
                            (.y #(y (:close %))))]
               [:svg {:viewBox (str 0 " " 0 " " size " " size)}
                [:path {:d      (line data)
                        :fill   "transparent"
                        :stroke (first d3/schemeCategory10)}]]))}))

(def pack
  (m/build-chart
   {:title "Circle Packing"
    :data  (r/atom nil)
    :code  (fn [data]
             (let [size 300
                   color (d3/scaleOrdinal d3/schemeCategory10)
                   margin 7
                   root ((-> (d3/pack)
                             (.size (into-array [(- size margin) (- size margin)])))
                         (-> (d3/hierarchy data)
                             (.sum #(.-value %))
                             (.sort #(- (.-value %2) (.-value %1)))))]
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
                 (.descendants root))]))}))

(def tree
  (m/build-chart
   {:title "Tree"
    :data  (r/atom nil)
    :code  (fn [data]
             (let [size 300
                   r 2
                   root ((-> (d3/tree)
                             (.size (into-array [(- size (* 2 r)) (- size (* 2 r))])))
                         (-> (d3/hierarchy data)))
                   draw-link (-> (d3/linkVertical)
                                 (.x #(.-x %))
                                 (.y #(.-y %)))]
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
                  (.links root))]]))}))

(def world-map
  (m/build-chart
   {:title "World Map"
    :data  (r/atom [])
    :code
    (fn [data]
      (let [size 393
            color (d3/scaleOrdinal d3/schemeCategory10)
            path (-> (d3/geoPath)
                     (.projection (d3/geoMercator)))]
        [:div {:style {:height size
                       :display "flex"
                       :flex-direction  "column"
                       :justify-content "center"}}
         [:svg {:viewBox (str 0 " " 0 " " 1000 " " 650)}
          [:g {:transform (str "translate(" 0 ", " 200 ")")}
           (map
            (fn [country]
              (let [country-name ^js (.-properties.abbrev country)]
                [:path {:key country-name
                        :d (path country)
                        :fill (color country-name)}]))
            ^js (.-features data))]]]))}))

(def sankey
  (m/build-chart
   {:title "Sankey"
    :data  (r/atom nil)
    :code
    (fn [data]
      (let [size 300
            color (d3/scaleOrdinal d3/schemeCategory10)
            links->nodes #(->> %
                               (mapcat (fn [{:keys [source target]}] [source target]))
                               distinct
                               (map (fn [name] {:name name :category (str/replace name #" .*" "")})))
            data (clj->js {:links data :nodes (links->nodes data)})
            compute-sankey (-> (d3-sankey/sankey)
                               (.nodeId #(.-name %))
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
           (.-links sankey-data))]]))}))

(def graph
  (m/build-chart
   {:title "Graph"
    :data  (r/atom [])
    :code
    (fn [data]
      (let [size 600]
        (-> (d3/forceSimulation (.-nodes data))
            (.force "link" (-> (d3/forceLink (.-links data))
                               (.id (fn [d] (.-id d)))))
            (.force "charge" (d3/forceManyBody))
            (.force "center" (-> (d3/forceCenter (/ size 2) (/ size 2))))
            (.stop)
            (.tick 1500))
        [:svg {:viewBox (str "0 0 " size " " size)}
         [:g
          (map-indexed (fn [idx node]
                         [:circle {:key idx
                                   :cx (.-x node)
                                   :cy (.-y node)
                                   :r 5}])
                       (.-nodes data))]
         [:g
          (map-indexed
           (fn [idx link]
             [:line {:key idx
                     :x1 (.-x (.-source link))
                     :y1 (.-y (.-source link))
                     :x2 (.-x (.-target link))
                     :y2 (.-y (.-target link))
                     :stroke "black"}])
           (.-links data))]]))}))

(def sunburst
  (m/build-chart
   {:title "Sunburst"
    :data  (r/atom [])
    :code
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
                           (.size (clj->js [(* 2 js/Math.PI) radius])))
                       (-> (d3/hierarchy data)
                           (.sum (fn [d] (.-value d)))
                           (.sort (fn [a b] (- (.-value b) (.-value a))))))]

        [:svg {:viewBox (str (- (/ size 2)) " " (- (/ size 2)) " " size " " size)}
         [:g
          (->> (.descendants partition)
               (filter (fn [d] (not= (.-depth d) 0)))
               (map-indexed (fn [idx d] [:path {:key idx :d (arc d) :fill (color ^js (.-data.name d))}])))]]))}))

(def treemap
  (m/build-chart
   {:title "Treemap"
    :data  (r/atom [])
    :code
    (fn [data]
      (let [size 300
            color (d3/scaleOrdinal d3/schemeCategory10)
            root ((-> (d3/treemap)
                      (.tile d3/treemapBinary)
                      (.size #js [size size]))
                  (-> (d3/hierarchy data)
                      (.sum (fn [d] (.-value d)))
                      (.sort (fn [a b] (- (.-value b) (.-value a))))))]

        [:svg {:viewBox (str 0 " " 0 " " size " " size)}
         [:g
          (->> (.leaves root)
               (map-indexed (fn [idx d]
                              [:rect {:key idx
                                      :x (.-x0 d) :y (.-y0 d)
                                      :width (- (.-x1 d) (.-x0 d))
                                      :height (- (.-y1 d) (.-y0 d))
                                      :stroke "black"
                                      :fill (loop [d d]
                                              (if (> (.-depth d) 1)
                                                (recur (.-parent d))
                                                (color ^js (.-data.name d))))}])))]]))}))

(def chord
  (m/build-chart
   {:title "Chord"
    :data  (r/atom (clj->js
                    [[0.096899, 0.008859, 0.000554, 0.004430, 0.025471, 0.024363, 0.005537, 0.025471],
                     [0.001107, 0.018272, 0.000000, 0.004983, 0.011074, 0.010520, 0.002215, 0.004983],
                     [0.000554, 0.002769, 0.002215, 0.002215, 0.003876, 0.008306, 0.000554, 0.003322],
                     [0.000554, 0.001107, 0.000554, 0.012182, 0.011628, 0.006645, 0.004983, 0.010520],
                     [0.002215, 0.004430, 0.000000, 0.002769, 0.104097, 0.012182, 0.004983, 0.028239],
                     [0.011628, 0.026024, 0.000000, 0.013843, 0.087486, 0.168328, 0.017165, 0.055925],
                     [0.000554, 0.004983, 0.000000, 0.003322, 0.004430, 0.008859, 0.017719, 0.004430],
                     [0.002215, 0.007198, 0.000000, 0.003322, 0.016611, 0.014950, 0.001107, 0.054264]]))
    :code
    (fn [data]
      (let [size 300
            radius (/ size 2)
            color (d3/scaleOrdinal d3/schemeCategory10)
            ribbon (-> (d3/ribbon)
                       (.radius (- radius 1)))
            arc (-> (d3/arc)
                    (.innerRadius (- radius 10))
                    (.outerRadius radius))
            chord (-> (d3/chord)
                      (.sortSubgroups d3/descending)
                      (.sortChords d3/descending))]

        [:svg {:viewBox (str (- radius) " " (- radius) " " size " " size)}
         [:g
          (map-indexed
           (fn [idx group]
             [:path {:key idx :d (ribbon group) :fill (color idx)}])
           (chord data))]
         [:g
          (map-indexed
           (fn [idx group]
             [:path {:d (arc group) :fill (color idx) :key idx}])
           (.-groups (chord data)))]]))}))

(def contour
  (m/build-chart
   {:title "Contour"
    :data  (r/atom #js [])
    :code
    (fn [data]
      (let [path (d3/geoPath)
            interpolateTerrain d3/interpolateTurbo
            color (-> (d3/scaleSequential interpolateTerrain)
                      (.domain (d3/extent (.-values  @(:data contour))))
                      (.nice))
            thresholds (.ticks color 20)
            width (or (.-width data) 0)
            height (or (.-height data) 0)
            contours (-> (d3/contours)
                         (.size (into-array [width height])))]
        [:div
         {:style {:height 399}}
         [:svg {:viewBox (str 0 " " 0 " " width " " height)}
          [:g
           (map-indexed
            (fn [idx threshold]
              [:path {:key idx :d (path (.contour contours (.-values data) threshold))
                      :fill (color threshold)}])
            thresholds)]]]))}))

(def voronoi
  (m/build-chart
   {:title "Voronoi"
    :data  (r/atom [])
    :code
    (fn [data]
      (let [size 300
            delaunay (.from d3/Delaunay (clj->js data))
            voronoi (.voronoi delaunay #js[0.5 0.5 (- size 0.5) (- size 0.5)])]
        [:svg {:viewBox (str 0 " " 0 " " size " " size)}
         [:path {:fill "transparent"
                 :stroke "black"
                 :d (.render voronoi)}]]))}))

(def streamgraph
  (m/build-chart
   {:title "Streamgraph"
    :data  (r/atom [])
    :code
    (fn [data]
      (let [size 300
            data-keys (remove #(= % "date") (map name (keys (first @(:data streamgraph)))))
            series ((-> (d3/stack)
                        (.keys data-keys)
                        (.offset d3/stackOffsetWiggle)
                        (.order d3/stackOrderInsideOut))
                    (clj->js data))
            dates (map :date data)
            x (-> (d3/scaleUtc)
                  (.domain
                   (clj->js
                    [(apply min dates)
                     (apply max dates)]))
                  (.range (clj->js  [0 size])))
            y (-> (d3/scaleLinear)
                  (.domain
                   (clj->js
                    [(apply min (map #(apply min (map first %)) series))
                     (apply max (map #(apply max (map first %)) series))]))
                  (.range (clj->js [0 size])))
            area (-> (d3/area)
                     (.x (fn [d] (x ^js (.-data.date d))))
                     (.y0 (fn [d] (y (first d))))
                     (.y1 (fn [d] (y (second d)))))
            color (-> (d3/scaleOrdinal)
                      (.domain (clj->js data-keys))
                      (.range d3/schemeCategory10))]
        [:svg {:viewBox (str 0 " " 0 " " size " " size)}
         (map-indexed
          (fn [idx d]
            [:path {:key idx :d (area d) :fill (color (.-key d))}])
          series)]))}))

(defn card [children]
  [:div.shadow-lg.border.md:rounded-xl.bg-white.w-full.mb-2.md:mr-16.md:mb-16 {:class "md:w-5/12"}
   children])

(defn chart-container []
  (let [copy-id (random-uuid)]
    (new clipboard ".copy-button")
    (let [active-tab (r/atom :chart)]
      (fn [{:keys [title chart code data]}]
        (let [height (- 393.08 42)]
          [card
           [:<>
            [:div.p-6.md:p-14.border-b
             [:h2.text-3xl.mb-7.font-semibold.tracking-wide
              title]
             [:div
              [:div
               {:class (r/class-names (when-not (= @active-tab :chart) "hidden"))}
               (when @data
                 [chart @data])]
              [:div
               {:class (r/class-names (when-not (= @active-tab :code) "hidden"))}
               [:pre.overflow-auto.mb-4
                {:style {:height height}  :id (str "code" copy-id)}
                (with-out-str (pprint code))]
               [:div.flex.justify-center
                [:button.copy-button.font-bold.border.px-3
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
                [:button.copy-button.font-bold.border.px-3
                 {:data-clipboard-target (str "#data" copy-id)}
                 [:div.flex.items-center.justify-center
                  [:div.w-4.h-4.mr-1 [icon {:name :copy :class "text-gray-600"}]]
                  "copy"]]]]]]
            [:div.flex.divide-x
             [:button.p-5.md:p-6.hover:bg-gray-100
              {:class "w-1/3" :on-click (fn [] (reset! active-tab :chart))}
              [:div.flex.items-center.justify-center
               [:div.w-4.h-4.mr-1 [icon {:name :chart :class "text-gray-600"}]]
               "Chart"]]
             [:button.p-5.md:p-6.hover:bg-gray-100
              {:class "w-1/3" :on-click (fn [] (reset! active-tab :code))}
              [:div.flex.items-center.justify-center
               [:div.w-4.h-4.mr-1 [icon {:name :code :class "text-gray-600"}]]
               "Code"]]
             [:button.p-5.md:p-6.hover:bg-gray-100
              {:class "w-1/3" :on-click (fn [] (reset! active-tab :data))}
              [:div.flex.items-center.justify-center
               [:div.w-4.h-4.mr-1 [icon {:name :data :class "text-gray-600"}]]
               "Data"]]]]])))))
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
(defn parse-unemployment-data [employment-data]
  (-> employment-data
      (update :date #(new js/Date %))
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
      (update :Government js/parseFloat)))

(defn parse-stock-data [stock-data]
  (-> stock-data
      (update :date #(new js/Date %))
      (update :close js/parseFloat)))

(defn parse-energy-data [energy-data]
  (-> energy-data
      (update :value js/parseFloat)))

(defn fetch-json [url]
  (-> (js/fetch url)
      (.then (fn [res] (.json res)))
      (.catch (fn [res] (prn res)))))

(defn app []
  (-> (fetch-json "data/frequencies.json")
      (.then (fn [res] (reset! (:data bar) (js->clj res :keywordize-keys true)))))
  (-> (fetch-json "data/population-by-age.json")
      (.then (fn [res] (reset! (:data pie) (js->clj res :keywordize-keys true)))))
  (-> (js/fetch "data/apple-stock.csv")
      (.then (fn [res] (.text res)))
      (.then (fn [res] (reset! (:data line) ((comp #(map parse-stock-data %) csv->clj) res))))
      (.catch (fn [res] (prn res))))
  (-> (js/fetch "data/energy.csv")
      (.then (fn [res] (.text res)))
      (.then (fn [res] (reset! (:data sankey) ((comp #(map parse-energy-data %) csv->clj) res))))
      (.catch (fn [res] (prn res))))
  (-> (js/fetch "data/unemployment.csv")
      (.then (fn [res] (.text res)))
      (.then (fn [res] (reset! (:data streamgraph) ((comp #(map parse-unemployment-data %) csv->clj) res))))
      (.catch (fn [res] (prn res))))
  (-> (fetch-json "data/flare-2.json")
      (.then (fn [res]
               (reset! (:data pack) res)
               (reset! (:data tree) res)
               (reset! (:data sunburst) res)
               (reset! (:data treemap) res))))
  (-> (fetch-json "data/countries.json")
      (.then (fn [res] (reset! (:data world-map) res))))
  (-> (fetch-json "data/miserables.json")
      (.then (fn [res] (reset! (:data graph) res))))
  (-> (fetch-json "data/volcano.json")
      (.then (fn [res] (reset! (:data contour) res))))
  (reset! (:data voronoi) (map (fn [] [(rand 300) (rand 300)]) (range 100)))
  (fn []
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
      [:a.text-white.underline {:href "https://github.com/rollacaster/hiccup-d3"} "Code"]]]))

(dom/render [app] (js/document.getElementById "root"))

(defn init [])

