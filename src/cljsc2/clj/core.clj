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

(defn stop [connection sc-process]
  (try (quit connection)
       (catch Error e (do 'nothing))
       (catch Exception e (do 'nothing)))
  (try (sh/destroy sc-process)
       (catch Exception e (do 'nothing))))

(defn restart-conn
  ([] (restart-conn "127.0.0.1" 5000))
  ([host port] (restart-conn (str "ws://" host ":" port "/sc2api")))
  ([client-address]
   (p/retry {:strategy (p/constant-retry-strategy 2000 10)}
     (p/retriable
         {}
         (let [connection @(http/websocket-client client-address
                                                  {:max-frame-payload 998524288})]
           (s/on-closed connection (fn [] (println client-address "connection closed")))
           connection)))))

(defn max-version [path]
  (reduce max (map (fn [f] (Integer/parseInt (subs f 4)))
                   (.list (-> path clojure.java.io/file)))))

(defn start-client
  ([] (start-client (str "/Applications/StarCraft II/Versions/Base"
                         (max-version "/Applications/StarCraft II/Versions/")
                         "/SC2.app/Contents/MacOS/SC2")))
  ([path] (start-client path "127.0.0.1" 5000))
  ([path host port]
   (sh/proc path
    "-listen" host "-port" (str port) "-displayMode" "0" "-eglpath" "/usr/lib/nvidia-384/libEGL.so")))

(def Request
  (protodef SC2APIProtocol.Sc2Api$Request))

(defn get-available-maps
  ([] (get-available-maps "/Applications/StarCraft II/Maps") )
  ([path]
   (->> (file-seq (clojure.java.io/file path))
        (map #(.getPath %))
        (filter #(clojure.string/ends-with? % ".SC2Map"))
        )))

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
  ([connection]
   (loop [res (latest-response-message connection)]
     (if (not (identical? res nil))
       (recur (latest-response-message connection))
       :done))))

(defn send-request-and-get-response-message [connection request]
  (let [req (try @(send-request connection request)
                 (catch Exception e (println "exception sending request" e)))]
    (loop [res (latest-response-message connection)
           depth 0]
      (when (> depth 10000000) (println "depth > 1000000"))
      (if (> depth 10000000)
        (throw (Exception. (str req "tried request > 10000000 times"))))
      (if (identical? res nil)
        (recur (latest-response-message connection) (inc depth))
        res))))

(defn req [connection req-data]
  (send-request-and-get-response-message connection req-data))

(defn restart-game [connection]
  (send-request-and-get-response-message
   connection
   #:SC2APIProtocol.sc2api$RequestRestartGame{:restart-game {}}))

(defn send-action-and-get-response [connection action]
  (send-request-and-get-response-message
   connection
   #:SC2APIProtocol.sc2api$RequestAction
   {:action #:SC2APIProtocol.sc2api$RequestAction
    {:actions
     [action]}}))

(defn request-step
  ([connection]
   (send-request-and-get-response-message
    connection
    #:SC2APIProtocol.sc2api$RequestStep{:step {:count 22}})))

(defn load-mineral-game
  ([connection]
   (load-mineral-game
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
  ([connection] (load-simple-map connection "/Applications/StarCraft II/Maps/Melee/Simple64.SC2Map"))
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

(defn run-for [n]
  (fn [starting-obs]
    (let [starting-game-loop (get-in starting-obs [:game-loop])]
      (fn [obs _]
        (let [current-game-loop (get-in obs [:game-loop])
              current-run-steps (- current-game-loop starting-game-loop)]
          (>= current-run-steps
              n))))))

(defn request-observation [connection]
  (send-request-and-get-response-message
   connection
   #:SC2APIProtocol.sc2api$RequestObservation{:observation {}}))

(defn do-sc2
  ([connection step-fn]
   (do-sc2 connection step-fn {}))
  ([connection step-fn {:keys [collect-actions
                               collect-observations
                               use-datalog-observation
                               run-until-fn]
                        :or {collect-actions false
                             collect-observations false
                             run-until-fn (run-for 500)}}]
   (flush-incoming-responses connection)
   (let [loops (atom 0)
         observations-transient (transient [])
         actions-transient (transient [])
         initial-observation (request-observation connection)
         run-until-pred (run-until-fn (get-in initial-observation
                                              [:observation :observation]))]
     (loop [req-observation initial-observation
            observations observations-transient
            actions actions-transient]
       (let [actual-observation (get-in req-observation
                                        [:observation :observation])
             observation actual-observation
             ended? (or (identical? (:status req-observation) :ended)
                        (run-until-pred observation @loops))
             step-actions (step-fn observation connection)]
         (swap! loops inc)
         (if (and (not ended?)
                  step-actions)
           (do
             (send-request-and-get-response-message
              connection
              #:SC2APIProtocol.sc2api$RequestAction
              {:action #:SC2APIProtocol.sc2api$RequestAction
               {:actions
                step-actions}})
             (request-step connection)
             (let [after-obs (send-request-and-get-response-message
                              connection
                              #:SC2APIProtocol.sc2api$RequestObservation
                              {:observation {}})]
               (recur after-obs
                      (if collect-observations
                        (conj! observations observation)
                        observations)
                      (if collect-actions
                        (conj! actions step-actions)))))
           [observations actions]))))))
