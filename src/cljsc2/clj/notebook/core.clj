(ns cljsc2.clj.notebook.core
  (:require
   [perseverance.core :as p]
   [me.raynes.conch :refer [programs with-programs let-programs] :as hsh]
   [me.raynes.conch.low-level :as sh]
   [cljsc2.cljc.model :as model]
   cljsc2.clj.core
   cljsc2.clj.game-parsing
   [datascript.transit :as dst]
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
   #_[ring.middleware.gzip :refer [wrap-gzip]]
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

(timbre/merge-config! {:ns-blacklist ["clojupyter.misc.messages"]} )

#_(defonce notebook
  (let [rt (Runtime/getRuntime)
        proc (.exec rt (str "jupyter" " notebook"))]
    proc))

(declare ws)
(declare server-db)

(defn uuid [] (java.util.UUID/randomUUID))

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

(defn add-run-config [server-db ident-path new-config process-ident-path]
  (swap! server-db assoc-in ident-path new-config)
  (doseq [id (:any @(:connected-uids ws))]
    (push ws id :run-config-added {:run-config new-config
                                   :process-ident process-ident-path})))

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

(def run-config-id (atom 0))

(defn next-run-config-id []
  (swap! run-config-id inc))

(defn new-process [server-db port]
  (let [process (cljsc2.clj.core/start-client
                 (cljsc2.clj.core/default-sc-path)
                 "127.0.0.1"
                 port)]
    (go (let [closed (<!! (process-closing-chan process))]
          (timbre/debug "process closed" port)
          (remove-process server-db port)))
    (do (let [process-ident-path [:process/by-id port]
              config-id (next-run-config-id)
              config-ident-path [:run-config/by-id config-id]
              new-process {:process/process process
                           :process/port port
                           :db/id port
                           :process/run-config config-ident-path
                           :process/runs []}
              new-run-config {:run-config/run-size 160
                              :run-config/step-size 16
                              :run-config/restart-on-episode-end true
                              :db/id config-id}]
          (add-process server-db process-ident-path new-process)
          (add-run-config server-db config-ident-path new-run-config process-ident-path)))
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
  (fn [game-loop run-loop]
    (let [ident-path [:run/by-id run-id]]
      (swap! server-db assoc-in (conj ident-path :run/ended-at)
             game-loop)
      (swap! server-db (fn [s]
                         (let [observation-ident-paths (-> (get-in s (conj ident-path :run/observations))
                                                           reverse
                                                           rest)]
                           (-> (reduce (fn [s [path key]]
                                         (update s path (fn [observations]
                                                          (dissoc observations key))))
                                       s observation-ident-paths)
                               (update-in (conj ident-path :run/observations)
                                          (comp vector last))))))
      (doseq [id (:any @(:connected-uids ws))]
        (push ws id :run-ended {:ident-path ident-path
                                :ended-at game-loop})))))

(defn run-started [server-db run-id port]
  (fn [game-loop]
    (let [ident-path [:run/by-id run-id]]
      (swap! server-db assoc-in (conj ident-path :run/started-at)
             game-loop)
      (doseq [id (:any @(:connected-uids ws))]
        (push ws id :run-started {:ident-path ident-path
                                  :started-at game-loop})))))

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

(defn update-process-run-config [server-db process-ident-path run-config-path]
  (swap! server-db assoc-in (conj process-ident-path :process/run-config) run-config-path))

(defn add-run [server-db port]
  (let [existing-ids (or (keys (:run/by-id @server-db)) [0])
        id (inc (apply max existing-ids))
        ident-path [:run/by-id id]
        new-run-config-id (next-run-config-id)
        latest-run-config-id (apply max (or (keys (:run-config/by-id @server-db)) [0]))
        new-run {:run/observations []
                 :run/run-config [:run-config/by-id latest-run-config-id]
                 :db/id id}
        new-run-config (merge (get-in @server-db [:run-config/by-id latest-run-config-id])
                              {:db/id new-run-config-id})]
    (swap! server-db #(-> %
                          (assoc-in ident-path new-run)
                          (prim/integrate-ident ident-path
                                                :append
                                                [:process/by-id port :process/runs])))
    (add-run-config server-db
                    [:run-config/by-id new-run-config-id]
                    new-run-config
                    [:process/by-id port])
    (update-process-run-config server-db [:process/by-id port] [:run-config/by-id new-run-config-id])
    (doseq [id (:any @(:connected-uids ws))]
      (push ws id :run-added
            {:run (merge new-run {:run/run-config new-run-config})
             :process-id port}))
    new-run))

(defn status [conn]
  (:status (cljsc2.clj.core/request-observation conn)))

(defn load-new-game [conn run-map-config]
  (do (println "Loading map..")
      (case (status conn)
        :ended (cljsc2.clj.core/restart-game conn)
        (do (cljsc2.clj.core/load-map conn run-map-config)
            (Thread/sleep 10000)
            (cljsc2.clj.core/flush-incoming-responses conn)))))

(declare server-db)

(defn get-selected-connection [server-db]
  (get-conn server-db 5000))

(declare port)

(declare connection)

(defn make-ready [server-db connection]
  (if-not (ready-for-actions? connection)
    (do (load-new-game connection (val (first (:map-config/by-id @server-db))))
        (add-game-info server-db 5000)
        (cljsc2.clj.core/quick-save connection)
        (swap! server-db
               (fn [s]
                 (-> s
                     (assoc-in [:process/by-id 5000 :process/savepoint-at]
                               1))))
        (doseq [id (:any @(:connected-uids ws))]
          (push ws id :savepoint-added {:ident-path [:process/by-id 5000]
                                        :savepoint-at 1}))
        connection)
    connection))

(defn is-action? [obj]
  (and
   (map? obj)
   (not (empty? (select-keys obj
                             [:SC2APIProtocol.sc2api$Action/action-raw
                              :SC2APIProtocol.sc2api$Action/action-feature-layer
                              :SC2APIProtocol.sc2api$Action/action-render
                              :SC2APIProtocol.sc2api$Action/action-ui
                              :SC2APIProtocol.sc2api$Action/action-chat])))))

(defn run-sc [agent-fn run-end-fn {:keys [init-fn run-for-steps ran-for-steps game-loop
                                          run-ended? game-ended?]}]
  (try
    (let [run (add-run server-db
                       port)
          {:keys [run-config/step-size
                  run-config/restart-on-episode-end
                  run-config/run-size] :as run-config} (get-in @server-db (:run/run-config run))]
      (let [run-result
            (cljsc2.clj.core/do-sc2
             connection
             agent-fn
             {:stepsize step-size
              :run-until-fn (cljsc2.clj.core/run-for (or run-for-steps run-size))
              :run-for-steps run-for-steps
              :additional-listening-fns [(fn [observation connection]
                                           (add-observation server-db port (:db/id run) observation))]
              :run-started-cb (run-started server-db (:db/id run) port)
              :run-ended-cb (run-ended server-db (:db/id run) port)})
            {:keys [run-for-steps ran-for-steps game-loop run-ended? game-ended?] :as run-info} (nth run-result 2)]
        (timbre/debug run-for-steps ran-for-steps game-ended? run-ended? restart-on-episode-end)
        (if restart-on-episode-end
          (if (and game-ended? (not run-ended?))
            (concat [run-result] (do (make-ready server-db connection)
                                     (run-sc agent-fn run-end-fn
                                             (assoc run-info :run-for-steps
                                                    (- (or run-for-steps run-size) ran-for-steps)))))
            [run-result])
          [run-result])))
    (catch Exception e (timbre/error e))))

(defn msg->code [message _]
  (try
    (let [{:keys [code]} (:content message)
            parsed-code (read-string code)]
        (str
         `(println "Running..")
         `(load-file "src/cljsc2/clj/notebook/kernel.clj")
         `(in-ns 'cljsc2.clj.kernel)
         `(use 'cljsc2.clj.core)
         `(use 'cljsc2.clj.build-order)
         "(with-redefs [cljsc2.clj.notebook.core/port 5000
                    cljsc2.clj.notebook.core/connection (cljsc2.clj.notebook.core/make-ready
                                  cljsc2.clj.notebook.core/server-db
                                  (cljsc2.clj.notebook.core/get-selected-connection
                                    cljsc2.clj.notebook.core/server-db))
                    connection (cljsc2.clj.notebook.core/make-ready
                                  cljsc2.clj.notebook.core/server-db
                                  (cljsc2.clj.notebook.core/get-selected-connection
                                    cljsc2.clj.notebook.core/server-db))]"
         (cond
           (is-action? parsed-code) (if (spec/valid? :SC2APIProtocol.sc2api/Action
                                                     parsed-code)
                                      (str "(cljsc2.clj.core/send-action-and-get-response connection " code ")")
                                      (str "(expound/expound :SC2APIProtocol.sc2api/Action "
                                           parsed-code
                                           ")"))
           (and (not (empty? parsed-code))
                (every? is-action? parsed-code)) (if (every? (partial spec/valid? :SC2APIProtocol.sc2api/Action)
                                                             parsed-code)
                                                   (str "(cljsc2.clj.core/send-action-and-get-response connection " code ")")
                                                   (str "(expound/expound (spec/coll-of :SC2APIProtocol.sc2api/Action) "
                                                        parsed-code
                                                        ")"))
           (= (first parsed-code) 'fn) (run-sc parsed-code)
           :else code)
         ")"))
    (catch Exception e (do (timbre/debug e)
                           (.toString e)))))

(def kernels (atom {}))

(def dir-watcher
  (do
    (Thread/sleep 10000)
    (hawk/watch! [{:paths [(str (System/getenv "XDG_RUNTIME_DIR") "/jupyter")]
                   :handler (fn [_ {:keys [file kind]}]
                              (let [path (.getPath file)
                                    name (.getName file)]
                                (timbre/debug name kind "observed in directory")
                                (when (and (clojure.string/starts-with? name "kernel")
                                           (clojure.string/ends-with? name ".json")
                                           (or (identical? kind :create)
                                               (identical? kind :modify)))
                                  (swap! kernels assoc path (with-bindings
                                                              {#'clojupyter.misc.messages/message->code (var msg->code)}
                                                              (cljp/start-latest-kernel! path)))
                                  #_(Thread/sleep 5000)
                                  #_(clojure.java.io/delete-file path))))}])))

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
                                        (merge {com.google.protobuf.ByteString$LiteralByteString
                                               (transit/write-handler
                                                "literal-byte-string"
                                                (fn [bs] (let [buff (ByteArrayOutputStream. 4096)
                                                               _ (.writeTo bs buff)]
                                                           (.toByteArray buff))))}
                                               dst/write-handlers)}}))
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
                         #_(wrap-gzip)
                         (ring.middleware.defaults/wrap-defaults ring.middleware.defaults/site-defaults))
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
  (let [map-config {:map-config/path (str (cljsc2.clj.core/default-maps-path) "/DefeatBanelings.SC2Map")
                    :db/id (uuid)}]
    (atom {:root/processes []
           :process/by-id {}
           :connection/by-id {}
           :run/by-id {}
           :run-config/by-id {}
           :map-config/by-id {(:db/id map-config) map-config}
           :root/process-starter {:process-starter/available-maps
                                  (spec/conform
                                   ::model/available-maps
                                   (->> (file-seq (clojure.java.io/file (str (cljsc2.clj.core/default-maps-path))))
                                        (filter (fn [f] (clojure.string/ends-with? (.getName f) ".SC2Map")))
                                        (filter (fn [f] (not (clojure.string/starts-with? (.getName f) "."))))
                                        (map (fn [f] [(.getAbsolutePath f) (.getName f)]))
                                        vec))
                                  :process-starter/map-config [:map-config/by-id (:db/id map-config)]}})))

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

(defquery-root :root/starcraft-static-data
  (value [env params]
         cljsc2.clj.game-parsing/knowledge-base))

(defquery-entity :run-config/by-id
  (value [env id params]
         (prim/db->tree (:query env)
                        (get-in @server-db [:run-config/by-id id])
                        (get-in @server-db [:run-config/by-id id]))))

(defmutation cljsc2.cljs.contentscript.core/send-request
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

(defmutation cljsc2.cljs.contentscript.core/make-savepoint
  [{:keys [game-loop port]}]
  (action [env]
          (let [_ (timbre/debug port game-loop)
                conn (get-conn server-db port)]
            (cljsc2.clj.core/quick-save conn)
            (swap! server-db
                   (fn [s]
                     (-> s
                         (assoc-in [:process/by-id port :process/savepoint-at]
                                   game-loop))))
            {})))

(defmutation cljsc2.cljs.contentscript.core/load-savepoint
  [{:keys [port]}]
  (action [env]
          (let [conn (get-conn server-db port)
                run-id (last (last (get-in @server-db [:process/by-id port :process/runs])))]
            (cljsc2.clj.core/quick-load conn)
            (cljsc2.clj.core/request-step conn 1)
            (add-observation server-db port run-id
                             (:observation (:observation (cljsc2.clj.core/request-observation conn))))
            {})))

(defmutation cljsc2.cljs.contentscript.core/send-action
  [{:keys [action port]}]
  (action [env]
          (let [_ (timbre/debug port action)
                conn (get-conn server-db port)
                run-id (last (last (get-in @server-db [:process/by-id port :process/runs])))]
            (when (not (empty? action))
              (do (cljsc2.clj.core/send-action-and-get-response
                   conn
                   action)
                  (cljsc2.clj.core/request-step conn 2)
                  (add-observation server-db port run-id
                                   (:observation (:observation (cljsc2.clj.core/request-observation conn)))))))))

(defmutation cljsc2.cljs.contentscript.core/update-map [{:keys [id path]}]
  (action [env]
          (do (swap! server-db assoc-in [:map-config/by-id id :map-config/path] path)
              {})))

(defmutation cljsc2.cljs.contentscript.core/submit-run-config [params]
  (action [env]
          (do
            (swap! server-db
                   (fn [s]
                     (reduce (fn [acc [ident-path diff]]
                               (timbre/debug ident-path diff)
                               (reduce (fn [acc [key {:keys [after] :as it}]]
                                         (assoc-in acc (conj ident-path key) after))
                                       acc
                                       diff))
                             s
                             (:diff params))))
            {})))
