(ns tech.thomas-sojka.hiccup-d3.macros)

(defmacro build-chart [{:keys [title data code]}]
  `{:title ~title
    :data  ~data
    :chart ~code
    :code  '~(last code)})
