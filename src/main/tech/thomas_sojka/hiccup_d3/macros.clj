(ns tech.thomas-sojka.hiccup-d3.macros
  (:require [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [glow.core :as glow]
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

(defmacro build-chart [title code]
  `{:title ~title
    :d3-apis ~(mapv (fn [fn] {:doc-link (d3-doc-link fn) :fn fn}) (d3-fns code))
    :code-formatted ~(glow/highlight-html (with-out-str (pprint (last code))))
    :chart (fn [data#] (~code data#))})

(spit "public/css/glow.css" (glow/generate-css
                             {:background "white"
                              :exception "#859900"
                              :repeat "#859900"
                              :conditional "#859900"
                              :variable "#268bd2"
                              :core-fn "#586e75"
                              :definition "#cb4b16"
                              :reader-char "#dc322f"
                              :special-form "#859900"
                              :macro "#859900"
                              :number "#2aa198"
                              :boolean "#2aa198"
                              :nil "#2aa198"
                              :s-exp "#586e75"
                              :keyword "#268bd2"
                              :comment "#586e75"
                              :string "#2aa198"
                              :character "#2aa198"
                              :regex "#dc322f"
                              :symbol "#586e75"}))

