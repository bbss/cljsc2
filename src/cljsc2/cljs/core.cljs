(ns cljsc2.cljs.core
  (:require [cognitect.transit :as transit]))

(enable-console-print!)

(defonce canvas (js/document.createElement "canvas"))

(defonce _
  (do
    (set! (.-innerHTML (js/document.querySelector "#app")) "")
    (.appendChild (js/document.querySelector "#app") canvas)))

(defonce reader
  (transit/reader
   :json
   {:handlers {"literal-byte-string" (fn [it] it)}}))

(defn handle-incoming-message [e]
  (let [message (.-data e)]
    (def msg message)))

(defonce es
  (doto (js/EventSource. (str "/sub"))
    (.addEventListener
     "message"
     handle-incoming-message)))

(comment
  (let [img-data []]
    (.putImageData (.getContext canvas "2d")
                   (js/ImageData. img-data, 84, 84))))
