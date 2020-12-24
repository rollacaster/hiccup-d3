(ns tech.thomas-sojka.hiccup-d3.macros)

(defmacro build-chart [{:keys [title code load]}]
  `(let [data# (r/atom nil)]
     (-> (~load)
         (.then (fn [res#] (reset! data# res#)))
         (.catch (fn [e#] (prn e#))))
     {:title ~title
      :data data#
      :chart (fn [] (~code @data#))
      :code  '~(last code)}))


