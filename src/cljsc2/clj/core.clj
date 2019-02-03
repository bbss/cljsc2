(ns cljsc2.clj.core
  (:require
   [manifold.stream :as strm :refer [stream]]
   [manifold.deferred :as d]
   [aleph.http :as http]
   [clojure.core.async :as a]
   [perseverance.core :as p]
   [me.raynes.conch.low-level :as sh]
   [flatland.protobuf.core :refer [protodef protobuf-dump protodef? protobuf-load]]
   [cljsc2.clj.proto :refer [ugly-memo-make-protobuf]]
   ;[cljsc2.clj.rendering :refer [mp4-file-path->markdown-html run-result->mp4-file-path]]
   [taoensso.timbre :as timbre]))


(defn to-byte [it]
  (.toByteArray it))

(defn send-request [connection request]
  (strm/put! connection
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
  ([host port] (restart-conn host port 3))
  ([host port timeout]
   (let [address (str "ws://" host ":" port "/sc2api")]
     (p/retry {:strategy (p/constant-retry-strategy 2000 timeout)}
       (p/retriable
           {}
         (let [connection @(http/websocket-client address
                                                  {:max-frame-payload 998524288})]
           (strm/on-closed connection (fn [] (timbre/debug address "connection closed")))
           connection))))))

(defn max-version [path]
  (reduce max
          (map(fn [f] (Integer/parseInt (subs f 4)))
              (filter
               #(clojure.string/starts-with? % "Base")
               (.list (-> path clojure.java.io/file))))))

(defn default-maps-path []
  (case (System/getProperty "os.name")
    "Linux" "/home/bb/cljsc2/StarCraftII/Maps"
    "Mac OS X" "/Applications/StarCraft II/Maps"))

(defn default-sc-path []
  (case (System/getProperty "os.name")
    "Linux" (str "/home/bb/cljsc2/StarCraftII/Versions/Base"
                 (cljsc2.clj.core/max-version "/home/bb/cljsc2/StarCraftII/Versions")
                 "/SC2_x64")
    "Mac OS X" (str "/Applications/StarCraft II/Versions/Base"
                    (cljsc2.clj.core/max-version "/Applications/StarCraft II/Versions/")
                    "/SC2.app/Contents/MacOS/SC2")
    (throw (Throwable. "No path set for this OS"))))

(defn start-client
  ([] (start-client (default-sc-path)))
  ([path] (start-client path "127.0.0.1" 5000))
  ([path host port]
   (let [proc (sh/proc path
                       "-listen" host "-port" (str port) "-displayMode" "0" "-eglpath" "/usr/lib/nvidia-384/libEGL.so"
                       "-verbose")]
     (a/go (spit (str port ".log") (sh/stream-to-string proc :err) :append true))
     proc)))

(def Request
  (protodef SC2APIProtocol.Sc2Api$Request))

(defn get-available-maps
  ([] (get-available-maps "/Applications/StarCraft II/Maps"))
  ([path]
   (->> (file-seq (clojure.java.io/file path))
        (map #(.getPath %))
        (filter #(clojure.string/ends-with? % ".SC2Map")))))

(def Response
  (protodef SC2APIProtocol.Sc2Api$Response))

(defn latest-response [connection]
  (loop [buffer-size (-> connection
                         strm/description
                         :source
                         :buffer-size)]
    (if (= buffer-size 0)
      nil
      (if (> buffer-size 0)
        (strm/take! connection)
        (do (strm/take! connection)
            (recur (-> connection
                       strm/description
                       :source
                       :buffer-size)))))))

(defn response [connection]
  (if (>
       (-> connection
           strm/description
           :source
           :buffer-size)
       0)
    (strm/take! connection)))

(defn conn-closed? [connection]
  (-> connection
      strm/description
      :sink
      :closed?))

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

(defn get-port [conn]
  (let [conn-ip (:remote-address (:connection (.description (.sink conn))))]
    (Integer/parseInt (subs conn-ip (inc (clojure.string/last-index-of conn-ip ":"))))))

(defn send-request-and-get-response-message [connection request]
  (let [req (try @(send-request connection request)
                 (catch Exception e (do (timbre/debug "trying again after exception sending request"
                                                      request
                                                      " with error: "
                                                      e)
                                        (try @(send-request connection request)
                                             (catch Exception e (timbre/debug "failed a second time" request " with error: " e))))))]
    (loop [res (latest-response-message connection)
           depth 0
           sleep-time 0]
      (if (> depth 2000)
        (throw (Exception. (str request " tried request > 10000000 times on port: " (get-port connection)))))
      (if (= res nil)
        (do (Thread/sleep sleep-time)
            (recur (latest-response-message connection) (inc depth)
                   (if (and (> depth 10)
                            (< sleep-time 20))
                     (inc sleep-time)
                     sleep-time)))
        (do
          (when (not (= (flush-incoming-responses connection) :done)) (throw (Throwable. "more incoming, shouldn't happen")))
          res)))))

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

(defn send-actions-and-get-response [connection actions]
  (send-request-and-get-response-message
   connection
   #:SC2APIProtocol.sc2api$RequestAction
   {:action #:SC2APIProtocol.sc2api$RequestAction
    {:actions
     actions}}))

(defn request-step [connection stepsize]
  (send-request-and-get-response-message
   connection
   #:SC2APIProtocol.sc2api$RequestStep{:step {:count stepsize}}))

(defn quick-save [conn]
  (send-request-and-get-response-message
   conn #:SC2APIProtocol.sc2api$RequestQuickSave{:quick-save {}})
  (Thread/sleep 2000)
  (latest-response-message conn))

(defn quick-load [conn]
  (send-request-and-get-response-message
   conn #:SC2APIProtocol.sc2api$RequestQuickLoad{:quick-load {}}))
(defn save-replay [conn path]
  "Saves a replay into the path passed, overwriting if there is a replay there."
  (.writeTo (:data
              (:save-replay
                (send-request-and-get-response-message
                  conn
                  #:SC2APIProtocol.sc2api$RequestSaveReplay{:save-replay {}})))
            (clojure.java.io/make-output-stream
              (java.io.File. path)
              {:append false})))

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
         :minimap-resolution #:SC2APIProtocol.common$Size2DI{:x 64 :y 64}}}}}))))




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
         :minimap-resolution #:SC2APIProtocol.common$Size2DI{:x 64 :y 64}}

        :render #:SC2APIProtocol.sc2api$SpatialCameraSetup
        {:width 24
         :resolution #:SC2APIProtocol.common$Size2DI{:x 84 :y 84}
         :minimap-resolution #:SC2APIProtocol.common$Size2DI{:x 64 :y 64}}}}}))))



(defn load-map
  ([connection]
   (load-map connection {:map-config/path "/Applications/StarCraft II/Maps/Melee/Simple64.SC2Map"}))
  ([connection {:keys [map-config/path] :as config}]
   (load-map connection
             config
             #:SC2APIProtocol.sc2api$InterfaceOptions
             {:raw true
              :score true
              :feature-layer #:SC2APIProtocol.sc2api$SpatialCameraSetup
              {:width 24
               :resolution #:SC2APIProtocol.common$Size2DI{:x 84 :y 84}
               :minimap-resolution #:SC2APIProtocol.common$Size2DI{:x 64 :y 64}}
              :render #:SC2APIProtocol.sc2api$SpatialCameraSetup
              {:width 24
               :resolution #:SC2APIProtocol.common$Size2DI{:x 84 :y 84}
               :minimap-resolution #:SC2APIProtocol.common$Size2DI{:x 64 :y 64}}}))

  ([connection {:keys [map-config/path] :as config} player-setups interface-options]
   (send-request-and-get-response-message
    connection
    #:SC2APIProtocol.sc2api$RequestCreateGame
    {:create-game #:SC2APIProtocol.sc2api$RequestCreateGame
     {:map #:SC2APIProtocol.sc2api$LocalMap
      {:local-map
       {:map-path path}}
      :player-setup player-setups}})
   (send-request-and-get-response-message
    connection
    #:SC2APIProtocol.sc2api$RequestJoinGame
    {:join-game
     #:SC2APIProtocol.sc2api$RequestJoinGame
     {:participation
      #:SC2APIProtocol.sc2api$RequestJoinGame
      {:race (:SC2APIProtocol.sc2api$PlayerSetup/race
              (first
               (filter (comp #{"Participant"} :SC2APIProtocol.sc2api$PlayerSetup/type)
                       player-setups)))}
      :options interface-options}})
   (flush-incoming-responses connection)))

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
                               stepsize
                               run-until-fn
                               run-for-steps
                               to-markdown
                               additional-listening-fns
                               run-ended-cb
                               run-started-cb]
                        :or {collect-actions false
                             collect-observations false
                             run-until-fn (run-for 500)
                             run-for-steps 500
                             stepsize 1
                             to-markdown false
                             run-started-cb (fn [game-loop])
                             run-ended-cb (fn [game-loop run-game-loop])
                             additional-listening-fns []}}]
   (flush-incoming-responses connection)
   (let [run-result
         (let [loops (atom 0)
               observations-transient (transient [])
               actions-transient (transient [])
               initial-observation (request-observation connection)
               run-until-pred (run-until-fn (get-in initial-observation
                                                    [:observation :observation]))]
           (run-started-cb (:game-loop (:observation (:observation initial-observation))))
           (loop [req-observation initial-observation
                  observations observations-transient
                  actions actions-transient]
             (let [actual-observation (get-in req-observation
                                              [:observation :observation])
                   observation actual-observation
                   run-ended? (run-until-pred observation @loops)
                   step-actions (step-fn observation connection)]
               (swap! loops inc)
               (doall (map (fn [listener] (listener observation step-actions))
                           additional-listening-fns))
               (if (and (not (or run-ended?
                                 (identical? (:status req-observation) :ended)))
                        step-actions)
                 (do
                   (send-request-and-get-response-message
                            connection
                            #:SC2APIProtocol.sc2api$RequestAction
                            {:action #:SC2APIProtocol.sc2api$RequestAction
                             {:actions
                              step-actions}})
                   (request-step connection stepsize)
                   (let [after-obs (send-request-and-get-response-message
                                    connection
                                    #:SC2APIProtocol.sc2api$RequestObservation
                                    {:observation {}})]
                     (recur after-obs
                            (if collect-observations
                              (conj! observations observation)
                              observations)
                            (if collect-actions
                              (conj! actions step-actions)
                              actions))))
                 (do (run-ended-cb (:game-loop actual-observation ) @loops)
                     [observations actions {:game-ended? (identical? (:status req-observation) :ended)
                                            :run-ended? run-ended?
                                            :game-loop (:game-loop actual-observation)
                                            :ran-for-steps (* stepsize @loops)
                                            :run-for-steps run-for-steps}])))))]
     ;(if to-markdown
     ;  (mp4-file-path->markdown-html (run-result->mp4-file-path run-result (get-port connection)))
       run-result)))
