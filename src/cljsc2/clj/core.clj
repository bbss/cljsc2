(ns cljsc2.clj.core
  (:require
   [cljsc2.clj.proto :as proto]
   [clojure.spec.alpha :as spec]
   [clojure.spec.gen.alpha :as gen]
   [clojure.test.check]
   [perseverance.core :as p]
   [flatland.protobuf.schema :only [field-schema]]
   [flatland.useful.fn :only [fix]]
   [clojure.java.io :only [input-stream output-stream]]
   [aleph.http :as http]
   [manifold.stream :as s]
   [manifold.deferred :as d]
   [me.raynes.conch.low-level :as sh]
   flatland.useful.utils
   [flatland.protobuf.core :refer [protodef protobuf-dump protodef? protobuf-load]])
  (:import
   (flatland.protobuf PersistentProtocolBufferMap PersistentProtocolBufferMap$Def PersistentProtocolBufferMap$Def$NamingStrategy Extensions)
   (com.google.protobuf GeneratedMessage CodedInputStream Descriptors$Descriptor Descriptors$EnumDescriptor)
   (java.io InputStream OutputStream)
   (clojure.lang Reflector)
   SC2APIProtocol.Raw
   SC2APIProtocol.Ui
   SC2APIProtocol.Data
   SC2APIProtocol.Debug
   SC2APIProtocol.Query
   SC2APIProtocol.Common
   SC2APIProtocol.Sc2Api
   SC2APIProtocol.Spatial
   SC2APIProtocol.ScoreOuterClass))

(declare sc-process)
(declare connection)

(defn send-request [connection request]
  (s/put! connection
          (.toByteArray
           (proto/make-protobuf
            #:SC2APIProtocol.sc2api$Request
            {:request request}))))

(defn quit [connection]
  (send-request
   connection
   #:SC2APIProtocol.sc2api$RequestQuit{:quit {}}))

(defn stop []
  (try (quit connection)
       (catch Exception e (do 'nothing)))
  (try (sh/destroy sc-process)
       (catch Exception e (do 'nothing))))

(defn restart-conn []
  (p/retry {:strategy (p/constant-retry-strategy 2000 10)}
    (p/retriable
        {}
        (def connection
          @(http/websocket-client "ws://127.0.0.1:5000/sc2api"
                                  {:max-frame-payload 524288})))))

(defn start []
  (stop)
  (def sc-process
    (sh/proc "/Applications/StarCraft II/Versions/Base55958/SC2.app/Contents/MacOS/SC2"
             "-listen" "127.0.0.1" "-port" "5000" "-displayMode" "0"))
  (restart-conn))

(def Request
  (protodef SC2APIProtocol.Sc2Api$Request))

(def Response
  (protodef SC2APIProtocol.Sc2Api$Response))

(defn latest-response [connection]
  (if (>
       (-> connection
            s/description
            :source
            :buffer-size)
       0)
    (s/take! connection)))

(defn latest-response-protobuf [connection]
  (let [res (latest-response connection)]
    (if res
      (SC2APIProtocol.Sc2Api$Response/parseFrom @res)
      :none)))

(defn latest-response-message [connection]
  (let [res (latest-response connection)]
    (if res
      (protobuf-load Response @res)
      :none)))


(comment
  (d/chain
   (send-request
    connection
    #:SC2APIProtocol.sc2api$RequestCreateGame
    {:create-game #:SC2APIProtocol.sc2api$RequestCreateGame
     {:map #:SC2APIProtocol.sc2api$RequestCreateGame
      {:local-map
       {:map-path "/Applications/StarCraft II/Maps/Melee/Simple64.SC2Map"}}
      :player-setup
      [#:SC2APIProtocol.sc2api$PlayerSetup
       {:race "Zerg" :type "Participant"}
       #:SC2APIProtocol.sc2api$PlayerSetup
       {:race "Protoss" :type "Computer"}]}})
   (send-request
    connection
    #:SC2APIProtocol.sc2api$RequestJoinGame
    {:join-game
     {:participation
      #:SC2APIProtocol.sc2api$RequestJoinGame{:race "Zerg" :options {}}}}))

  (d/chain
   (send-request
    connection
    #:SC2APIProtocol.sc2api$RequestCreateGame
    {:create-game #:SC2APIProtocol.sc2api$RequestCreateGame
     {:map #:SC2APIProtocol.sc2api$LocalMap
      {:local-map
       {:map-path "/Applications/StarCraft II/Maps/mini_games/CollectMineralShards.SC2Map"}}
      :player-setup
      [#:SC2APIProtocol.sc2api$PlayerSetup
       {:race "Terran" :type "Participant"}
       #:SC2APIProtocol.sc2api$PlayerSetup
       {:race "Protoss" :type "Computer"}]}})

   (send-request
    connection
    #:SC2APIProtocol.sc2api$RequestJoinGame
    {:join-game
     #:SC2APIProtocol.sc2api$RequestJoinGame
     {:participation
      #:SC2APIProtocol.sc2api$RequestJoinGame
      {:race "Terran"}
      :options #:SC2APIProtocol.sc2api$InterfaceOptions
      {:raw true
       :feature-layer #:SC2APIProtocol.sc2api$SpatialCameraSetup
       {:width 24
        :resolution #:SC2APIProtocol.common$Size2DI{:x 84 :y 84}
        :minimap-resolution #:SC2APIProtocol.common$Size2DI{:x 64 :y 64}
        }
       }}}))

  (send-request
   connection
   #:SC2APIProtocol.sc2api$RequestStep{:step {}})

  (send-request
   connection
   #:SC2APIProtocol.sc2api$RequestObservation{:observation {}})
  (send-request
   connection
   #:SC2APIProtocol.sc2api$RequestData
   {:data #:SC2APIProtocol.sc2api$RequestData
    {:ability-id true
     :unit-type-id true
     }})

  (send-request
   connection
   #:SC2APIProtocol.sc2api$Request
   {:available-maps {}})

  (send-request
   connection
   #:SC2APIProtocol.sc2api$RequestAction
   {:action #:SC2APIProtocol.sc2api$RequestAction
    {:actions
     [#:SC2APIProtocol.sc2api$Action
      {:action-raw #:SC2APIProtocol.raw$ActionRaw
       {:action #:SC2APIProtocol.raw$ActionRaw
        {:unit-command #:SC2APIProtocol.raw$ActionRawUnitCommand
         {:target #:SC2APIProtocol.raw$ActionRawUnitCommand
          {:target-world-space-pos #:SC2APIProtocol.common$Point2D
           {:x 50 :y 50}}
          :ability-id 16
          :queue-command false
          :unit-tags (->> obs
                          :observation
                          :observation
                          :raw-data
                          :units
                          (filter :is-selected)
                          (map :tag)
                          )}}}}]}})

  (send-request
   connection
   #:SC2APIProtocol.sc2api$RequestAction
   {:action #:SC2APIProtocol.sc2api$RequestAction
    {:actions
     [#:SC2APIProtocol.sc2api$Action
      {:action-ui #:SC2APIProtocol.ui$ActionUI
       {:action
        #:SC2APIProtocol.ui$ActionUI{:select-army {}}}}]}})

  (send-request
   connection
   #:SC2APIProtocol.sc2api$RequestAction
   {:action #:SC2APIProtocol.sc2api$RequestAction
    {:actions
     [#:SC2APIProtocol.sc2api$Action
      {:action-feature-layer #:SC2APIProtocol.spatial$ActionSpatial
       {:action #:SC2APIProtocol.spatial$ActionSpatial
        {:unit-selection-rect #:SC2APIProtocol.spatial$ActionSpatialUnitSelectionRect
         {:selection-add true,
          :selection-screen-coord
          [#:SC2APIProtocol.common$RectangleI
           {:p1 #:SC2APIProtocol.common$PointI{:x 0, :y 0},
            :p0 #:SC2APIProtocol.common$PointI{:x 60, :y 60}}]}}}}]}})
  (send-request
   connection
   #:SC2APIProtocol.sc2api$RequestStep{:step {}})

  )
