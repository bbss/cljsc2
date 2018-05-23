(ns cljsc2.cljs.content-script
  (:require-macros [chromex.support :refer [runonce]])
  (:require [cljsc2.cljs.content-script.core :as core]))

(js/setTimeout #(runonce
                 (core/init!))
               1000)