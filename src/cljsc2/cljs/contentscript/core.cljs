(ns cljsc2.cljs.contentscript.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [react :as react]
            ["d3" :as d3]
            ["./imageutil.js" :as iutil]
            [clojure.core.async :refer [<! >!]]
            [cljsc2.cljs.core :refer [render-canvas feature-layer-draw-descriptions]]
            [cljsc2.cljs.actions :refer [ui-available-actions]]
            [cljsc2.cljc.model :as model]
            [datascript.transit :as dst]
            [datascript.core :as ds]
            [chromex.logging :refer-macros [log info warn error group group-end]]
            [chromex.protocols :refer [post-message!]]
            [chromex.ext.runtime :as runtime :refer-macros [connect]]
            [oops.core :refer [oset! oget]]
            [goog.dom :as gdom]
            [taoensso.sente  :as sente :refer (cb-success?)]
            [taoensso.sente.packers.transit :as sente-transit]
            [cljs.spec.alpha :as spec]
            [fulcro.client :as fc]
            [fulcro.client.dom :as dom]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.ui.form-state :as fs]
            [fulcro.client.data-fetch :refer [load] :as df]
            [fulcro.websockets :as fw]
            [fulcro.client.mutations :as m :refer [defmutation]]
            [fulcro.websockets.networking :refer [push-received]]
            [fulcro.ui.bootstrap3 :as bs]))

(reset! sente/debug-mode?_ true)

(enable-console-print!)

(def pri js/console.log)

(defonce app (atom (fc/new-fulcro-client)))

(set! js/window.appstate (fn [] (prim/app-state (:reconciler @app))))

(def uint8->binary iutil/uint8toBinaryString)

(def binary->ab32 iutil/str2ab32)

(defn map->nsmap
  [m n]
  (reduce-kv (fn [acc k v]
               (let [new-kw (if (and (keyword? k)
                                     (not (qualified-keyword? k)))
                              (keyword (str n) (name k))
                              k) ]
                 (assoc acc new-kw v)))
             {} m))

(defn render-feature-layers [canvas to-resolution last-obs state layer-path]
  (if-let [feature-layer (get-in last-obs layer-path)]
    (let [ctx (.getContext canvas "2d")
          is-rgb (or (= (last layer-path) :map) (= (last layer-path) :minimap))
          data (if is-rgb
                 feature-layer
                 (let [{:keys [data bits-per-pixel] :as fl} feature-layer]
                           (assoc fl :data (case bits-per-pixel
                                             1 (.split uint8->binary "")
                                             8 data
                                             32 (binary->ab32 (uint8->binary data))
                                             []))))
          scale (get feature-layer-draw-descriptions (last layer-path)
                     (:unit-type feature-layer-draw-descriptions))]
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


(defsc Observation [this _]
  {:query [{:feature-layer-data [{:minimap-renders []}
                                 {:renders []}]}
           {:render-data [:map :minimap]}
           :abilities
           :score
           :game-loop
           :db/id
           {:player-common [:minerals :vespene :food-used :food-cap]}
           {:raw-data [{:player [:camera]} :units]}
           {:abilities [:ability-id :requires-point]}]
   :ident [:observation/by-id :db/id]})

(def ui-observation (prim/factory Observation {:keyfn :game-loop}))

(defn event->dom-coords
  "Translate a javascript evt to a clj [x y] within the given dom element."
  [evt dom-ele]
  (let [cx (.-clientX evt)
        cy (.-clientY evt)
        BB (.getBoundingClientRect dom-ele)
        x  (- cx (.-left BB))
        y  (- cy (.-top BB))]
    [x y]))

(defn latest-observation-from-runs [runs]
  (-> runs
      last :run/observations last))

(defn render-screen [this element-size path]
  (let [image-promise (render-feature-layers
                       (dom/node this "process-feed")
                       element-size
                       (latest-observation-from-runs (:process/runs (prim/props this)))
                       (prim/get-state this)
                       path)]
    (render-selection
     (dom/node this "process-feed")
     (prim/get-state this)
     image-promise true)))

(defn render-minimap [this element-size path]
  (let [image-promise (render-feature-layers
                       (dom/node this "process-feed-minimap")
                       element-size
                       (latest-observation-from-runs (:process/runs (prim/props this)))
                       (prim/get-state this)
                       path)]
    (render-selection
     (dom/node this "process-feed")
     (prim/get-state this)
     image-promise
     false)))

(defmutation send-request [_]
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
   :minimap [:render-data :minimap]})

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
      (when (not (or (= (last path) :minimap)
                     (= (last path) :map)))
        (prim/set-query!
         this
         ui-process
         {:query (assoc-in (prim/get-query this @state)
                           [0 :process/runs 0 :run/observations 0 :feature-layer-data 1 :renders] [(last path)])}))
      (prim/transact!
       this
       `[(send-action ~{:port port :x x :y y})]))))

(defn size-in-screen [screen-dim element-dim distance-in-element]
   (* (/ distance-in-element element-dim)
      screen-dim))

(defn minimap-mouse-up [this port element-size screen-size]
  (fn [evt]
    (let [action
          (let [[x y] (event->dom-coords
                       evt
                       (dom/node this "process-feed-minimap"))]
            #:SC2APIProtocol.sc2api$Action
            {:action-render #:SC2APIProtocol.spatial$ActionSpatial
             {:action #:SC2APIProtocol.spatial$ActionSpatial
              {:camera-move #:SC2APIProtocol.spatial$ActionSpatialCameraMove
               {:center-minimap #:SC2APIProtocol.common$PointI
                {:x (size-in-screen
                     (:x screen-size)
                     (:x element-size)
                     x)
                 :y (size-in-screen
                     (:y screen-size)
                     (:y element-size)
                     y)}}}}})]
      (prim/transact!
       this
       `[(send-action
          ~{:port port
            :action action
            })])
      (prim/set-state! this (merge (prim/get-state this) {:selection nil}))
      (prim/get-state this))))

(defn screen-mouse-move [this]
  (fn [evt]
    (let [state (prim/get-state this)
          start-coords (get-in state
                               [:selection :start])]
      (when start-coords
        (prim/set-state!
         this
         (assoc-in state [:selection :end]
                   (event->dom-coords
                    evt
                    (dom/node this "process-feed"))))))))

(defn screen-mouse-up [this port element-size screen-size selected-ability]
  (fn [evt]
    (let [start-coords (get-in (prim/get-state this)
                               [:selection :start])
          end-coords (event->dom-coords
                      evt
                      (dom/node this "process-feed"))
          action
          (let [[x y] start-coords]
            (if selected-ability
              #:SC2APIProtocol.sc2api$Action
              {:action-render #:SC2APIProtocol.spatial$ActionSpatial
               {:action #:SC2APIProtocol.spatial$ActionSpatial
                {:unit-command #:SC2APIProtocol.spatial$ActionSpatialUnitCommand
                 {:target #:SC2APIProtocol.spatial$ActionSpatialUnitCommand
                  {:target-screen-coord #:SC2APIProtocol.common$PointI
                   {:x (size-in-screen
                        (:x screen-size)
                        (:x element-size)
                        x)
                    :y (size-in-screen
                        (:y screen-size)
                        (:y element-size)
                        y)}}
                  :ability-id (:ability-id selected-ability)}}}}
              (if (= start-coords end-coords)
                #:SC2APIProtocol.sc2api$Action
                {:action-render #:SC2APIProtocol.spatial$ActionSpatial
                 {:action #:SC2APIProtocol.spatial$ActionSpatial
                  {:unit-selection-point #:SC2APIProtocol.spatial$ActionSpatialUnitSelectionPoint
                   {:selection-screen-coord #:SC2APIProtocol.common$PointI
                    {:x (size-in-screen
                         (:x screen-size)
                         (:x element-size)
                         x)
                     :y (size-in-screen
                         (:y screen-size)
                         (:y element-size)
                         y)}}}}}
                #:SC2APIProtocol.sc2api$Action
                {:action-render #:SC2APIProtocol.spatial$ActionSpatial
                 {:action #:SC2APIProtocol.spatial$ActionSpatial
                  {:unit-selection-rect #:SC2APIProtocol.spatial$ActionSpatialUnitSelectionRect
                   {:selection-screen-coord
                    [#:SC2APIProtocol.common$RectangleI
                     {:p0 #:SC2APIProtocol.common$PointI
                      {:x  (size-in-screen
                            (:x screen-size)
                            (:x element-size)
                            x)
                       :y (size-in-screen
                           (:y screen-size)
                           (:y element-size)
                           y)}
                      :p1 #:SC2APIProtocol.common$PointI
                      {:x (size-in-screen
                           (:x screen-size)
                           (:x element-size)
                           (first end-coords))
                       :y (size-in-screen
                           (:y screen-size)
                           (:y element-size)
                           (second end-coords))}}]}}}})))]
      (prim/transact!
       this
       `[(send-action
          ~{:port port
            :action action
            })])
      (prim/set-state! this
                       (merge (prim/get-state this)
                              {:selection nil
                               :selected-ability nil}))
      (prim/get-state this))))

(defn ui-draw-sizes [this local-state render-size minimap-size]
  (dom/div
   "Drawing size: "
   (dom/button #js {:onClick #(prim/set-state!
                               this
                               (merge local-state
                                      {:draw-size render-size
                                       :draw-size-minimap minimap-size}))}
               "Rendered resolution")
   (dom/button #js {:onClick #(prim/set-state!
                               this
                               (merge local-state
                                      {:draw-size {:x (* 2 (:x render-size))
                                                   :y (* 2 (:y render-size))}
                                       :draw-size-minimap {:x (* 2 (:x minimap-size))
                                                           :y (* 2 (:y minimap-size))}}))}
               "Enlarged (2x)")
   (dom/button #js {:onClick #(prim/set-state!
                               this
                               (merge local-state
                                      {:draw-size {:x (* 4 (:x render-size))
                                                   :y (* 4 (:y render-size))}
                                       :draw-size-minimap {:x (* 4 (:x minimap-size))
                                                           :y (* 4 (:y minimap-size))}}))}
               "Enlarged (4x)")))

(defn send-camera-action [this port x y]
  (fn [_]
    (prim/transact!
     this
     `[(send-action
        ~{:port port
          :action
          #:SC2APIProtocol.sc2api$Action
          {:action-raw #:SC2APIProtocol.raw$ActionRaw
           {:action #:SC2APIProtocol.raw$ActionRaw
            {:camera-move #:SC2APIProtocol.raw$ActionRawCameraMove
             {:center-world-space #:SC2APIProtocol.common$Point{:x x :y y}}}}}})])))

(defn ui-camera-move-arrows [this port x y]
  (dom/div
   (dom/button #js {:onClick (send-camera-action this port (- x 3) y)}
               "left")
   (dom/button #js {:onClick (send-camera-action this port x (- y 3))}
               "down")
   (dom/button #js {:onClick (send-camera-action this port x (+ y 3))}
               "up")
   (dom/button #js {:onClick (send-camera-action this port (+ x 3) y)}
               "right")))

(defn ui-game-info [port runs camera food-used food-cap minerals vespene]
  (dom/div
   (dom/p "current camera position "
          (str camera))
   (dom/p "Supply: " food-used "/" food-cap)
   (dom/p "Minerals: " minerals " Gas: " vespene)))

(defn ui-canvas [this local-state port draw-size-minimap draw-size render-size
                 minimap-size selected-ability selected-minimap-layer-path selected-render-layer-path x y]
  (dom/div
   (dom/select
    #js {:value selected-minimap-layer-path
         :onChange (select-minimap-layer this port x y)}
    (for [[layer-name layer-path] feature-layer-minimap-paths]
      (dom/option #js {:key layer-name
                       :value layer-path} (str layer-name))))
   (dom/select
    #js {:value selected-render-layer-path
         :onChange (select-render-layer this port x y)}
    (for [[layer-name layer-path] feature-layer-render-paths]
      (dom/option #js {:key layer-name
                       :value layer-path} (str layer-name))))
   (ui-draw-sizes this local-state render-size minimap-size)
   (dom/canvas
    #js {:ref "process-feed-minimap"
         :width (or (:x draw-size-minimap) (:x minimap-size))
         :height (or (:y draw-size-minimap) (:y minimap-size))
         :onMouseUp (minimap-mouse-up this port (or draw-size-minimap minimap-size) minimap-size)})
   (dom/canvas
    #js {:ref "process-feed"
         :width (or (:x draw-size) (:x render-size))
         :height (or (:y draw-size) (:y render-size))
         :onMouseDown (fn [evt]
                        (let [coords (event->dom-coords
                                      evt
                                      (dom/node this "process-feed"))]
                          (prim/set-state!
                           this
                           (merge (prim/get-state this)
                                  {:selection {:start coords}}))))
         :onMouseMove (screen-mouse-move this)
         :onMouseUp (screen-mouse-up this port (or draw-size render-size) render-size selected-ability)})
   (ui-camera-move-arrows this port x y)))

(defn ui-timeline [runs run-size step-size]
  (let [scale (d3.scaleLinear)
        total-runs-size (reduce (fn [total {:keys [:run/started-at :run/ended-at]}]
                                  (+ total (- (or ended-at 0) (or started-at 0))))
                                0
                                runs)]
    (doto scale
      (.domain #js [0 (+ total-runs-size run-size)])
      (.range #js [0 400]))
    (apply (partial dom/div #js {:style #js {:margin "10px"
                                             :display "flex"
                                             :width "400px"
                                             :height "4px"
                                             :justifyContent "flex-end"
                                             :boxShadow "0 3px 6px rgba(0,0,0,0.16), 0 3px 6px rgba(0,0,0,0.23)"
                                             }})
           (concat
            (map (fn [{:keys [run/started-at run/ended-at] :as run}]
                   (dom/div #js {:style #js {:width (str (- (scale (or ended-at
                                                                       total-runs-size))
                                                            (scale started-at)) "px")
                                             :height "6px"
                                             :borderBottom "1px solid black"
                                             :borderRight "1px solid black"}}))
                 runs)
            [(apply (partial dom/div #js {:style #js {:display "flex"
                                                           :width (str (- (scale (+ total-runs-size run-size))
                                                                          (scale total-runs-size)) "px")
                                                           :height "6px"
                                                           :borderBottom "1px dashed green"
                                                           :borderRight "1px solid green"
                                                           }})
                         (map (fn [step] (dom/div #js {:style
                                                       #js {:width (str (- (scale step)
                                                                           (scale (- step step-size))) "px")
                                                            :borderRight "1px solid orange"
                                                            :height "4px"}}))
                              (take (scale run-size)
                                    (range total-runs-size
                                           (+ total-runs-size run-size)
                                           step-size))))]))))

(defn close-connection [this port]
  (prim/transact!
   this
   `[(send-request
      ~{:port port
        :request #:SC2APIProtocol.sc2api$RequestQuit{:quit {}}
        })]))

(defn clear-jupyter-events [i]
  (when (and js/window.Jupyter i)
    (js/window.Jupyter.keyboard_manager.register_events i)))

(defn render-field [component field renderer]
  (let [form         (prim/props component)
        entity-ident (prim/get-ident component form)
        id           (str (first entity-ident) "-" (second entity-ident))
        is-dirty?    (fs/dirty? form field)
        clean?       (not is-dirty?)
        validity     (fs/get-spec-validity form field)
        is-invalid?  (= :invalid validity)
        value        (get form field "")]
    (renderer {:dirty?   is-dirty?
               :ident    entity-ident
               :id       id
               :clean?   clean?
               :validity validity
               :invalid? is-invalid?
               :value    value})))

(defn input-with-label
  "A non-library helper function, written by you to help lay out your form."
  ([component field field-label validation-string input-element options]
   (render-field component field
                 (fn [{:keys [invalid? id dirty?]}]
                   (when js/window.Jupyter
                     (js/window.Jupyter.keyboard_manager.register_events input-element))
                   (bs/labeled-input (merge {:error           (when invalid? validation-string)
                                             :id              id
                                             :warning         (when dirty? "(unsaved)")
                                             :input-generator input-element}
                                            options) field-label))))
  ([component field field-label validation-string options]
   (render-field component field
                 (fn [{:keys [invalid? id dirty? value invalid ident] :as arg}]
                   (bs/labeled-input
                    (merge
                     {:value    value
                      :ref      clear-jupyter-events
                      :id       id
                      :error    (when invalid? validation-string)
                      :warning  (when dirty? "(unsaved)")
                      :onBlur   #(prim/transact!
                                  component
                                  `[(fs/mark-complete! {:entity-ident ~ident
                                                        :field        ~field})])
                      :onChange (case (:type options)
                                  "number" (fn [e]
                                             (let [value (.-value (.-target e))]
                                               (if (and (string? value)
                                                        (empty? value))
                                                 (m/set-integer! component field :value 1)
                                                 (m/set-integer! component field :value value)))
                                             )
                                  "checkbox" (fn [e]
                                               (let [value (.-checked (.-target e))]
                                                 (m/set-value! component field value)))
                                  (fn [e]
                                    (m/set-string! component field :event e)))}
                     options) field-label)))))

(defn update-player-setup-field [this id field]
  (fn [e]
    (prim/transact!
     this
     `[(update-player-setup ~{:field field
                              :id id
                              :value (.-value (.-target e))})])))

(defsc PlayerSetup [this {:keys [:SC2APIProtocol.sc2api$PlayerSetup/difficulty
                                 :SC2APIProtocol.sc2api$PlayerSetup/type
                                 :SC2APIProtocol.sc2api$PlayerSetup/race
                                 :ui/editting
                                 :db/id]}]
  {:query [:SC2APIProtocol.sc2api$PlayerSetup/difficulty
           :SC2APIProtocol.sc2api$PlayerSetup/type
           :SC2APIProtocol.sc2api$PlayerSetup/race
           :ui/editting
           :db/id
           fs/form-config-join]
   :ident [:player-setup/by-id :db/id]
   :form-fields #{:SC2APIProtocol.sc2api$PlayerSetup/race}}
  (dom/div
   (if editting
     (dom/div (dom/button #js {:onClick (fn [] (prim/transact! this `[(abort-player-setup ~{:id id})]))}
                          "Abort player setup")
              (dom/button #js {:onClick (fn [] (prim/transact! this `[(submit-player-setup ~{:id id
                                                                                             :diff (fs/dirty-fields (prim/props this) true)})]))}
                          "Save player setup"))
     (dom/button #js {:onClick (fn [] (prim/transact! this `[(edit-player-setup ~{:id id})]))}
                 "Edit player setup"))
   (if editting
     (dom/div
      (dom/select #js {:value type
                       :onChange (update-player-setup-field this id :SC2APIProtocol.sc2api$PlayerSetup/type)}
                  (for [type ["Participant" "Computer"]]
                    (dom/option #js {:key type
                                     :value type}
                                type)))
      (dom/p "Race:")
      (dom/select #js {:value race
                       :onChange (update-player-setup-field this id :SC2APIProtocol.sc2api$PlayerSetup/race)}
                  (for [race ["Protoss" "Terran" "Zerg" "Random"]]
                    (dom/option #js {:key race
                                     :value race}
                                race)))
      (when (= type "Computer")
        (dom/div
         (dom/p "Difficulty")
         (dom/select
          #js {:value difficulty
               :onChange (update-player-setup-field this id :SC2APIProtocol.sc2api$PlayerSetup/difficulty)}
          (map (fn [v]
                 (dom/option #js {:key v
                                  :value v}
                             v))
               ["VeryEasy" "Easy" "Medium" "MediumHard" "Harder" "Hard"
                "VeryHard" "CheatVision" "CheatMoney" "CheatInsane"])))))
     (dom/div (dom/p "Race: " race)
              (when difficulty (dom/p "Difficulty: " difficulty))
              (dom/p "Type: " type)))))

(defmutation edit-player-setup [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state
                 (fn [s]
                   (-> s
                       (fs/add-form-config* PlayerSetup [:player-setup/by-id id])
                       (assoc-in [:player-setup/by-id id :ui/editting] true))))))

(defmutation abort-player-setup [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state
                 (fn [s]
                   (-> s
                       (fs/pristine->entity* [:player-setup/by-id id])
                       (assoc-in [:player-setup/by-id id :ui/editting] false))))))

(defmutation submit-player-setup [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state
                 (fn [s]
                   (-> s
                       (assoc-in [:player-setup/by-id id :ui/editting] false)
                       (fs/entity->pristine* [:player-setup/by-id id])))))
  (remote [env] true))

(def ui-player-setup (prim/factory PlayerSetup {:keyfn :db/id}))


(defsc Resolution [this {:keys [:SC2APIProtocol.common$Size2DI/x
                                :SC2APIProtocol.common$Size2DI/y
                                :ui/editting
                                :db/id]}]
  {:query [:SC2APIProtocol.common$Size2DI/x
           :SC2APIProtocol.common$Size2DI/y
           :db/id
           :ui/editting
           fs/form-config-join]
   :ident [:resolution/by-id :db/id]
   :form-fields #{:SC2APIProtocol.common$Size2DI/x
                  :SC2APIProtocol.common$Size2DI/y}}
  (dom/div
   (if editting
     (dom/div (dom/button #js {:onClick
                               #(prim/transact! this `[(abort-resolution ~{:id id})])}
                          "Abort resolution")
              (dom/button #js {:onClick
                               #(prim/transact! this `[(submit-resolution ~{:id id
                                                                            :diff (fs/dirty-fields (prim/props this) true)})])}
                          "Save resolution"))
     (dom/button #js {:onClick #(prim/transact! this `[(edit-resolution ~{:id id})])}
                 "Edit resolution"))
   (if editting
     (dom/div
      (dom/p "X: ")
      (input-with-label this :SC2APIProtocol.common$Size2DI/x x
                        "Rendered horizontal pixels"
                        {:type "number"
                         :min 1
                         :max 1024
                         :style #js {:width "100px"}})
      (dom/p "Y: ")
      (input-with-label this :SC2APIProtocol.common$Size2DI/y y
                        "Rendered vertical pixels"
                        {:type "number"
                         :min 1
                         :max 1024
                         :style #js {:width "100px"}}))
     (dom/div
      (dom/p (str "X: " x))
      (dom/p (str "Y: " y))))))

(defmutation edit-resolution [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state
                 (fn [s]
                   (-> s
                       (fs/add-form-config* Resolution [:resolution/by-id id])
                       (assoc-in [:resolution/by-id id :ui/editting] true))))))

(defmutation abort-resolution [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state
                 (fn [s]
                   (-> s
                       (fs/pristine->entity* [:resolution/by-id id])
                       (assoc-in [:resolution/by-id id :ui/editting] false))))))

(defmutation submit-resolution [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state
                 (fn [s]
                   (-> s
                       (assoc-in [:resolution/by-id id :ui/editting] false)
                       (fs/entity->pristine* [:resolution/by-id id])))))
  (remote [env] true))

(def ui-resolution (prim/factory Resolution))

(defsc SpatialCameraSetup
  [this {:keys [SC2APIProtocol.sc2api$SpatialCameraSetup/width
                SC2APIProtocol.sc2api$SpatialCameraSetup/resolution
                SC2APIProtocol.sc2api$SpatialCameraSetup/minimap-resolution
                :db/id]}]
  {:query [:SC2APIProtocol.sc2api$SpatialCameraSetup/width
           {:SC2APIProtocol.sc2api$SpatialCameraSetup/resolution (prim/get-query Resolution)}
           {:SC2APIProtocol.sc2api$SpatialCameraSetup/minimap-resolution (prim/get-query Resolution)}
           :db/id
           fs/form-config-join]
   :ident [:spatial-camera-setup/by-id :db/id]
   :form-fields #{:SC2APIProtocol.sc2api$SpatialCameraSetup/width
                  :SC2APIProtocol.sc2api$SpatialCameraSetup/resolution
                  :SC2APIProtocol.sc2api$SpatialCameraSetup/minimap-resolution}}
  (dom/div
   (dom/h4 "Minimap resolution")
   (ui-resolution minimap-resolution)
   (dom/h4 "Map resolution")
   (ui-resolution resolution)))

(def ui-spatial-camera-setup (prim/factory SpatialCameraSetup))

(defsc InterfaceOptions
  [this {:keys [:SC2APIProtocol.sc2api$InterfaceOptions/raw
                :SC2APIProtocol.sc2api$InterfaceOptions/score
                :SC2APIProtocol.sc2api$InterfaceOptions/feature-layer
                :SC2APIProtocol.sc2api$InterfaceOptions/render
                :ui/editting
                :db/id]}]
  {:query [:SC2APIProtocol.sc2api$InterfaceOptions/raw
           :SC2APIProtocol.sc2api$InterfaceOptions/score
           {:SC2APIProtocol.sc2api$InterfaceOptions/feature-layer (prim/get-query SpatialCameraSetup)}
           {:SC2APIProtocol.sc2api$InterfaceOptions/render (prim/get-query SpatialCameraSetup)}
           :ui/editting
           :db/id
           fs/form-config-join]
   :ident [:interface-options/by-id :db/id]
   :form-fields #{:SC2APIProtocol.sc2api$InterfaceOptions/raw
                  :SC2APIProtocol.sc2api$InterfaceOptions/score
                  :SC2APIProtocol.sc2api$InterfaceOptions/feature-layer
                  :SC2APIProtocol.sc2api$InterfaceOptions/render}}
  (dom/div (if editting
             (dom/div
              (dom/button #js {:onClick
                               (fn [] (prim/transact!
                                       this
                                       `[(abort-interface-options
                                          ~{:id id})]))}
                          "Abort raw/score")
              (dom/button #js {:onClick
                               (fn [] (prim/transact!
                                       this
                                       `[(submit-interface-options
                                          ~{:id id
                                            :diff (fs/dirty-fields (prim/props this) true)})]))}
                          "Save raw/score"))
             (dom/button #js {:onClick (fn [] (prim/transact! this `[(edit-interface-options ~{:id id})]))}
                         "Edit raw/score"))
           (if editting
             (dom/div
              #js {:style #js {:display "flex"}}
              (dom/p "Raw data: ")
              (input-with-label this :SC2APIProtocol.sc2api$InterfaceOptions/raw
                                raw "" {:type "checkbox"
                                        :checked raw})
              (dom/p "Score: ")
              (input-with-label this :SC2APIProtocol.sc2api$InterfaceOptions/score
                                score "" {:type "checkbox"
                                          :checked score}))
             (dom/div
              #js {:style #js {:display "flex"}}
              (dom/div
               (dom/p (str "Raw data: " raw))
               (dom/p (str "Score: " score)))
              ))
           (dom/div
            #js {:style #js {:display "flex"}}
            (dom/div #js {:style #js {:margin "10px"}}
             (dom/h3 "Feature layers")
             (ui-spatial-camera-setup feature-layer))
            (dom/div #js {:style #js {:margin "10px"}}
             (dom/h3"Full RGB layers")
             (ui-spatial-camera-setup render)))))

(defmutation edit-interface-options [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state
                 (fn [s]
                   (-> s
                       (fs/add-form-config* InterfaceOptions [:interface-options/by-id id])
                       (assoc-in [:interface-options/by-id id :ui/editting] true))))))

(defmutation abort-interface-options [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state
                 (fn [s]
                   (-> s
                       (fs/pristine->entity* [:interface-options/by-id id])
                       (assoc-in [:interface-options/by-id id :ui/editting] false))))))

(defmutation submit-interface-options [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state
                 (fn [s]
                   (-> s
                       (assoc-in [:interface-options/by-id id :ui/editting] false)
                       (fs/entity->pristine* [:interface-options/by-id id])))))
  (remote [env] true))

(defn update-interface-options-field [this id field]
  (fn [e]
    (prim/transact!
     this
     `[(update-interface-setup
        ~{:field field
          :id id
          :value (.-value (.-target e))})])))

(def ui-interface-options (prim/factory InterfaceOptions))

(defsc GameConfig [this {:keys [db/id
                                game-config/interface-options
                                game-config/player-setups]}]
  {:query [:db/id
           {:game-config/interface-options (prim/get-query InterfaceOptions)}
           {:game-config/player-setups (prim/get-query PlayerSetup)}
           fs/form-config-join]
   :ident [:game-config/by-id :db/id]
   :form-fields #{:game-config/player-setups}}
  (dom/div #js {:style #js {:display "flex"}}
           (map #(ui-player-setup %)
                (or player-setups []))
           (ui-interface-options interface-options)))

(def ui-game-config (prim/factory GameConfig))

(defsc RunConfig [this {:keys [db/id
                               run-config/step-size
                               run-config/run-size
                               run-config/restart-on-episode-end
                               ui/editting]}
                  {:keys [process/runs
                          process/game-loop
                          process/savepoint-at
                          process/make-savepoint
                          process/load-savepoint]}]
  {:query [:db/id
           :ui/editting
           :run-config/step-size
           :run-config/run-size
           :run-config/restart-on-episode-end
           fs/form-config-join]
   :ident [:run-config/by-id :db/id]
   :form-fields #{:run-config/step-size :run-config/run-size
                  :run-config/restart-on-episode-end}}
  (dom/div
   (dom/button #js {:onClick #(make-savepoint)}
               (str "Set the time-travel savepoint at " game-loop))
   (when savepoint-at
     (dom/button #js {:onClick #(load-savepoint)}
                 (str "Load savepoint at game-loop " savepoint-at)))
   (when editting
     (dom/div
              (dom/button #js {:onClick #(prim/transact!
                                          this
                                          `[(abort-run-config ~{:id id})])}
                          "Abort")
              (dom/button #js {:onClick #(prim/transact!
                                          this
                                          `[(submit-run-config
                                             ~{:id id
                                               :diff (fs/dirty-fields (prim/props this) true)})])}
                          "Save")))
   (if editting
     (dom/div
      #js {:style #js {:display "flex"}}
      (dom/div "Step size: ")
      (input-with-label this :run-config/step-size step-size
                        "Step size for the run should be a whole number"
                        {:type "number"
                         :min 1
                         :style #js {:width "100px"}})
      (dom/div "Run size :")
      (input-with-label this :run-config/run-size run-size
                        "Run size for the run should be a whole number"
                        {:type "number"
                         :min 1
                         :style #js {:width "100px"}})
      (dom/div "Restart ended episodes and keep running:")
      (input-with-label this :run-config/restart-on-episode-end restart-on-episode-end
                        "Run size for the run should be a whole number"
                        {:type "checkbox"
                         :checked restart-on-episode-end
                         :style #js {:width "100px"}}))
     (dom/button #js {:onClick (fn [_] (prim/transact! this `[(edit-run-config ~{:id id})]))}
                 "Adjust run settings"))
   (ui-timeline runs
                run-size
                step-size)))


(defsc Run [this {:keys [db/id :run/observations]}]
  {:query [{:run/observations (prim/get-query Observation)}
           {:run/run-config (prim/get-query RunConfig)}
           :run/ended-at
           :run/started-at
           :db/id]
   :ident [:run/by-id :db/id]})

(defmutation edit-run-config [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state (fn [s]
                         (-> s
                             (fs/add-form-config* RunConfig [:run-config/by-id id])
                             (assoc-in [:run-config/by-id id :ui/editting] true))))))

(defmutation abort-run-config [{:keys [id]}]
  (action [{:keys [state]}]
          (swap! state (fn [s]
                         (-> s
                             (fs/pristine->entity* [:run-config/by-id id])
                             (assoc-in [:run-config/by-id id :ui/editting] false)
                             )))))

(defmutation submit-run-config [{:keys [id delta]}]
  (action [{:keys [state]}]
          (swap! state (fn [s]
                         (-> s
                             (assoc-in [:run-config/by-id id :ui/editting] false)
                             (fs/entity->pristine* [:run-config/by-id id])
                             ))))
  (remote [env] true))

(defmutation make-savepoint [{:keys [port game-loop]}]
  (action [{:keys [state]}]
          (swap! state (fn [s]
                         (-> s
                             (assoc-in [:process/by-id port :process/savepoint-at]
                                       game-loop)))))
  (remote [env] true))

(defmutation load-savepoint [{:keys [port]}]
  (remote [env] true))

(defmutation make-conn [_]
  (value [{:keys [state]}]
         (let [knowledge-base (:root/starcraft-static-data @state)]
            (swap! state assoc :root/starcraft-static-data
                   (ds/conn-from-datoms
                    (:eavt knowledge-base)
                    (:schema knowledge-base))))))

(defmutation update-map [{:keys [id path]}]
  (action [{:keys [state]}]
          (swap! state assoc-in [:map-config/by-id id :map-config/path] path))
  (remote [env] true))

(defmutation update-player-setup [{:keys [id field value]}]
  (action [{:keys [state]}]
          (swap! state assoc-in [:player-setup/by-id id field] value)))

(def ui-run-config (prim/factory RunConfig))

(defn paste-first-element [text]
  (.setValue (.-CodeMirror (aget (js/document.querySelectorAll ".CodeMirror") 0)) text))

(defsc Process [this {:keys [db/id
                             process/port
                             process/runs
                             process/savepoint-at
                             process/latest-response
                             process/game-info
                             process/run-config]}
                {:keys [knowledge-base]}]
  {:query [{:process/runs (prim/get-query Run)}
           {:process/run-config (prim/get-query RunConfig)}
           :process/port
           :process/latest-response
           :process/game-info
           :process/savepoint-at
           :db/id]
   :initLocalState (fn [] {:selected-minimap-layer-path [:render-data :minimap]
                           :selected-render-layer-path [:render-data :map]})
   :componentDidUpdate (fn [_ _]
                         (let [{:keys [draw-size
                                       draw-size-minimap
                                       selected-render-layer-path
                                       selected-minimap-layer-path]}
                               (prim/get-state this)
                               runs (:process/runs (prim/props this))
                               render-size (get-in (latest-observation-from-runs runs) [:render-data :map :size])
                               minimap-size (get-in (latest-observation-from-runs runs) [:render-data :minimap :size])]
                           (render-screen this
                                          (or draw-size render-size)
                                          selected-render-layer-path)
                           (render-minimap this
                                           (or draw-size-minimap minimap-size)
                                           selected-minimap-layer-path)))
   :componentDidMount (fn []
                        (let [{:keys [draw-size
                                      draw-size-minimap
                                      selected-render-layer-path
                                      selected-minimap-layer-path]}
                              (prim/get-state this)
                              runs (:process/runs (prim/props this))
                              render-size (get-in (latest-observation-from-runs runs) [:render-data :map :size])
                              minimap-size (get-in (latest-observation-from-runs runs) [:render-data :minimap :size])]
                          (prim/set-state!
                           this
                           (merge (prim/get-state this)
                                  {:draw-size {:x (* 2 (:x render-size))
                                               :y (* 2 (:y render-size))}
                                   :draw-size-minimap {:x (* 2 (:x minimap-size))
                                                       :y (* 2 (:y minimap-size))}}))
                          (ui-draw-sizes this (prim/get-state this) render-size minimap-size)
                          (render-screen this
                                         (or draw-size render-size)
                                         selected-render-layer-path)
                          (render-minimap this
                                          (or draw-size-minimap minimap-size)
                                          selected-minimap-layer-path)))
   :ident [:process/by-id :db/id]}
  (let [latest-observation (latest-observation-from-runs runs)
        {:keys [x y] :as camera} (get-in latest-observation [:raw-data :player :camera])
        {:keys [food-used food-cap vespene minerals]} (:player-common latest-observation)
        {:keys [draw-size
                draw-size-minimap
                selected-minimap-layer-path
                selected-render-layer-path
                selected-ability
                ] :as local-state} (prim/get-state this)
        render-size (get-in latest-observation [:render-data :map :size])
        minimap-size (get-in latest-observation [:render-data :minimap :size])
        game-loop (:game-loop latest-observation)]
    (dom/div
     (dom/button #js {:style #js {"float" "right"}
                      :onClick #(close-connection this port)} "Close process")
     (ui-available-actions
      (:abilities latest-observation)
      knowledge-base
      selected-ability
      (fn [id ability-name requires-point]
        (prim/set-state! this (merge
                               local-state
                               {:selected-ability {:ability-id id
                                                   :ability-name ability-name
                                                   :requires-point requires-point}}))))
     (str selected-ability)
     (ui-run-config
      (prim/computed run-config
                     {:process/runs runs
                      :process/savepoint-at savepoint-at
                      :process/game-loop game-loop
                      :process/make-savepoint #(prim/transact!
                                                this
                                                `[(make-savepoint ~{:port port
                                                                    :game-loop game-loop})])
                      :process/load-savepoint #(prim/transact!
                                                this
                                                `[(load-savepoint ~{:port port})])}))
     (dom/button
      #js {:onClick
           (fn [e]
             (paste-first-element
              (str '(execute-plans))))} "Add inactive run")
     (dom/button
      #js {:onClick
           (fn [e]
             (paste-first-element
              (str '(execute-plans
                     (build "SCV" :until-count 25)
                     (build "SupplyDepot")
                     (build "Barracks" :until-count 5)
                     (build "Marine" :until-count 1000)
                     (select "Marine" :at-least 10
                             :whenever-goals-succeed
                             (add-plan (attack :at-location :enemy-base)))
                     ))))} "Add 5rax Marine build")
     (dom/button
      #js {:onClick
           (fn [e]
             (paste-first-element
              (str '(execute-plans
                     (build "SCV" :until-count 25)
                     (build "Refinery")
                     (build "SupplyDepot")
                     (build "Barracks"
                            :whenever-goals-succeed (add-plans
                                                     (build "Factory")
                                                     (build "Refinery" :until-count 2)
                                                     (build "Cyclone" :until-count 3)
                                                     (select "Cyclone" :at-least 3
                                                             :and-do (select "Marine" :at-least 15)
                                                             :whenever-goals-succeed
                                                             (add-plan (attack :at-location :enemy-base)))
                                                     ))
                     (build "Marine" :until-count 5)
                     (keep-gas-mined)
                     (camera-follow-army)))))} "Add marine/cyclones build")
     (ui-game-info port runs camera food-used food-cap minerals vespene)
     (ui-canvas this local-state port draw-size-minimap draw-size render-size
                minimap-size selected-ability selected-minimap-layer-path selected-render-layer-path x y))))


(def ui-process (prim/factory Process {:keyfn :db/id}))

(defsc MapConfig [this _]
  {:query [:map-config/path :db/id]
   :ident [:map-config/by-id :db/id]})

(defsc ProcessStarter
  [this {:keys [process-starter/available-maps
                process-starter/game-config
                process-starter/map-config]} {:keys [processes]}]
  {:query [:process-starter/available-maps
           {:process-starter/map-config (prim/get-query MapConfig)}
           {:process-starter/game-config (prim/get-query GameConfig)}
           fs/form-config-join]
   :form-fields #{:process-starter/game-config}}
  (dom/div
   (dom/h4
    "New connections will load map: "
    (let [map (:map-config/path map-config)
            i (clojure.string/last-index-of
               (:map-config/path map-config) "/")
            map-name (subs map (inc i))]
      map-name))
   (dom/select
    #js {:value (:map-config/path map-config)
         :onChange (fn [e]
                     (prim/transact! this `[(update-map ~{:id (:db/id map-config)
                                                          :path (.-value (.-target e))})]))}
    (map (fn [{:keys [absolute-path file-name]}]
           (dom/option #js {:key absolute-path
                            :value absolute-path}
                       file-name))
         available-maps))
   (when (seq game-config) (ui-game-config game-config))
   #_(dom/h4 "Commands will be executed on:"
           (dom/select
            #js {:value 5000
                 :onChange (fn [e] (pri "implement"))}
            (map (fn [{:keys [process/port]}] (dom/option #js {:key port} port))
                 processes)))))

(def ui-process-starter (prim/factory ProcessStarter))

(defsc Root [this {:keys [:root/processes :root/process-starter :root/starcraft-static-data]}]
  {:query [{:root/processes (prim/get-query Process)}
           {:root/process-starter (prim/get-query ProcessStarter)}
           :root/starcraft-static-data]}
  (dom/div #js {:style #js {"margin" 10
                            "marginLeft" 65}}
           (when (empty? processes)
             (dom/h3 "There are no starcraft processes running yet, they will start automatically when you run a code cell. (play button or shortcut ctrl-enter)"))
           (when (and (not (empty? process-starter)) (not (seq processes)))
             (ui-process-starter (prim/computed process-starter {:processes processes})))
           (map #(ui-process (prim/computed % {:knowledge-base starcraft-static-data}))
                processes)))

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

(defmethod push-received :savepoint-added
  [{:keys [reconciler] :as app} {{:keys [ident-path savepoint-at]} :msg}]
  (let [state (prim/app-state reconciler)]
    (swap! state assoc-in (conj ident-path :process/savepoint-at) savepoint-at)))

(defmethod push-received :run-config-added
  [{:keys [reconciler] :as app} {{:keys [run-config process-ident]} :msg}]
  (let [form-merged (fs/add-form-config RunConfig run-config)]
    (prim/merge-component! reconciler RunConfig form-merged)
    #_(prim/merge-component! reconciler Process {:db/id 1
                                                 :process/run-config run-config})
    (swap! (prim/app-state reconciler) (fn [s]
                                         (assoc-in s (conj process-ident :process/run-config) [:run-config/by-id (:db/id run-config)] )))))

(defmethod push-received :run-ended
  [{:keys [reconciler] :as app} {{:keys [ident-path ended-at]} :msg}]
  (let [state (prim/app-state reconciler)]
    (swap! state (fn [s]
                   (let [observation-ident-paths (-> (get-in s (conj ident-path :run/observations))
                                                     reverse
                                                     rest)]
                     (-> (reduce (fn [s [path key]]
                                   (update s path (fn [observations]
                                                    (dissoc observations key))))
                                 s observation-ident-paths)
                         (assoc-in (conj ident-path :run/ended-at) ended-at)
                         (update-in (conj ident-path :run/observations)
                                    (comp vector last))))))))

(defmethod push-received :run-started
  [{:keys [reconciler] :as app} {{:keys [ident-path started-at]} :msg}]
  (let [state (prim/app-state reconciler)]
    (swap! state assoc-in (conj ident-path :run/started-at) started-at)))

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

(defn mount []
  (reset! app (fc/mount @app Root "sc-viewer")))

(defn reset-app! []
  (reset! app (fc/new-fulcro-client
               :networking {:remote
                            (fw/make-websocket-networking
                             {:host (case :local-staging
                                      :local "0.0.0.0:3446"
                                      :local-staging "192.168.1.94:3446"
                                      :remote-staging "cljsc.org"
                                      (pri "no ip for env"))
                              :push-handler (fn [m]
                                              (push-received @app m))
                              :transit-handlers {:read (merge {"literal-byte-string" (fn [it] it)}
                                                              datascript.transit/read-handlers)}
                              })}
               :started-callback (fn [app]
                                   (js/setTimeout #(do (load app ::model/processes Process
                                                             {:target [:root/processes]
                                                              :marker false})
                                                       (load app ::model/process-starter ProcessStarter
                                                             {:target [:root/process-starter]
                                                              :marker false})
                                                       (load app :root/starcraft-static-data Root
                                                             {:marker false
                                                              :post-mutation `make-conn}))
                                                  5000)))))

(defn init! []
  (when-let [el (aget (.querySelectorAll js/document "#sc-viewer") 0)]
    (.remove el))
  (reset-app!)
  (let [el (js/document.createElement "div")
        _ (oset! el "id" "sc-viewer")]  (.prepend (gdom/getElement "notebook") el))
  (mount))

(defn ^:dev/after-load on-js-reload []
  (when-let [el (aget (.querySelectorAll js/document "#sc-viewer") 0)]
    (.remove el))
  (let [el (js/document.createElement "div")
        _ (oset! el "id" "sc-viewer")]  (.prepend (gdom/getElement "notebook") el))
  (go-loop [stopped (<! (:ch-recv @(:channel-socket (:remote (:networking @app)))))]
    (reset-app!)
    (mount))
  (sente/chsk-disconnect!
   (:chsk @(:channel-socket (:remote (:networking @app))))))


;;in notebook page during devtime.
;; <script type="text/javascript">
;; setInterval(function () {
;;                          [].forEach.call(document.querySelectorAll('.form-control'), i => Jupyter.keyboard_manager.register_events(i))
;;                          }, 1000)
;; </script>
