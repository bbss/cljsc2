(ns cljsc2.clj.build-order-test
  (:require
   [datascript.core :as ds])
  (:use
   cljsc2.clj.core
   cljsc2.clj.rules
   cljsc2.clj.notebook.core))

(def planning (atom {}))

(def conn (get-conn server-db 5000))

(load-simple-map conn)

(defn build
  ([unit] (build unit {}))
  ([unit config]
   ;;do -s from string
   ;;classcase, name on keyword
   ;;building scvs until a certain limit
   ;;not filling queue over 1
   ;; not too many at same time
   ;; one in the start
   ;; not when there is a big difference between food and cap
   ;; selecting builders from pool to avoid conflicting commands
   ;; decide between build new and wait for queue to finish
   ;; make sure build x command does not stack
   ))

(defn quick-save [conn]
  (send-request-and-get-response-message
   conn #:SC2APIProtocol.sc2api$RequestQuickSave{:quick-save {}}))

(defn quick-load [conn]
  (send-request-and-get-response-message
   conn #:SC2APIProtocol.sc2api$RequestQuickLoad{:quick-load {}}))

(cljsc2.clj.core/send-action-and-get-response
 conn
 #:SC2APIProtocol.sc2api$Action
 {:action-render #:SC2APIProtocol.spatial$ActionSpatial
  {:action #:SC2APIProtocol.spatial$ActionSpatial
   {:unit-selection-rect #:SC2APIProtocol.spatial$ActionSpatialUnitSelectionRect
    {:selection-screen-coord
     [#:SC2APIProtocol.common$RectangleI
      {:p0 #:SC2APIProtocol.common$PointI{:x 0 :y 0}
       :p1 #:SC2APIProtocol.common$PointI{:x 100 :y 100}}
      ]
     }
    }}})

(send-action-and-get-response
 conn
 #:SC2APIProtocol.sc2api$Action
 {:action-render #:SC2APIProtocol.spatial$ActionSpatial
  {:action #:SC2APIProtocol.spatial$ActionSpatial
   {:unit-selection-point #:SC2APIProtocol.spatial$ActionSpatialUnitSelectionPoint
    {:selection-screen-coord #:SC2APIProtocol.common$PointI{:x 27 :y 62}
     :type "Select"}}}})

(send-action-and-get-response
 conn
 #:SC2APIProtocol.sc2api$Action{:action-render #:SC2APIProtocol.spatial$ActionSpatial{:action #:SC2APIProtocol.spatial$ActionSpatial{:unit-selection-rect #:SC2APIProtocol.spatial$ActionSpatialUnitSelectionRect{:selection-screen-coord [#:SC2APIProtocol.common$RectangleI{:p0 #:SC2APIProtocol.common$PointI{:x 28.666666666666668, :y 64.37946428571429}, :p1 #:SC2APIProtocol.common$PointI{:x 0, :y 0}}], :selection-add true}}}})
(request-step conn 1)
(quit conn)
(comment
  :scv build one scv
  (build :scvs)
  (build "supply-depot")
  (build 'barracks)
  (build "marines")

  (expand)
  ;; Max per amount of expands and strategy
  (rally barracks)

  (attack-at 50 supp))
