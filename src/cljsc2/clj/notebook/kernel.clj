(ns cljsc2.clj.kernel
  (:require
   [perseverance.core :as p]
   [fulcro.easy-server :as easy]
   [me.raynes.conch :refer [programs with-programs let-programs] :as hsh]
   [me.raynes.conch.low-level :as sh]
   [cheshire.core :as cheshire]
   [clojure.string :refer [lower-case includes?]]
   [clojure.spec.alpha :as spec]
   [clojure.spec.gen.alpha :as gen]
   [expound.alpha :as expound]
   [clojure.java.io :as io]
   [manifold
    [stream :as s :refer [stream connect]]
    [bus :refer [event-bus publish! subscribe]]]
   [cognitect.transit :as transit]
   [aleph.http :as aleph]
   [taoensso.sente.server-adapters.aleph :refer (get-sch-adapter)]
   [taoensso.sente.packers.transit :as sente-transit]
   [taoensso.sente :as sente]
   [taoensso.timbre :as timbre]
   [clojupyter.core :as cljp]
   [datascript.core :as ds]
   )
  (:use cljsc2.clj.rules
        cljsc2.clj.game-parsing
        cljsc2.clj.build-order)
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(declare connection)
