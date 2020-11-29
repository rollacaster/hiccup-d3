(ns tech.thomas-sojka.shadow-tailwind.dev-hook
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]))

(defn build-dev-css
  {:shadow.build/stage :configure}
  [{:shadow.build/keys [mode] :as build-state} source target]
  (let [source-file (io/file source)]
    (io/make-parents target)
    (let [{:keys [exit err]}
          (shell/sh "npx" "postcss" source "-o" target
                    :env (assoc (into {} (System/getenv)) "NODE_ENV" (if (= mode :release) "production" "")))]
      (when (= exit 1)
        (throw (Exception. err))))
    (assoc build-state ::source-last-mod (.lastModified source-file))))

