(ns cljsc2.clj.web
  (:require [yada.yada :as yada :refer [yada]]
            [manifold
             [stream :as s :refer [stream connect]]
             [bus :refer [event-bus publish! subscribe]]]
            [cognitect.transit :as transit])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(def to-client (s/stream* {:permanent? true}))

(defn web-step-fn [obs connection]
  (let [out (ByteArrayOutputStream. 4096)]
    (transit/write
     (transit/writer
      out
      :json
      {:handlers
       {com.google.protobuf.ByteString$LiteralByteString
        (transit/write-handler
         "literal-byte-string"
         (fn [bs] (let [buff (ByteArrayOutputStream. 4096)]
                   (.writeTo bs buff)
                   (.toByteArray buff))))}})
     (->> obs
          :observation
          :observation
          :feature-layer-data))
    out))

(def stepper
  {:step-fn web-step-fn
   :throttle-max-per-second 2
   :downstream? false
   :description "steps -> client"
   :sink to-client})

(defn sub-resource []
  (yada/resource
   {:methods
    {:get
     {:produces "text/event-stream"
      :consumes #{"application/transit+json"
                  "application/json"}
      :response (fn [req] to-client)}}}))

(defn routes []
  [""
   [["/sub" (sub-resource)]
    ["/build" [["/"
                (yada/handler (new java.io.File "resources/public/build"))]]]
    ["/js" [["/" (yada/handler (new java.io.File "resources/public/js"))]]]
    ["/css" [["/" (yada/handler (new java.io.File "resources/public/css"))]]]
    ["/favicon.ico" (-> (yada (clojure.java.io/file "resources/public/favicon.ico"))
                        (assoc :id ::index))]
    ["/" (-> (yada (clojure.java.io/file "resources/public/index.html"))
             (assoc :id ::index))]]])

(def server
  (yada/listener
   (routes)
   {:port 3000}))
