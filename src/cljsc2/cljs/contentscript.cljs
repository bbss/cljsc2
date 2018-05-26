(ns cljsc2.cljs.contentscript
  (:require-macros [chromex.support :refer [runonce]])
  (:require [cljsc2.cljs.contentscript.core :as core]))
(println "loadiong contentscript")
(runonce
 (core/init!))
