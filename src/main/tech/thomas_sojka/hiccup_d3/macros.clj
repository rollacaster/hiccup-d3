(ns tech.thomas-sojka.hiccup-d3.macros
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [markdown-to-hiccup.core :as m]))

(def doc-hiccup
  (-> "https://raw.githubusercontent.com/d3/d3/master/API.md"
      slurp
      (m/md->hiccup)))

(defn find-call-in-doc-item [call doc-item]
  (and (string? doc-item)
       (str/includes? doc-item call)
       (= call
          (last
           (re-matches
            #"^https:\/\/github\.com\/d3\/.*\/blob\/v.*\/README\.md#(.*)$"
            doc-item)))))

(defn d3-doc-link [call]
  (cond
    (= call "sankey") "https://github.com/d3/d3-sankey#_sankey"
    (= call "sankeyLinkHorizontal") "https://github.com/d3/d3-sankey#sankeyLinkHorizontal"
    :else
    (let [d3-api (atom nil)]
      (walk/postwalk
       (fn [doc-item]
         (when (find-call-in-doc-item (case call
                                        "Delaunay" "new_Delaunay"
                                        call)
                                      doc-item)
           (reset! d3-api doc-item)))
       doc-hiccup)
      @d3-api)))

(defn d3-fns [code]
  (let [d3-calls (atom [])]
    (walk/postwalk
     (fn [item]
       (when (and (symbol? item) (str/includes? item "d3"))
         (swap! d3-calls conj (name item))))
     code)
    (set @d3-calls)))

(defmacro build-chart [{:keys [title code load]}]
  `(let [data# (r/atom nil)]
     (-> (~load)
         (.then (fn [res#] (reset! data# res#)))
         (.catch (fn [e#] (prn e#))))
     {:title ~title
      :data data#
      :d3-apis ~(mapv (fn [fn] {:doc-link (d3-doc-link fn) :fn fn})
                      (d3-fns code))
      :chart (fn [data#]
               (~code data#))
      :code  '~(last code)}))

(defmacro variant [title code]
  `{:title ~title
    :d3-apis ~(mapv (fn [fn] {:doc-link (d3-doc-link fn) :fn fn}) (d3-fns code))
    :code '~(last code)
    :chart (fn [data#] (~code data#))})

