(ns cljsc2.clj.core
  (:use flatland.protobuf.core
        [clojure.java.shell :only [sh]]
        lucid.mind)
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.spec.gen.alpha :as gen]
   [clojure.test.check]
   [flatland.protobuf.schema :only [field-schema]]
   [flatland.useful.fn :only [fix]]
   [clojure.java.io :only [input-stream output-stream]]
   [aleph.http :as http]
   [manifold.stream :as s]
   flatland.useful.utils)
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


(future
  (sh "/Applications/StarCraft II/Versions/Base55958/SC2.app/Contents/MacOS/SC2"
      "-listen" "127.0.0.1" "-port" "5000" "-displayMode" "0"))

(def Request
  (protodef SC2APIProtocol.Sc2Api$Request))

(def RequestCreateGame
  (protodef SC2APIProtocol.Sc2Api$RequestCreateGame))

(def LocalMap
  (protodef SC2APIProtocol.Sc2Api$LocalMap))

(def connection (http/websocket-client "ws://127.0.0.1:5000/sc2api"))

(def RequestPing (protodef SC2APIProtocol.Sc2Api$RequestPing))

(def PlayerSetup (protodef SC2APIProtocol.Sc2Api$PlayerSetup))

(def player-builder
  (doto (SC2APIProtocol.Sc2Api$PlayerSetup/newBuilder)
    (.setType  (SC2APIProtocol.Sc2Api$PlayerType/forNumber 1))
    (.setRace  (SC2APIProtocol.Sc2Api$Race/forNumber 1))))

(s/put! @connection
        (protobuf-dump
         (protobuf Request :create-game
                   (protobuf RequestCreateGame
                             :local-map
                             {:map-path "/Applications/StarCraft II/Maps/Melee/Simple64.SC2Map"}
                             :player-setup [(protobuf-load PlayerSetup (.toByteArray (.build player-builder)))]))))

(s/put! @connection
        (protobuf-dump
         (protobuf Request :ping (protobuf (protodef SC2APIProtocol.Sc2Api$RequestPing)))))

(def took (s/take! @connection))

(def Response
  (protodef SC2APIProtocol.Sc2Api$Response))

(protobuf-load Response @took)

(protobuf-load (protodef SC2APIProtocol.Sc2Api$ResponseJoinGame) @took)

(protobuf (protodef SC2APIProtocol.Sc2Api$PlayerSetup))

(protobuf-schema SC2APIProtocol.Sc2Api$PlayerSetup)

(protobuf-schema Request)

(def RequestJoinGame (protodef SC2APIProtocol.Sc2Api$RequestJoinGame))

(defn send-request [connection request]
  (s/put! @connection
          (protobuf-dump request)))

(def join-game-request
  (protobuf Request :join-game
            (protobuf-load RequestJoinGame
                           (-> (doto (SC2APIProtocol.Sc2Api$RequestJoinGame/newBuilder)
                                 (.setRace (SC2APIProtocol.Sc2Api$Race/forNumber 1))
                                 (.setOptions (-> (doto (SC2APIProtocol.Sc2Api$InterfaceOptions/newBuilder))
                                                  .build))
                                 )
                               .build
                               .toByteArray))))

(comment
  (clojure.spec.alpha/conform
   :SC2APIProtocol.sc2api/Request
   #:SC2APIProtocol.sc2api$Request
   {:request #:SC2APIProtocol.sc2api$RequestJoinGame
    {:join-game {:participation #:SC2APIProtocol.sc2api$RequestJoinGame{:race "Random"}}}})

  (send-request
   connection
   (protobuf-load
    Request
    (.toByteArray
     (cljsc2.clj.proto/make-protobuf
      #:SC2APIProtocol.sc2api$Request
      {:request #:SC2APIProtocol.sc2api$RequestCreateGame
       {:create-game #:SC2APIProtocol.sc2api$RequestCreateGame
        {:map #:SC2APIProtocol.sc2api$RequestCreateGame{:local-map {:map-path "/Applications/StarCraft II/Maps/Melee/Simple64.SC2Map"}}
         :player-setup [#:SC2APIProtocol.sc2api$PlayerSetup{:race "Zerg" :type "Participant"}]}}}))))

  (send-request
   connection
   (protobuf-load
    Request
    (.toByteArray
     (cljsc2.clj.proto/make-protobuf
      #:SC2APIProtocol.sc2api$Request
      {:request #:SC2APIProtocol.sc2api$RequestJoinGame
       {:join-game
        {:participation
         #:SC2APIProtocol.sc2api$RequestJoinGame{:race "Random" :options {}}}}})))))
