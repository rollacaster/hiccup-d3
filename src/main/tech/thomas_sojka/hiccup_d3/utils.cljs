(ns tech.thomas-sojka.hiccup-d3.utils
  (:require [clojure.string :as str]))

(defn fetch-json [url]
  (-> (js/fetch url)
      (.then (fn [res] (.json res)))
      (.catch (fn [res] (prn res)))))

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
