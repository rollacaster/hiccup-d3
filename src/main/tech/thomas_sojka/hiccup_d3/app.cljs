(ns tech.thomas-sojka.hiccup-d3.app
  (:require ["clipboard" :as clipboard]
            [cljs.pprint :refer [pprint]]
            [reagent.core :as r]
            [reagent.dom :as dom]
            [tech.thomas-sojka.hiccup-d3.charts.bar :refer [bar]]
            [tech.thomas-sojka.hiccup-d3.charts.pie :refer [pie]]
            [tech.thomas-sojka.hiccup-d3.charts.pack :refer [pack]]
            [tech.thomas-sojka.hiccup-d3.charts.tree :refer [tree]]
            [tech.thomas-sojka.hiccup-d3.charts.world-map :refer [world-map]]
            [tech.thomas-sojka.hiccup-d3.charts.sankey :refer [sankey]]
            [tech.thomas-sojka.hiccup-d3.charts.sunburst :refer [sunburst]]
            [tech.thomas-sojka.hiccup-d3.charts.graph :refer [graph]]
            [tech.thomas-sojka.hiccup-d3.charts.line :refer [line]]
            [tech.thomas-sojka.hiccup-d3.charts.treemap :refer [treemap]]
            [tech.thomas-sojka.hiccup-d3.charts.chord :refer [chord]]
            [tech.thomas-sojka.hiccup-d3.charts.voronoi :refer [voronoi]]
            [tech.thomas-sojka.hiccup-d3.charts.streamgraph :refer [streamgraph]]
            [tech.thomas-sojka.hiccup-d3.charts.contour :refer [contour]]))

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
          "Hiccup D3-Charts in ClojureScript"]]
        [:div.text-lg.hidden.md:block {:class "w-1/2"}
         [:div.max-w-md
          "Use these starting points to create a new chart with "
          [:a.underline {:href "https://github.com/weavejester/hiccup"} "hiccup"]
          ". No functionality was wrapped, access the full "
          [:a.underline
           {:href "https://github.com/d3/d3/blob/master/API.md"} "D3 API"] ". The
          example code assumes D3 is already required."]]]]]
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

