(ns cljsc2.cljs.selectors)

(defn latest-observation-from-runs [runs]
  (-> runs
      last :run/observations last))
