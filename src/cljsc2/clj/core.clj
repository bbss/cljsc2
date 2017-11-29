(ns cljsc2.clj.core
  (:require
   [manifold.stream :as s :refer [stream]]
   [manifold.deferred :as d]
   [aleph.http :as http]
   [perseverance.core :as p]
   [me.raynes.conch.low-level :as sh]
   [flatland.protobuf.core :refer [protodef protobuf-dump protodef? protobuf-load]]
   [cljsc2.clj.proto :refer [ugly-memo-make-protobuf]]
   ))

(declare sc-process)
(declare connection)

(defn to-byte [it]
  (.toByteArray it))

(defn send-request [connection request]
  (s/put! connection
          (to-byte
           (ugly-memo-make-protobuf
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
      (do (def connection
           @(http/websocket-client "ws://127.0.0.1:5000/sc2api"
                                   {:max-frame-payload 998524288}))
          (s/on-closed connection (fn [] (println "connection closed")))
          connection))))

(defn max-version [path]
  (reduce max (map (fn [f] (Integer/parseInt (subs f 4)))
                   (.list (-> path clojure.java.io/file)))))

(defn start-client
  ([] (start-client (str "/Applications/StarCraft II/Versions/Base"
                  (max-version "/Applications/StarCraft II/Versions/")
                  "/SC2.app/Contents/MacOS/SC2")))
  ([path]
   (println path)
   (stop)
   (def sc-process
     (sh/proc path
              "-listen" "127.0.0.1" "-port" "5000" "-displayMode" "0"))
   (restart-conn)))

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

(defn req
  ([req-data] (req connection req-data))
  ([connection req-data] (send-request-and-get-response-message connection req-data)))

(defn restart-game
  ([] (restart-game connection))
  ([connection]
   (send-request-and-get-response-message
    connection
    #:SC2APIProtocol.sc2api$RequestRestartGame{:restart-game {}})))

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
  ([] (load-mineral-game
       connection
       "/Applications/StarCraft II/Maps/mini_games/CollectMineralShards.SC2Map"))
  ([connection path]
   (d/chain
    (send-request
     connection
     #:SC2APIProtocol.sc2api$RequestCreateGame
     {:create-game #:SC2APIProtocol.sc2api$RequestCreateGame
      {:map #:SC2APIProtocol.sc2api$LocalMap
       {:local-map
        {:map-path path}}
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
        }}}))))


(defn load-simple-map
  ([] (load-simple-map connection "/Applications/StarCraft II/Maps/Melee/Simple64.SC2Map"))
  ([connection path]
   (d/chain
    (send-request
     connection
     #:SC2APIProtocol.sc2api$RequestCreateGame
     {:create-game #:SC2APIProtocol.sc2api$RequestCreateGame
      {:map #:SC2APIProtocol.sc2api$LocalMap
       {:local-map
        {:map-path path}}
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
        }}}))))

(comment :render  #:SC2APIProtocol.sc2api$SpatialCameraSetup
         {:width 24
          :resolution #:SC2APIProtocol.common$Size2DI{:x 84 :y 84}
          :minimap-resolution #:SC2APIProtocol.common$Size2DI{:x 64 :y 64}
          })

(defn request-observation
  ([] (request-observation connection))
  ([connection]
   (send-request-and-get-response-message
    connection
    #:SC2APIProtocol.sc2api$RequestObservation{:observation {}})))


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
      #:SC2APIProtocol.sc2api$RequestJoinGame{:race "Zerg" :options {}}
      :options #:SC2APIProtocol.sc2api$InterfaceOptions
      {:raw true
       :render  #:SC2APIProtocol.sc2api$SpatialCameraSetup
       {:width 24
        :resolution #:SC2APIProtocol.common$Size2DI{:x 84 :y 84}
        :minimap-resolution #:SC2APIProtocol.common$Size2DI{:x 64 :y 64}
        }
       :feature-layer #:SC2APIProtocol.sc2api$SpatialCameraSetup
       {:width 24
        :resolution #:SC2APIProtocol.common$Size2DI{:x 84 :y 84}
        :minimap-resolution #:SC2APIProtocol.common$Size2DI{:x 64 :y 64}
        }}}}))

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

  (send-request-and-get-response-message
   connection
   #:SC2APIProtocol.sc2api$RequestRestartGame{:restart-game {}}))


(defn after-each-step [connection incoming-step-observations]
  "Responsible for adding new observations to the incoming observation stream. Steps through the game and sends the actions from the agents step function to the game. Closes when the agent returns falsey for actions"
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
          (let [next-observation (send-request-and-get-response-message
                                  connection
                                  #:SC2APIProtocol.sc2api$RequestObservation
                                  {:observation {}})]
            (if (identical? (:status next-observation) :ended)
              (s/close! incoming-step-observations)
              (s/put!
               incoming-step-observations
               (get-in next-observation
                       [:observation :observation])))))
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
                (step-fn
                 step-observation
                 connection)
                ))))
         incoming-step-observations)
        (s/connect throttled-src stepper-results stepper))
      (s/connect-via
       incoming-step-observations
       (if observation-meta-fn
         (fn [step-observation]
           (try (s/put! stepper-results
                        (step-fn (with-meta
                                   step-observation
                                   (observation-meta-fn (System/currentTimeMillis)))
                                 connection))
                (catch Exception e (do
                                     (println e)
                                     (throw e)))))
         (fn [step-observation]
           (try (s/put! stepper-results (step-fn step-observation connection))
                (catch Exception e (do
                                     (println e)
                                     (throw e))))))
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
                               (get-in (send-request-and-get-response-message
                                       connection
                                       #:SC2APIProtocol.sc2api$RequestObservation
                                       {:observation {}})
                                       [:observation :observation])))
     :consume-step-actions (s/consume
                            (after-each-step connection incoming-step-observations)
                            step-action-stream)
     :step-action-stream step-action-stream
     :incoming-step-observations-stream incoming-step-observations
     :additional-stepper-streams additional-stepper-streams}))

(start-client)

(load-simple-map)

(comment
  (start-client)

  (restart-conn)

  (restart-game)

  (load-mineral-game)

  (def running-loop
    (do
      (when (:incoming-step-observations-stream running-loop)
        (s/close! (:incoming-step-observations-stream running-loop)))
      (run-loop
        connection
        {:step-fn cljsc2.clj.agent/step
         :description "steps -> agent"}
        {:additional-steppers [cljsc2.clj.web/stepper]}))))
