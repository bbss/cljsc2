(ns cljsc2.clj.rendering
  (:require
   [manifold.stream :as s :refer [stream]]
   [clojure.data :refer [diff]]
   ;[clojupyter.misc.display :as display]
   [clojure.spec.alpha :as spec]
   [telegenic.core :refer [encode]]
   [byte-transforms :as byte-tf]
   [byte-streams :as byte-streams])
  (:import
   java.awt.image.BufferedImage
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
    bi))


(defn observations->mp4
  ([unique observations]
   (let [filename (str unique " - " (System/currentTimeMillis) ".mp4")]
     (encode
      (->> observations
           (map (comp render-observation->buffered-image :map :render-data)))
      {:filename filename})
     {:filename filename})))

(defn to-base64 [file-path]
  (byte-streams/to-string
   (byte-tf/encode
    (byte-streams/to-byte-array (clojure.java.io/File. file-path))
    :base64 {:url-safe? false})))

;(defn run-result->mp4-file-path
;  ([run-result]
;   (run-result->mp4-file-path run-result 5000))
;  ([run-result port]
;   (->> run-result
;        first
;        persistent!
;        ((partial observations->mp4 port))
;        :filename)))
;
;(defn mp4-file-path->markdown-html [file-path]
;  (display/make-markdown (str "
;<video
;autoplay
;loop
;src=\"" file-path "\"
;controls></video>")))
