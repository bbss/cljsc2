(ns cljsc2.clj.kernel
  (:require
   cljsc2.clj.rules
   [perseverance.core :as p]
   [fulcro.easy-server :as easy]
   [me.raynes.conch :refer [programs with-programs let-programs] :as hsh]
   [me.raynes.conch.low-level :as sh]
   [cheshire.core :as cheshire]
   [clojure.string :refer [lower-case includes?]]
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
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))
