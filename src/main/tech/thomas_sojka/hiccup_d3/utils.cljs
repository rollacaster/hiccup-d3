(ns tech.thomas-sojka.hiccup-d3.utils)

(defn fetch-json [url]
  (-> (js/fetch url)
      (.then (fn [res] (.json res)))
      (.catch (fn [res] (prn res)))))
