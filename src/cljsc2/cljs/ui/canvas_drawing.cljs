(ns cljsc2.cljs.ui.canvas_drawing
  (:require cljsjs.d3
            [d3 :as d3]
            ["./imagedrawing.js" :as iutil]
            [cljsc2.cljs.selectors :refer [latest-observation-from-runs]]
            [cljsc2.cljs.ui.layer_selection :refer [feature-layer-minimap-paths feature-layer-render-paths
                                                    select-minimap-layer select-render-layer
                                                    ui-draw-sizes minimap-mouse-up screen-mouse-up
                                                    event->dom-coords screen-mouse-move ui-camera-move-arrows]]
            [cognitect.transit :as transit]
            [cljsc2.cljs.colors :refer [discrete-color-palette
                                        hot-palette
                                        player-absolute-colors]]
            [fulcro.client :as fc]
            [fulcro.client.dom :as dom]
            [fulcro.client.primitives :as prim :refer [defsc]]))

(def feature-layer-draw-descriptions
  {:unit-density-aa (doto (d3.scaleLinear)
                      (.domain #js [0 256])
                      (.range hot-palette))
   :selected (doto (d3/scaleOrdinal)
               (.domain #js [0 1])
               (.range #js ["rgba(0,0,0,0)"
                            "rgba(20,200,20,1)"]))
   :player-relative (doto (d3/scaleOrdinal)
                      (.domain (clj->js (range 1 6)))
                      (.range #js ["rgba(0,0,0,1)"
                                   "rgba(0,142,0, 1)"
                                   "rgba(255, 255, 0, 1)"
                                   "rgba(129, 166, 196, 1)"
                                   "rgba(113,25,34, 1)"
                                   ]))
   :unit-type (doto (d3/scaleOrdinal)
                (.domain (clj->js (range 0 1851)))
                (.range discrete-color-palette))
   :unit-energy (doto (d3/scaleLinear)
                  (.domain #js [0 1000])
                  (.range hot-palette))
   :unit-density (doto (d3/scaleLinear)
                   (.domain #js [0 16])
                   (.range hot-palette))
   :player-id (doto (d3/scaleOrdinal)
                (.domain (clj->js (range 1 17)))
                (.range player-absolute-colors))
   :visibility-map (doto (d3/scaleOrdinal)
                     (.domain (clj->js (range 0 4)))
                     (.range #js ["rgba(0,0,0,1)"
                                  "rgba(255,255,255, 0.25)"
                                  "rgba(255,255,255, 0.6)"]))
   :power (doto (d3/scaleLinear)
            (.domain #js [0 1])
            (.range  #js ["rgba(0,0,0,0)" "blue"]))
   :unit-hit-points (doto (d3/scaleLinear)
                      (.domain #js [0 1600])
                      (.range hot-palette))
   :height-map (doto (d3/scaleLinear)
                 (.domain #js [0 255])
                 (.ticks 400)
                 (.range #js ["rgba(0,0,0,0)" "brown"]))
   :unit-shield (doto (d3/scaleLinear)
                  (.domain #js [0 1000])
                  (.range hot-palette))
   :creep (doto (d3/scaleLinear)
            (.domain #js [0 1])
            (.range #js ["rgba(0,0,0,0)" "purple"]))})

(declare msg)

(defn to-colored-clamped-arr [from-arr to-arr-size scale]
  (let [to-arr (js/Uint8ClampedArray. to-arr-size)]
    (loop [i 0
           d 0]
      (let [color-string (scale (aget from-arr i))
            [r g b a] (.split (.slice color-string 5 (dec (.-length color-string))) ",")
            alpha (* 255 a)]
        (aset to-arr d r)
        (aset to-arr (+ d 1) g)
        (aset to-arr (+ d 2) b)
        (aset to-arr (+ d 3) alpha)
        (if (< i (inc (.-length from-arr)))
          (recur (inc i) (+ d 4))
          to-arr)))))

(defn to-rgb-clamped-arr [from-arr to-arr-size]
  (let [to-arr (js/Uint8ClampedArray. to-arr-size)]
    (loop [i 0
           d 0]
      (let []
        (aset to-arr d (aget from-arr i))
        (aset to-arr (+ d 1) (aget from-arr (inc i)))
        (aset to-arr (+ d 2) (aget from-arr (inc (inc i))))
        (aset to-arr (+ d 3) 255)
        (if (< i (inc (.-length from-arr)))
          (recur (+ i 3) (+ d 4))
          to-arr)))))

(def uint8->binary iutil/uint8toBinaryString)

(def binary->ab32 iutil/str2ab32)

(defn render-canvas [canvas feature-layer-name {:keys [data bits-per-pixel size]}
                     scale to-resolution is-rgb]
  (let [arr-size (* 4 (:x size) (:y size))
        image-width (:x size)
        image-height (:y size)
        ctx (.getContext canvas "2d")
        data (if is-rgb
               (to-rgb-clamped-arr data arr-size)
               (to-colored-clamped-arr
                data
                arr-size
                scale))
        image-p (js/createImageBitmap
                 (js/ImageData.
                  data
                  image-width image-height))
        render-cb (fn [img-data]
                    (.drawImage ctx
                                img-data 0 0 (:x to-resolution) (:y to-resolution)))]
    (set! (.-fillStyle ctx) "white")
    (.fillRect ctx 0 0 (:x to-resolution) (:y to-resolution))
    [image-p render-cb]))

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
     (dom/node this "process-feed-minimap")
     (prim/get-state this)
     image-promise
     false)))

(defn ui-canvas [this local-state port draw-size-minimap draw-size render-size
                 minimap-size selected-ability selected-minimap-layer-path selected-render-layer-path x y ui-process-class]
  (dom/div
   (dom/select
    #js {:value selected-minimap-layer-path
         :onChange (select-minimap-layer this port x y ui-process-class)}
    (for [[layer-name layer-path] feature-layer-minimap-paths]
      (dom/option #js {:key layer-name
                       :value layer-path} (str layer-name))))
   (dom/select
    #js {:value selected-render-layer-path
         :onChange (select-render-layer this port x y ui-process-class)}
    (for [[layer-name layer-path] feature-layer-render-paths]
      (dom/option #js {:key layer-name
                       :value layer-path} (str layer-name))))
   (ui-draw-sizes this local-state render-size minimap-size)
   (dom/canvas
    #js {:ref "process-feed-minimap"
         :width (or (:x draw-size-minimap)
                    (* (:x minimap-size) 2))
         :height (or (:y draw-size-minimap)
                     (* (:y minimap-size) 2))
         :onMouseUp (minimap-mouse-up this port (or draw-size-minimap (-> minimap-size
                                                                          (update :x #(* 2 %))
                                                                          (update :y #(* 2 %))))
                                      minimap-size)})
   (dom/canvas
    #js {:ref "process-feed"
         :width (or (:x draw-size) (* (:x render-size) 2))
         :height (or (:y draw-size) (* (:y render-size) 2))
         :onMouseDown (fn [evt]
                        (let [coords (event->dom-coords
                                      evt
                                      (dom/node this "process-feed"))]
                          (prim/set-state!
                           this
                           (merge (prim/get-state this)
                                  {:selection {:start coords}}))))
         :onMouseMove (screen-mouse-move this)
         :onMouseUp (screen-mouse-up this port (or draw-size (-> render-size
                                                                 (update :x #(* 2 %))
                                                                 (update :y #(* 2 %))))
                                     render-size
                                     selected-ability)})
   (ui-camera-move-arrows this port x y)))
