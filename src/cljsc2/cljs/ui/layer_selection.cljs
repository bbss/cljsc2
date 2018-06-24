(ns cljsc2.cljs.ui.layer_selection
  (:require [fulcro.client.dom :as dom]
            [cljsc2.cljs.material_ui :refer [ui-button]]
            [cljsc2.cljs.ui.fulcro :refer [input-with-label]]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.ui.form-state :as fs]))

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

(defn select-minimap-layer [this port x y ui-process-class]
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
       ui-process-class
       {:query (assoc-in (prim/get-query this @state)
                         [0 :process/runs 0 :run/observations 0 :feature-layer-data 0 :minimap-renders]
                         [(last path)])})
      (prim/transact!
       this
       `[(cljsc2.cljc.mutations/send-action ~{:port port :x x :y y})]))))

(defn select-render-layer [this port x y ui-process-class]
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
         ui-process-class
         {:query (assoc-in (prim/get-query this @state)
                           [0 :process/runs 0 :run/observations 0 :feature-layer-data 1 :renders] [(last path)])}))
      (prim/transact!
       this
       `[(cljsc2.cljc.mutations/send-action ~{:port port :x x :y y})]))))

(defn size-in-screen [screen-dim element-dim distance-in-element]
   (* (/ distance-in-element element-dim)
      screen-dim))

(defn event->dom-coords
  "Translate a javascript evt to a clj [x y] within the given dom element."
  [evt dom-ele]
  (let [cx (.-clientX evt)
        cy (.-clientY evt)
        BB (.getBoundingClientRect dom-ele)
        x  (- cx (.-left BB))
        y  (- cy (.-top BB))]
    [x y]))

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
       `[(cljsc2.cljc.mutations/send-action
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
       `[(cljsc2.cljc.mutations/send-action
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
   (ui-button #js {:onClick #(prim/set-state!
                               this
                               (merge local-state
                                      {:draw-size render-size
                                       :draw-size-minimap minimap-size}))}
               "Rendered resolution")
   (ui-button #js {:onClick #(prim/set-state!
                               this
                               (merge local-state
                                      {:draw-size {:x (* 2 (:x render-size))
                                                   :y (* 2 (:y render-size))}
                                       :draw-size-minimap {:x (* 2 (:x minimap-size))
                                                           :y (* 2 (:y minimap-size))}}))}
               "Enlarged (2x)")
   (ui-button #js {:onClick #(prim/set-state!
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
     `[(cljsc2.cljc.mutations/send-action
        ~{:port port
          :action
          #:SC2APIProtocol.sc2api$Action
          {:action-raw #:SC2APIProtocol.raw$ActionRaw
           {:action #:SC2APIProtocol.raw$ActionRaw
            {:camera-move #:SC2APIProtocol.raw$ActionRawCameraMove
             {:center-world-space #:SC2APIProtocol.common$Point{:x x :y y}}}}}})])))

(defn ui-camera-move-arrows [this port x y]
  (dom/div
   (ui-button #js {:onClick (send-camera-action this port (- x 3) y)}
               "left")
   (ui-button #js {:onClick (send-camera-action this port x (- y 3))}
               "down")
   (ui-button #js {:onClick (send-camera-action this port x (+ y 3))}
               "up")
   (ui-button #js {:onClick (send-camera-action this port (+ x 3) y)}
               "right")))
