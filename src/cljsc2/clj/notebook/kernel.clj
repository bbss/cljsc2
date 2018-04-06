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

(comment
  (ugly-make-protobuf #:SC2APIProtocol.sc2api$Action
                      {:action-render #:SC2APIProtocol.spatial$ActionSpatial
                       {:action #:SC2APIProtocol.spatial$ActionSpatial
                        {:unit-selection-rect #:SC2APIProtocol.spatial$ActionSpatialUnitSelectionRect
                         {:selection-screen-coord
                          [#:SC2APIProtocol.common$RectangleI
                           {:p0 #:SC2APIProtocol.common$PointI{:x 20 :y 66}
                            :p1 #:SC2APIProtocol.common$PointI{:x 29 :y 60}}]
                          :selection-add true}
                         }}})
  (ugly-make-protobuf #:SC2APIProtocol.sc2api$Action
                      {:action-render #:SC2APIProtocol.spatial$ActionSpatial
                       {:action #:SC2APIProtocol.spatial$ActionSpatial
                        {:unit-selection-point #:SC2APIProtocol.spatial$ActionSpatialUnitSelectionPoint
                         {:selection-screen-coord #:SC2APIProtocol.common$PointI{:x 26 :y 60}}
                         }}}))
