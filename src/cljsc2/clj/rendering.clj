(ns cljsc2.clj.rendering
  (:require
   [cljsc2.clj.core :as core]
   [manifold.stream :as s :refer [stream]]
   [clojure.data :refer [diff]]
   [datascript.core :as ds]
   [clojure.spec.alpha :as spec]
   [telegenic.core :refer [encode]]
   [byte-transforms :as byte-tf]
   )
  (:import java.awt.image.BufferedImage
           java.awt.image.DataBuffer
           java.awt.image.Raster
           java.io.File))

(defn render-observation->buffered-image [{:keys [data bits-per-pixel size]}]
  (let [as-byte-array (byte-array data)
        data-buf-byte (java.awt.image.DataBufferByte. as-byte-array (count as-byte-array))
        bi (java.awt.image.BufferedImage. (:x size) (:y size) java.awt.image.BufferedImage/TYPE_INT_RGB)
        raster (java.awt.image.Raster/createInterleavedRaster data-buf-byte
                                                              (:x size)
                                                              (:y size)
                                                              (* 3 (:x size))
                                                              3
                                                              (int-array [0 1 2])
                                                              nil)]
    (.setData bi raster)
    bi
    ))

(byte-tf/encode )

(defn observations->mp4
  ([observations port]
   (let [filename]
     (encode
      (->> observations
           (map (comp render-observation->buffered-image :map :render-data)))
      {:filename filename}))))
