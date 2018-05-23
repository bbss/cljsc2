(ns cljsc2.cljs.core
  (:require [cognitect.transit :as transit]
            [cljsc2.cljs.colors :refer [discrete-color-palette
                                        hot-palette
                                        player-absolute-colors]]))

(enable-console-print!)

(defonce reader
  (transit/reader
   :json
   {:handlers {"literal-byte-string" (fn [it] it)}}))

(def feature-layer-types
  [:creep
   :height-map
   :unit-shield
   :unit-density-aa
   :player-relative
   :unit-type
   :selected
   :unit-energy
   :unit-density
   :player-id
   :visibility-map
   :power
   :unit-hit-points
   ])

(def feature-layer-draw-descriptions
  {:unit-density-aa (doto (d3.scaleLinear)
                      (.domain #js [0 256])
                      (.range hot-palette))
   :selected (doto (d3.scaleOrdinal)
               (.domain #js [0 1])
               (.range #js ["rgba(0,0,0,0)"
                            "rgba(20,200,20,1)"]))
   :player-relative (doto (d3.scaleOrdinal)
                      (.domain (clj->js (range 1 6)))
                      (.range #js ["rgba(0,0,0,1)"
                                   "rgba(0,142,0, 1)"
                                   "rgba(255, 255, 0, 1)"
                                   "rgba(129, 166, 196, 1)"
                                   "rgba(113,25,34, 1)"
                                   ]))
   :unit-type (doto (d3.scaleOrdinal)
                (.domain (clj->js (range 0 1851)))
                (.range discrete-color-palette))
   :unit-energy (doto (d3.scaleLinear)
                  (.domain #js [0 1000])
                  (.range hot-palette))
   :unit-density (doto (d3.scaleLinear)
                   (.domain #js [0 16])
                   (.range hot-palette))
   :player-id (doto (d3.scaleOrdinal)
                (.domain (clj->js (range 1 17)))
                (.range player-absolute-colors))
   :visibility-map (doto (d3.scaleOrdinal)
                     (.domain (clj->js (range 0 4)))
                     (.range #js ["rgba(0,0,0,1)"
                                  "rgba(255,255,255, 0.25)"
                                  "rgba(255,255,255, 0.6)"]))
   :power (doto (d3.scaleLinear)
            (.domain #js [0 1])
            (.range  #js ["rgba(0,0,0,0)" "blue"]))
   :unit-hit-points (doto (d3.scaleLinear)
                      (.domain #js [0 1600])
                      (.range hot-palette))
   :height-map (doto (d3.scaleLinear)
                 (.domain #js [0 255])
                 (.ticks 400)
                 (.range #js ["rgba(0,0,0,0)" "brown"]))
   :unit-shield (doto (d3.scaleLinear)
                  (.domain #js [0 1000])
                  (.range hot-palette))
   :creep (doto (d3.scaleLinear)
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

(def uint8->binary js/uint8toBinaryString)

(def binary->ab32 js/str2ab32)

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
