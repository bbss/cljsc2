(ns cljsc2.clj.core
  (:require
   [cljsc2.clj.proto :as proto]
   [clojure.spec.alpha :as spec]
   [clojure.spec.gen.alpha :as gen]
   [clojure.test.check]
   [manifold.stream :as s :refer [stream]]
   [manifold.deferred :as d]
   [aleph.http :as http]
   [perseverance.core :as p]
   [flatland.protobuf.schema :only [field-schema]]
   [flatland.useful.fn :only [fix]]
   [me.raynes.conch.low-level :as sh]
   flatland.useful.utils
   [flatland.protobuf.core :refer [protodef protobuf-dump protodef? protobuf-load]])
  (:import
   (clojure.lang Reflector)
   SC2APIProtocol.Raw
   SC2APIProtocol.Ui
   SC2APIProtocol.Data
   SC2APIProtocol.Debug
   SC2APIProtocol.Query
   SC2APIProtocol.Common
   SC2APIProtocol.Sc2Api
   SC2APIProtocol.Spatial
   SC2APIProtocol.ScoreOuterClass)
  )

(declare sc-process)
(declare connection)

(defn to-byte [it]
  (.toByteArray it))

(defn send-request [connection request]
  (s/put! connection
          (to-byte
           (proto/memoized-make-protobuf
                   #:SC2APIProtocol.sc2api$Request
                   {:request request}))))

(defn quit [connection]
  (send-request
   connection
   #:SC2APIProtocol.sc2api$RequestQuit{:quit {}}))

(defn stop []
  (try (quit connection)
       (catch Error e (do 'nothing))
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
  (loop [buffer-size (-> connection
                         s/description
                         :source
                         :buffer-size)]
    (if (= buffer-size 0)
      nil
      (if (= buffer-size 1)
        (s/take! connection)
        (do (s/take! connection)
            (recur (-> connection
                       s/description
                       :source
                       :buffer-size)))))))

(defn response [connection]
  (if (>
       (-> connection
           s/description
           :source
           :buffer-size)
       0)
    (s/take! connection)))

(defn response-message [connection]
  (let [res (response connection)]
    (if res
      (protobuf-load Response @res)
      nil)))

(defn latest-response-protobuf [connection]
  (let [res (latest-response connection)]
    (if res
      (SC2APIProtocol.Sc2Api$Response/parseFrom @res)
      nil)))

(defn latest-response-message [connection]
  (let [res (latest-response connection)]
    (if res
      (protobuf-load Response @res)
      nil)))

(defn flush-incoming-responses
  ([] (flush-incoming-responses connection))
  ([connection]
   (loop [res (latest-response-message connection)]
     (if (not (identical? res nil))
       (recur (latest-response-message connection))
       :done))))

(defn send-request-and-get-response-message [connection request]
  @(send-request connection request)
  (loop [res (latest-response-message connection)]
    (if (identical? res nil)
      (recur (latest-response-message connection))
      res)))

(defn send-action-and-get-response [connection action]
  (send-request-and-get-response-message
   connection
   #:SC2APIProtocol.sc2api$RequestAction
   {:action #:SC2APIProtocol.sc2api$RequestAction
    {:actions
     [action]}}))

(defn request-step
  ([] (request-step connection))
  ([connection]
   (send-request-and-get-response-message
    connection
    #:SC2APIProtocol.sc2api$RequestStep{:step {}})))

(defn load-mineral-game
  ([] (load-mineral-game connection))
  ([connection]
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
         }}}}))))

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

  (send-request-and-get-response-message
   connection
   #:SC2APIProtocol.sc2api$RequestStep{:step {}})

  (send-request-and-get-response-message
   connection
   #:SC2APIProtocol.sc2api$RequestObservation{:observation {}})

  (send-request
   connection
   #:SC2APIProtocol.sc2api$RequestData
   {:data #:SC2APIProtocol.sc2api$RequestData
    {:ability-id true
     :unit-type-id true}})

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
           {:x 29 :y 25}}
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

  (send-request-and-get-response-message
   connection
   #:SC2APIProtocol.sc2api$RequestAction
   {:action #:SC2APIProtocol.sc2api$RequestAction
    {:actions
     [#:SC2APIProtocol.sc2api$Action
      {:action-ui #:SC2APIProtocol.ui$ActionUI
       {:action
        #:SC2APIProtocol.ui$ActionUI{:select-army {}}}}]}})

  (send-request-and-get-response-message
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
           {:p1 #:SC2APIProtocol.common$PointI{:x 84, :y 84},
            :p0 #:SC2APIProtocol.common$PointI{:x 0, :y 0}}]}}}}]}})

  (send-request
   connection
   #:SC2APIProtocol.sc2api$RequestStep{:step {}}))


(defn after-each-step [connection incoming-step-observations]
  "Responsible for adding new observations to the incoming observation stream. Steps through the game and sends the actions from the agents step function to the game. Closes when the agent returns falsy for actions"
  (fn [step-actions]
    (if step-actions
      (do (when (not (empty? step-actions))
            (send-request-and-get-response-message
             connection
             #:SC2APIProtocol.sc2api$RequestAction
             {:action #:SC2APIProtocol.sc2api$RequestAction
              {:actions
               (if (map? step-actions)
                 [step-actions]
                 step-actions)}}))
          (send-request-and-get-response-message
           connection
           #:SC2APIProtocol.sc2api$RequestStep{:step {}})
          (s/put!
           incoming-step-observations
           (send-request-and-get-response-message
            connection
            #:SC2APIProtocol.sc2api$RequestObservation
            {:observation {}})))
      (s/close! incoming-step-observations))))

(defn set-up-stepper [connection
                      incoming-step-observations
                      {:keys [throttle-max-per-second
                              step-fn
                              sink] :as stepper}
                      observation-meta-fn]
  (let [stepper-results (or sink (stream))]
    (if throttle-max-per-second
      (let [intermediate (stream)
            throttled-src (when throttle-max-per-second
                            (s/throttle throttle-max-per-second intermediate))]
        (s/consume
         (if observation-meta-fn
           (fn [step-observation]
             (s/try-put!
              intermediate
              (step-fn (with-meta
                                step-observation
                                (observation-meta-fn (System/currentTimeMillis)))
                              connection)
              0))
           (fn [step-observation]
             (when (= 0 (:pending-puts (s/description intermediate)))
               (s/put!
                intermediate
                (step-fn step-observation connection)
                ))))
         incoming-step-observations)
        (s/connect throttled-src stepper-results stepper))
      (s/connect-via
       incoming-step-observations
       (if observation-meta-fn
         (fn [step-observation]
           (s/put! stepper-results
                   (step-fn (with-meta
                                     step-observation
                                     (observation-meta-fn (System/currentTimeMillis)))
                                   connection)))
         (fn [step-observation]
           (s/put! stepper-results (step-fn step-observation connection))))
       stepper-results))
    stepper-results))

(defn set-up-additional-steppers [additional-steppers connection incoming-step-observations observation-meta-fn]
  (doall (map (fn [additional-stepper]
                (set-up-stepper
                 connection
                 incoming-step-observations
                 additional-stepper
                 observation-meta-fn))
              additional-steppers)))

(defn run-loop [connection
                {:keys [sink
                        description
                        throttle-max-per-second
                        step-fn] :as stepper}
                {:keys [observation-meta-fn
                        additional-steppers]
                 :or {additional-steppers []}
                 :as run-config}]
  (flush-incoming-responses connection)
  (let [incoming-step-observations (stream)
        additional-stepper-streams (set-up-additional-steppers additional-steppers connection incoming-step-observations observation-meta-fn)
        step-action-stream (set-up-stepper connection
                                           incoming-step-observations
                                           stepper
                                           observation-meta-fn)]
    {:stepping (future (s/put! incoming-step-observations
                               (send-request-and-get-response-message
                                connection
                                #:SC2APIProtocol.sc2api$RequestObservation
                                {:observation {}})))
     :consume-step-actions (s/consume
                            (after-each-step connection incoming-step-observations)
                            step-action-stream)
     :step-action-stream step-action-stream
     :incoming-step-observations-stream incoming-step-observations
     :additional-stepper-streams additional-stepper-streams}))

(def counter (atom 0))

(defn step [observation connection]
  (swap! counter inc)
  (when (< @counter 2200)
    (if (or (or (= @counter 1) (= @counter 2)) (= (mod @counter 50) 0))
      (if (empty? (->> observation
                            :observation
                            :observation
                            :raw-data
                            :units
                            (filter :is-selected)
                            (map :tag)
                            ))
        #:SC2APIProtocol.sc2api$Action
        {:action-ui #:SC2APIProtocol.ui$ActionUI
         {:action
          #:SC2APIProtocol.ui$ActionUI{:select-army {}}}}
        #:SC2APIProtocol.sc2api$Action
        {:action-raw #:SC2APIProtocol.raw$ActionRaw
         {:action #:SC2APIProtocol.raw$ActionRaw
          {:unit-command #:SC2APIProtocol.raw$ActionRawUnitCommand
           {:target #:SC2APIProtocol.raw$ActionRawUnitCommand
            {:target-world-space-pos #:SC2APIProtocol.common$Point2D
             {:x (rand-nth (range 15 45)) :y (rand-nth (range 20 40))}}
            :ability-id 16
            :queue-command false
            :unit-tags (->> observation
                            :observation
                            :observation
                            :raw-data
                            :units
                            (filter :is-selected)
                            (map :tag)
                            )}}}})
      [])))

(comment
  (start)

  (restart-conn)

  (load-mineral-game)

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

  (def counter (atom 0))

  (def running-loop
    (run-loop
     connection
     {:step-fn cljsc2.clj.agent/step
      :description "steps -> agent"}
     {:additional-steppers
      [cljsc2.clj.web/stepper]}))

  (s/close! (:incoming-step-observations-stream running-loop))
  )
