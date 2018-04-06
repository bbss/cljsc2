(ns cljsc2.clj.notebook.core
  (:require
   [perseverance.core :as p]
   [me.raynes.conch :refer [programs with-programs let-programs] :as hsh]
   [me.raynes.conch.low-level :as sh]
   [cljsc2.cljc.model :as model]
   cljsc2.clj.core
   [hawk.core :as hawk]
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
   [compojure.core     :as comp :refer (defroutes GET POST)]
   [compojure.route    :as route]
   [clojupyter.core :as cljp]
   [ring.middleware.defaults]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.gzip :refer [wrap-gzip]]
   [ring.middleware.not-modified :refer [wrap-not-modified]]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.util.response :as rsp :refer [file-response resource-response]]
   [ring.middleware.params :as params]
   [ring.middleware.keyword-params :as keyword-params]
   [fulcro.client.primitives :as prim]
   [fulcro.server :as server :refer [defquery-root defquery-entity]]
   [fulcro.websockets :as ws]
   [fulcro.websockets.protocols :refer [push]]
   [fulcro.server :refer [defmutation]]
   [clojure.core.async :as async :refer [go <! <!! >! >!! go-loop timeout]]
   [clojure.spec.alpha :as spec]
   )
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defonce notebook
  (let [rt (Runtime/getRuntime)
        proc (.exec rt (str "jupyter" " notebook"))]
    proc))

(declare ws)
(declare server-db)

(defn add-observation [server-db port run-id observation]
  (let [id (str port " - " run-id " - " (System/currentTimeMillis))
        ident-path [:observation/by-id id]
        observation (merge observation
                           {:db/id id})]
    (swap! server-db
           #(-> %
                (assoc-in ident-path observation)
                (prim/integrate-ident
                 ident-path :append [:run/by-id run-id :run/observations])))
    (doseq [id (:any @(:connected-uids ws))]
      (push ws id :add-observation {:run-id run-id :ident-path ident-path :observation observation}))))

(defn add-latest-response [server-db port response]
  (let [ident-path [:process/by-id port]]
    (swap! server-db assoc-in
           (conj ident-path :process/latest-response) response)
    (doseq [id (:any @(:connected-uids ws))]
      (push ws id :latest-response {:response response :ident-path ident-path}))))

(defn process-closing-chan [process]
  (let [process-closed (async/chan)]
    (go-loop [closed? (not (.isAlive (:process process)))]
      (<! (timeout 1000))
      (if closed?
        (do (async/put! process-closed closed?)
            (async/close! process-closed))
        (recur (not (.isAlive (:process process))))))
    process-closed))

(defn add-process [server-db ident-path new-process]
  (swap! server-db #(-> %
                        (assoc-in ident-path new-process)
                        (prim/integrate-ident ident-path :append [:root/processes])))
  (doseq [id (:any @(:connected-uids ws))]
    (push ws id :process-spawned (dissoc new-process :process/process))))

(defn remove-process [server-db port]
  (swap! server-db
         #(-> %
              (update :process/by-id (fn [processes]
                                       (dissoc processes port)))
              (update :root/processes (fn [processes]
                                        (vec (filter (comp not #{[:process/by-id port]}) processes))))))
  (doseq [id (:any @(:connected-uids ws))]
    (push ws id :process-died [:process/by-id port])))

(defn new-process [server-db port]
  (let [process (cljsc2.clj.core/start-client
                 (str "/Applications/StarCraft II/Versions/Base"
                      (cljsc2.clj.core/max-version "/Applications/StarCraft II/Versions/")
                      "/SC2.app/Contents/MacOS/SC2")
                 "127.0.0.1"
                 port)]
    (go (let [closed (<!! (process-closing-chan process))]
          (remove-process server-db port)))
    (do (let [ident-path [:process/by-id port]
              new-process {:process/process process
                           :process/port port
                           :db/id port
                           :process/runs []}]
          (add-process server-db ident-path new-process)))
    process))

(defn new-conn [server-db port]
  (let [conn (cljsc2.clj.core/restart-conn "127.0.0.1" port)]
    (swap! server-db assoc-in [:connection/by-id port]
           conn)
    conn))

(defn get-conn [server-db port]
  (let [conn (get-in @server-db [:connection/by-id port])
        conn-alive? (and conn (not (cljsc2.clj.core/conn-closed? conn)))]
    (if conn-alive?
      conn
      (let [process (get-in @server-db [:process/by-id port])]
        (if process
          (new-conn server-db port)
          (do (new-process server-db port)
              (new-conn server-db port)))))))

(defn ready-for-actions? [conn]
  (identical? (:status (cljsc2.clj.core/send-request-and-get-response-message
                        conn
                        #:SC2APIProtocol.sc2api$RequestGameInfo{:game-info {}}))
              :in-game))

(defn run-ended [server-db run-id port]
  (fn [game-loop]
    (let [ident-path [:run/by-id run-id]]
      (swap! server-db assoc-in (conj ident-path :run/ended-at) game-loop)
      (doseq [id (:any @(:connected-uids ws))]
        (push ws id :run-ended {:ident-path ident-path
                                :ended-at game-loop})))))

(defn add-game-info [server-db port]
  (let [ident-path [:process/by-id port]
        game-info (cljsc2.clj.core/send-request-and-get-response-message
                   (get-conn server-db 5000)
                   #:SC2APIProtocol.sc2api$RequestGameInfo{:game-info {}})]
    (swap! server-db assoc-in (conj ident-path :process/game-info) (:game-info game-info))
    (doseq [id (:any @(:connected-uids ws))]
      (push ws id :game-info-added {:ident-path ident-path
                                    :attr :process/game-info
                                    :value game-info}))))

(defn add-run [server-db port]
  (let [existing-ids (or (keys (:run/by-id @server-db)) [0])
        id (inc (apply max existing-ids))
        ident-path [:run/by-id id]
        new-run {:run/observations []
                 :db/id id}]
    (swap! server-db #(-> %
                          (assoc-in ident-path new-run)
                          (prim/integrate-ident ident-path
                                                :append
                                                [:process/by-id port :process/runs])))
    (doseq [id (:any @(:connected-uids ws))]
      (push ws id :run-added {:run new-run :process-id port}))
    new-run))

(defn msg->code [message _]
  (let [{:keys [cljsc code]} (:content message)
        {:keys [port run-for step-size] :or {port 5000
                                             step-size 1
                                             run-for 200}} cljsc]
    (if cljsc
      (str
       `(load-file "src/cljsc2/clj/notebook/kernel.clj")
       `(in-ns 'cljsc2.clj.kernel)
       `(use 'cljsc2.clj.core)
       `(let [~'process-conn (get-conn server-db ~port)
              ~'ready? (ready-for-actions? ~'process-conn)]
          (when-not ~'ready? (do (println "Loading map..")
                                 (cljsc2.clj.core/load-map ~'process-conn (:path (:root/map-config @server-db)))
                                 (Thread/sleep 10000)))
          (let [~'run (add-run server-db ~port)]
            (cljsc2.clj.core/do-sc2
             ~'process-conn
             (eval (read-string ~code))
             {:stepsize ~step-size
              :run-until-fn (cljsc2.clj.core/run-for ~run-for)
              :additional-listening-fns [(fn [~'observation ~'connection]
                                           (add-observation server-db ~port (:db/id ~'run) ~'observation))]
              :run-ended-cb (run-ended server-db (:db/id ~'run) ~port)
              :game-ended-cb (fn [~'game-loop])}))
          (when-not ~'ready? (add-game-info server-db ~port))))
      code)))

#_(doseq [[k v] @kernels]
    (reset! (second v) true))

(def kernels (atom {}))

(def dir-watcher
  (hawk/watch! [{:paths ["/Users/baruchberger/Library/Jupyter/runtime/"]
                 :handler (fn [_ {:keys [file kind]}]
                            (let [path (.getPath file)
                                  name (.getName file)]
                              (timbre/debug name kind "observed in directory")
                              (when (and (clojure.string/starts-with? name "kernel")
                                         (clojure.string/ends-with? name ".json")
                                         (identical? kind :create))
                                (swap! kernels assoc path (with-bindings
                                                            {#'clojupyter.misc.messages/message->code (var msg->code)}
                                                            (cljp/start-latest-kernel! path)))
                                (Thread/sleep 5000)
                                (clojure.java.io/delete-file path)))
                            )}]))

(reset! sente/debug-mode?_ true)

(defn not-found-handler []
  (fn [req]
    {:status  404
     :headers {"Content-Type" "text/plain"}
     :body    "NOPE"}))

(defonce web-server_ (atom nil))
(defn stop-web-server! [] (when-let [stop-fn @web-server_] (stop-fn)))

(defn start-web-server! [& [port]]
  (stop-web-server!)
  (let [port (or port 0)              ; 0 => Choose any available port
        ws (.start (ws/make-websockets
                    (server/fulcro-parser)
                    {:http-server-adapter (get-sch-adapter)
                     :transit-handlers {:write
                                        {com.google.protobuf.ByteString$LiteralByteString
                                         (transit/write-handler
                                          "literal-byte-string"
                                          (fn [bs] (let [buff (ByteArrayOutputStream. 4096)
                                                         _ (.writeTo bs buff)]
                                                     (.toByteArray buff))))}}}))
        wrap-root (fn [handler] (fn [req] (handler (update req :uri #(if (= "/" %) "/index.html" %)))))
        ring-handler (-> (not-found-handler)
                         (ws/wrap-api ws)
                         (wrap-root)
                         (keyword-params/wrap-keyword-params)
                         (params/wrap-params)
                         (server/wrap-transit-params)
                         (server/wrap-transit-response)
                         (wrap-resource "public")
                         #_(wrap-not-modified)
                         #_(wrap-gzip))
        [port stop-fn]
        (let [server (aleph/start-server ring-handler {:port port})
              p (promise)]
          (future @p)      ; Workaround for Ref. https://goo.gl/kLvced
          ;; (aleph.netty/wait-for-close server)
          [(aleph.netty/port server)
           (fn [] (.close ^java.io.Closeable server) (deliver p nil))])
        uri (format "http://0.0.0.0:%s/" port)]
    (def ws ws)

    (println "Web server is running at `%s`" uri)

    (reset! web-server_ stop-fn)))

(start-web-server! 3446)

(def server-db
  (atom {:root/processes []
         :process/by-id {}
         :connection/by-id {}
         :run/by-id {}
         :root/process-starter {:available-maps
                                (spec/conform
                                 ::model/available-maps
                                 (->> (file-seq (clojure.java.io/file "/Applications/StarCraft II/Maps"))
                                      (filter (fn [f] (clojure.string/ends-with? (.getName f) ".SC2Map")))
                                      (map (fn [f] [(.getAbsolutePath f) (.getName f)]))
                                      vec))}
         :root/map-config {:path "/Applications/StarCraft II/Maps/Melee/Simple64.SC2Map"}}))

#_(add-process server-db
               [:process/by-id 5000]
               {:process/process {}
                :process/port 5000
                :db/id 5000
                :process/runs []})

(defquery-root ::model/processes
  (value [env params]
         (:root/processes
          (prim/db->tree [{:root/processes (:query env)}]
                         @server-db
                         @server-db))))

(defquery-root ::model/process-starter
  (value [env params]
         (:root/process-starter
          (prim/db->tree [{:root/process-starter (:query env)}]
                         @server-db
                         @server-db))))

(defmutation cljsc2.cljs.content-script.core/change-default-map
  [{:keys [map]}]
  (action [what]
          (swap! server-db assoc-in [:root/map-config :path] map)
          {}))

(defmutation cljsc2.cljs.content-script.core/send-request-to-process
  [{:keys [port request]}]
  (action [env]
          (let [conn (get-conn server-db port)
                sc2-response (cljsc2.clj.core/send-request-and-get-response-message
                              conn
                              request)
                run-id (last (last (get-in @server-db [:process/by-id port :process/runs])))]
            (swap! server-db assoc-in
                   [:process/by-id port :latest-sc2-response] sc2-response)
            (add-latest-response server-db port sc2-response)
           (cljsc2.clj.core/request-step conn 1)
            (add-observation server-db port run-id
                             (:observation (:observation (cljsc2.clj.core/request-observation conn))))
            {})))

(defmutation cljsc2.cljs.content-script.core/send-action
  [{:keys [action port]}]
  (action [env]
          (let [_ (println port action)
                conn (get-conn server-db port)
                run-id (last (last (get-in @server-db [:process/by-id port :process/runs])))]
            (when (not (empty? action))
              (do (cljsc2.clj.core/send-action-and-get-response
                   conn
                   action)
                  (cljsc2.clj.core/request-step conn 2)
                  (add-observation server-db port run-id
                                   (:observation (:observation (cljsc2.clj.core/request-observation conn)))))))))
