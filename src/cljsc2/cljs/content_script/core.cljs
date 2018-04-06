(ns cljsc2.cljs.content-script.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [react :as react]
            [clojure.core.async :refer [<! >!]]
            [cljsc2.cljs.core :refer [render-canvas feature-layer-draw-descriptions]]
            [cljsc2.cljc.model :as model]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols :refer [post-message!]]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]
            [oops.core :refer [oset! oget]]
            [goog.dom :as gdom]
            [taoensso.sente  :as sente :refer (cb-success?)]
            [taoensso.sente.packers.transit :as sente-transit]
            [fulcro.client :as fc]
            [fulcro.client.dom :as dom]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.client.data-fetch :refer [load]]
            [fulcro.websockets :as fw]
            [fulcro.client.mutations :as m :refer [defmutation]]
            [fulcro.websockets.networking :refer [push-received]]))

(enable-console-print!)

(defonce app (atom (fc/new-fulcro-client)))

(def uint8->binary js/uint8toBinaryString)

(def binary->ab32 js/str2ab32)

(defn map->nsmap
  [m n]
  (reduce-kv (fn [acc k v]
               (let [new-kw (if (and (keyword? k)
                                     (not (qualified-keyword? k)))
                              (keyword (str n) (name k))
                              k) ]
                 (assoc acc new-kw v)))
             {} m))

(def pri js/console.log)

(defn render-feature-layers [canvas last-obs state layer-path]
  (if-let [feature-layer (get-in last-obs layer-path)]
    (let [ctx (.getContext canvas "2d")
          is-rgb (identical? (last layer-path) :map)
          data (if is-rgb
                 feature-layer
                 (let [{:keys [data bits-per-pixel] :as fl} feature-layer]
                           (assoc fl :data (case bits-per-pixel
                                             1 (.split (js/uint8toBinaryString data) "")
                                             8 data
                                             32 (binary->ab32 (uint8->binary data))
                                             []))))
          scale (get feature-layer-draw-descriptions (last layer-path)
                     (:unit-type feature-layer-draw-descriptions))
          to-resolution [(* 4 (:x (:size feature-layer))) (* 4 (:y (:size feature-layer)))]]
      (render-canvas
       canvas
       (last layer-path)
       data
       scale
       to-resolution
       is-rgb))))

(defn render-selection [canvas state [image-p render-cb] render-selection]
  (let [{:keys [start end]} (:selection state)
        ctx (.getContext canvas "2d")]
    (when image-p
      (.then image-p
             (fn [img-data]
               (render-cb img-data)
               (when (and render-selection (and start end))
                 (let [[sx sy] start
                       [ex ey] end]
                   (set! (.-strokeStyle ctx) "#10ff00")
                   (.strokeRect ctx sx sy (- ex sx) (- ey sy))
                   (set! (.-fillStyle ctx) "rgba(0, 255, 31, 0.32")
                   (.fillRect ctx sx sy (- ex sx) (- ey sy)))))))))

(defsc Observation [this {:keys [:game-loop :player-common :db/id] :as observation}]
  {:query [{:feature-layer-data [{:minimap-renders [:selected]}
                                 {:renders [:unit-type]}]}
           #_{:render-data [:map :minimap]}
           :game-loop
           :db/id
           {:player-common [:minerals :vespene :food-used :food-cap]}
           {:raw-data [{:player [:camera]} :units]}
           {:abilities [:ability-id :requires-point]}]
   :ident [:observation/by-id :db/id]})

(def ui-observation (prim/factory Observation {:keyfn :game-loop}))

(defsc Run [this {:keys [db/id :run/observations]}]
  {:query [{:run/observations (prim/get-query Observation)}
             :db/id]
   :ident [:run/by-id :db/id]})

(defn event->dom-coords
  "Translate a javascript evt to a clj [x y] within the given dom element."
  [evt dom-ele]
  (let [cx (.-clientX evt)
        cy (.-clientY evt)
        BB (.getBoundingClientRect dom-ele)
        x  (- cx (.-left BB))
        y  (- cy (.-top BB))]
    [x y]))

(defn evt-sc-coord [{:keys [x y] :as camera} element evt [click-x click-y]]
  (let [left-map (- x 11.5)
        right-map (+ x 11.5)
        top-map (- y 10)
        bottom-map (+ y 13)]
    {:x (+ left-map
           (* (/ click-x 336)
              (- right-map left-map)))
     :y (+ top-map
           (* (/ click-y 336)
              (- bottom-map top-map)))}))

(defn latest-observation-from-runs [runs]
  (-> runs
      last last last last))

(defn render-screen [this path]
  (let [image-promise (render-feature-layers
                       (dom/node this "process-feed")
                       (latest-observation-from-runs (:process/runs (prim/props this)))
                       (prim/get-state this)
                       path)]
    (render-selection
     (dom/node this "process-feed")
     (prim/get-state this)
     image-promise true)))

(defn render-minimap [this path]
  (let [image-promise (render-feature-layers
                       (dom/node this "process-feed-minimap")
                       (latest-observation-from-runs (:process/runs (prim/props this)))
                       (prim/get-state this)
                       path)]
    (render-selection
     (dom/node this "process-feed")
     (prim/get-state this)
     image-promise
     false)))

(defmutation send-request-to-process [_]
  (remote [env] true))

(defmutation send-action [_]
  (remote [env] true))

(declare ui-process)

(def feature-layer-minimap-paths
  {:camera [:feature-layer-data :minimap-renders :camera]
   :unit-type [:feature-layer-data :minimap-renders :unit-type]
   :selected [:feature-layer-data :minimap-renders :selected]
   :creep [:feature-layer-data :minimap-renders :creep]
   :player-relative [:feature-layer-data :minimap-renders :player-relative]
   :player-id [:feature-layer-data :minimap-renders :player-id]
   :visibility-map [:feature-layer-data :minimap-renders :visibility-map]
   :minimap [:renders :minimap]})

(def feature-layer-render-paths
  {:unit-hit-points [:feature-layer-data :renders :unit-hit-points]
   :unit-energy-ratio [:feature-layer-data :renders :unit-energy-ratio]
   :unit-shields-ratio [:feature-layer-data :renders :unit-shield-ratio]
   :unit-density [:feature-layer-data :renders :unit-density]
   :unit-energy [:feature-layer-data :renders :unit-energy]
   :unit-type [:feature-layer-data :renders :unit-type]
   :height-map [:feature-layer-data :renders :height-map]
   :unit-shields [:feature-layer-data :renders :unit-shields]
   :unit-density-aa [:feature-layer-data :renders :unit-density-aa]
   :selected [:feature-layer-data :renders :selected]
   :creep [:feature-layer-data :renders :creep]
   :effects [:feature-layer-data :renders :effects]
   :power [:feature-layer-data :renders :power]
   :player-relative [:feature-layer-data :renders :player-relative]
   :player-id [:feature-layer-data :renders :player-id]
   :visibility-map [:feature-layer-data :renders :visibility-map]
   :map [:render-data :map]})

(defn select-minimap-layer [this port x y]
  (fn [evt]
    (let [path (cljs.reader/read-string (.. evt -target -value))
          state (prim/app-state (prim/get-reconciler this))]
      (prim/set-state!
       this
       (assoc (prim/get-state this)
              :selected-minimap-layer-path
              path))
      (prim/set-query!
       this
       ui-process
       {:query (assoc-in (prim/get-query this @state)
                         [0 :process/runs 0 :run/observations 0 :feature-layer-data 0 :minimap-renders]
                         [(last path)])})
      (prim/transact!
       this
       `[(send-action ~{:port port :x x :y y})]))))

(defn select-render-layer [this port x y]
  (fn [evt]
    (let [path (cljs.reader/read-string (.. evt -target -value))
          state (prim/app-state (prim/get-reconciler this))]
      (prim/set-state!
       this
       (assoc (prim/get-state this)
              :selected-render-layer-path
              path))
      (when (not (or (identical? (last path) :minimap)
                     (identical? (last path) :map)))
        (prim/set-query!
         this
         ui-process
         {:query (assoc-in (prim/get-query this @state)
                           [0 :process/runs 0 :run/observations 0 :feature-layer-data 1 :renders] [(last path)])}))
      (prim/transact!
       this
       `[(send-action ~{:port port :x x :y y})]))))

(defsc Process [this {:keys [db/id process/port
                             process/runs
                             process/latest-response
                             process/game-info]}]
  {:query [{:process/runs (prim/get-query Run)}
           :process/port
           :process/latest-response
           :process/game-info
           :db/id]
   :initLocalState (fn [] {:selection nil
                           :selected-minimap-layer-path [:feature-layer-data :minimap-renders :selected]
                           :selected-render-layer-path [:feature-layer-data :renders :unit-type]})
   :componentDidUpdate (fn [_ _]
                         (render-screen this (:selected-render-layer-path (prim/get-state this)))
                         (render-minimap this
                                         (:selected-minimap-layer-path
                                          (prim/get-state this))))
   :componentDidMount (fn []
                        (render-screen this (:selected-render-layer-path (prim/get-state this)))
                        (render-minimap this
                                        (:selected-minimap-layer-path
                                         (prim/get-state this))))
   :ident [:process/by-id :db/id]}
  (let [latest-observation (latest-observation-from-runs runs)
        {:keys [x y] :as camera} (get-in latest-observation [:raw-data :player :camera])
        {:keys [food-used food-cap vespene minerals]} (:player-common latest-observation)]
    (dom/div #js {:key id}
             (dom/p nil "process on port " port)
             (dom/p nil "has evaluated  " (count runs) " runs")
             (dom/p nil "current camera position "
                    (str camera))
             (dom/p nil "Supply: " food-used "/" food-cap)
             (dom/p nil "Minerals: " minerals " Gas: " vespene)
             (dom/select
              #js {:key "mm-render"
                   :value (:selected-minimap-layer-path (prim/get-state this))
                   :onChange (select-minimap-layer this port x y)}
              (for [[layer-name layer-path] feature-layer-minimap-paths]
                (dom/option #js {:key layer-name
                                 :value layer-path} (str layer-name))))
             (dom/select
              #js {:key "render"
                   :value (:selected-render-layer-path (prim/get-state this))
                   :onChange (select-render-layer this port x y)}
              (for [[layer-name layer-path] feature-layer-render-paths]
                (dom/option #js {:key layer-name
                                 :value layer-path} (str layer-name))))
             (dom/button #js {:onClick (fn [_]
                                         (prim/transact!
                                          this
                                          `[(send-action
                                             ~{:port port
                                               :action
                                               #:SC2APIProtocol.sc2api$Action
                                               {:action-raw #:SC2APIProtocol.raw$ActionRaw
                                                {:action #:SC2APIProtocol.raw$ActionRaw
                                                 {:camera-move #:SC2APIProtocol.raw$ActionRawCameraMove
                                                  {:center-world-space #:SC2APIProtocol.common$Point{:x (- x 3) :y y}}}}}})]))}
                         "left")
             (dom/button #js {:onClick (fn [_]
                                         (prim/transact!
                                          this
                                          `[(send-action
                                             ~{:port port
                                               :action
                                               #:SC2APIProtocol.sc2api$Action
                                               {:action-raw #:SC2APIProtocol.raw$ActionRaw
                                                {:action #:SC2APIProtocol.raw$ActionRaw
                                                 {:camera-move #:SC2APIProtocol.raw$ActionRawCameraMove
                                                  {:center-world-space #:SC2APIProtocol.common$Point{:x x :y  (- y 3)}}}}}})]))}
                         "down")
             (dom/button #js {:onClick (fn [_]
                                         (prim/transact!
                                          this
                                          `[(send-action
                                             ~{:port port
                                               :action
                                               #:SC2APIProtocol.sc2api$Action
                                               {:action-raw #:SC2APIProtocol.raw$ActionRaw
                                                {:action #:SC2APIProtocol.raw$ActionRaw
                                                 {:camera-move #:SC2APIProtocol.raw$ActionRawCameraMove
                                                  {:center-world-space #:SC2APIProtocol.common$Point{:x x
                                                                                                     :y (+ y 3)}}}}}})]))}
                         "up")
             (dom/button #js {:onClick (fn [_]
                                         (prim/transact!
                                          this
                                          `[(send-action
                                             ~{:port port
                                               :action
                                               #:SC2APIProtocol.sc2api$Action
                                               {:action-raw #:SC2APIProtocol.raw$ActionRaw
                                                {:action #:SC2APIProtocol.raw$ActionRaw
                                                 {:camera-move #:SC2APIProtocol.raw$ActionRawCameraMove
                                                  {:center-world-space #:SC2APIProtocol.common$Point{:x (+ x 3) :y y}}}}}})]))}
                         "right")
             (dom/div
              nil
              (dom/canvas
               #js {:ref "process-feed-minimap"
                    :width 256
                    :height 256})
              (dom/canvas
               #js {:ref "process-feed"
                    :width 336
                    :height 336
                    :onMouseDown (fn [evt]
                                   (let [coords (event->dom-coords
                                                 evt
                                                 (dom/node this "process-feed"))]
                                     (prim/set-state!
                                      this
                                      (merge (prim/get-state this)
                                             {:selection {:start coords}}))))
                    :onMouseMove (fn [evt]
                                   (let [state (prim/get-state this)
                                         start-coords (get-in state
                                                              [:selection :start])]
                                     (when start-coords
                                       (prim/set-state!
                                        this
                                        (assoc-in state [:selection :end]
                                                  (event->dom-coords
                                                   evt
                                                   (dom/node this "process-feed")))))))
                    :onMouseUp (fn [evt]
                                 ;;action created todo
                                 (let [start-coords (get-in (prim/get-state this)
                                                            [:selection :start])
                                       end-coords (event->dom-coords
                                                   evt
                                                   (dom/node this "process-feed"))
                                       action (if (= start-coords end-coords)
                                                (let [{:keys [x y]} (evt-sc-coord camera (dom/node this "process-feed") evt start-coords)]
                                                  #:SC2APIProtocol.sc2api$Action
                                                  {:action-render #:SC2APIProtocol.spatial$ActionSpatial
                                                   {:action #:SC2APIProtocol.spatial$ActionSpatial
                                                    {:unit-selection-point #:SC2APIProtocol.spatial$ActionSpatialUnitSelectionPoint
                                                     {:selection-screen-coord #:SC2APIProtocol.common$PointI{:x x :y y}}}}})
                                                (let [start (evt-sc-coord camera (dom/node this "process-feed") evt start-coords)
                                                      end (evt-sc-coord camera (dom/node this "process-feed") evt end-coords)]
                                                  #:SC2APIProtocol.sc2api$Action
                                                  {:action-render #:SC2APIProtocol.spatial$ActionSpatial
                                                   {:action #:SC2APIProtocol.spatial$ActionSpatial
                                                    {:unit-selection-rect #:SC2APIProtocol.spatial$ActionSpatialUnitSelectionRect
                                                     {:selection-screen-coord
                                                      [#:SC2APIProtocol.common$RectangleI
                                                       {:p0 #:SC2APIProtocol.common$PointI{:x (:x start) :y (:y start)}
                                                        :p1 #:SC2APIProtocol.common$PointI{:x (:x end) :y (:y end)}}]
                                                      :selection-add true}
                                                     }}}))]
                                   (prim/transact!
                                    this
                                    `[(send-action
                                       ~{:port port
                                         :action action
                                         })]))
                                 (prim/set-state! this (merge (prim/get-state this) {:selection nil}))
                                 (prim/get-state this)
                                 )})))))

(def ui-process (prim/factory Process {:keyfn :db/id}))

(def init-process-local {:default-path "/Applications/StarCraft II/Maps/Melee/Simple64.SC2Map"})

(defmutation change-default-map [_]
  (remote [env] true))

(defsc ProcessStarter
  [this {:keys [available-maps]}]
  {:query [:available-maps]
   :initLocalState (fn [] init-process-local)}
  (let [{:keys [default-path selected-path]} (prim/get-state this)]
    (dom/div nil
             (dom/h4 nil "New connections will load map: "
                     (let [map (or selected-path default-path)
                           i (clojure.string/last-index-of
                              (or selected-path default-path) "/")
                           map-name (subs map (inc i))]
                       map-name))
             (dom/select

              #js {:value (or selected-path default-path)
                   :onChange (fn [change]
                               (prim/set-state! this
                                                (merge init-process-local
                                                       {:selected-path (.. change -target -value)}))
                               (prim/transact! this `[(change-default-map ~{:map (.. change -target -value)})]))}
              (map (fn [{:keys [absolute-path file-name]}]
                     (dom/option #js {:key absolute-path
                                      :value absolute-path}
                                 file-name)
                     )
                   available-maps)))))

(def ui-process-starter (prim/factory ProcessStarter))

(defsc Root [this {:keys [:root/processes :root/process-starter]}]
  {:query [{:root/processes (prim/get-query Process)}
           {:root/process-starter (prim/get-query ProcessStarter)}]
   :initial-state {:root/processes []
                   :root/process-starter {}}}
  (dom/div #js {:style #js {"margin" 10
                            "marginLeft" 65}}
           (when (empty? processes)
             (dom/h3 nil "There are no starcraft processes running yet. Why don't you start one?"))
           (ui-process-starter process-starter)
           (map #(ui-process %) processes)))


(defmethod push-received :add-observation
  [{:keys [reconciler] :as app}
   {{:keys [run-id ident-path observation] :as msg} :msg}]
  (let [state (prim/app-state reconciler)
        observation (merge
                     observation
                     {:db/id (second ident-path)})]
    (swap! state (fn [s]
                   (-> s
                       (assoc-in ident-path observation)
                       (prim/integrate-ident ident-path
                                             :append [:run/by-id run-id :run/observations]))))))

(defmethod push-received :run-added
  [{:keys [reconciler] :as app} {{:keys [run process-id]} :msg}]
  (prim/merge-component! reconciler Run run
                         :append [:process/by-id process-id :process/runs]))

(defmethod push-received :run-ended
  [{:keys [reconciler] :as app} {{:keys [run process-id]} :msg}])


(defmethod push-received :process-spawned [{:keys [reconciler] :as app} {process :msg}]
  (let [state (prim/app-state reconciler)
        ident-path [:process/by-id (:db/id process)]]
    (swap! state (fn [s]
                   (-> s
                       (assoc-in ident-path process)
                       (prim/integrate-ident ident-path :append [:root/processes]))))))

(defmethod push-received :process-died [{:keys [reconciler] :as app} {process-ident-path :msg}]
  (let [state (prim/app-state reconciler)]
    (swap! state
           (fn [s]
             (-> s
                 (update :process/by-id (fn [processes]
                                          (dissoc
                                           processes
                                           (second process-ident-path))))
                 (update :root/processes (fn [processes]
                                           (vec (filter
                                                (comp not #{process-ident-path})
                                                processes)))))))))

(defmethod push-received :game-info-added [{:keys [reconciler] :as app}
                                           {{:keys [value ident-path]} :msg}]
  (let [state (prim/app-state reconciler)]
    (swap! state
           (fn [s]
             (-> s
                 (assoc-in (conj ident-path :process/game-info)  value))))))

(defmethod push-received :latest-response [{:keys [reconciler] :as app}
                                           {{:keys [response ident-path]} :msg}]
  (let [state (prim/app-state reconciler)]
    (swap! state
           (fn [s]
             (-> s
                 (assoc-in (conj ident-path :process/latest-response) response))))))

(set! js/window.cljsc_execute
      (fn [cell]
        (clj->js {:cell-id (.-cell_id cell)
                  :run-for 200
                  :step-size 16})))

(defn mount []
  (reset! app (fc/mount @app Root "sc-viewer")))

(defn reset-app! []
  (reset! app (fc/new-fulcro-client
               :networking {:remote
                            (fw/make-websocket-networking
                             {:host "0.0.0.0:3446"
                              :push-handler (fn [m]
                                              (push-received @app m))
                              :transit-handlers {:read {"literal-byte-string" (fn [it] it)}}
                              })}
               :started-callback (fn [app]
                                   (load app ::model/processes Process
                                         {:target [:root/processes]
                                          :marker false})
                                   (load app ::model/process-starter ProcessStarter
                                         {:target [:root/process-starter]
                                          :marker false})))))

(defn ^:export init! []
  (reset-app!)
  (let [el (js/document.createElement "div")
        _ (oset! el "id" "sc-viewer")]  (.prepend (gdom/getElement "notebook") el))
  (mount))

(defn on-js-reload []
  (sente/chsk-disconnect!
   (:chsk @(:channel-socket (:remote (:networking @app)))))
  (js/setTimeout (fn []
                   (reset-app!)
                   (mount))
                 100))
