(ns cljsc2.cljs.background
  (:require-macros [chromex.support :refer [runonce]])
  (:require [cljsc2.cljs.background.core :as core]))

(runonce
 (core/init!))
