(ns cljsc2.clj.web
  (:require [yada.yada :as yada :refer [yada]]
            [manifold
             [stream :as s :refer [stream connect]]
             [bus :refer [event-bus publish! subscribe]]]
            [cognitect.transit :as transit])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))


;;make webserver
;;set-up websockets connection to front
;;set-up transit serialization and send feature layer binary string
;;draw binary string to canvas
;;set-up loop to redraw on step

(def to-client (stream))

(defn sub-resource []
  (yada/resource
   {:methods
    {:get
     {:produces "text/event-stream"
      :consumes #{"application/transit+json"
                  "application/json"}
      :response (fn [req]
                  to-client
                  )}}}))
(->> cljsc2.clj.core/obs
     :observation
     :observation
     :feature-layer-data
     :renders
     :selected
     )

(transit/write
 writer
 (->> cljsc2.clj.core/obs
      :observation
      :observation
      :feature-layer-data
      :renders
      :selected
      :data))

(s/put!
 to-client
 out)

(def out (ByteArrayOutputStream. 16384))

(def writer
  (transit/writer
   out
   :json
   {:handlers
    {com.google.protobuf.ByteString$LiteralByteString
     (transit/write-handler
      "literal-byte-string"
      (fn [bs] (let [buff (ByteArrayOutputStream. 4096)]
                 (.writeTo bs buff)
                 (.toByteArray buff))))}}))

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


(defonce server
  (yada/listener
   (routes)
   {:port 3000}))

(comment
  (defn sub-resource []
  (yada/resource
   {:methods
    {:get
     {:produces "text/event-stream"
      :consumes #{"application/transit+json"
                  "application/json"}
      :response (fn [req]
                  (let [db-id (get-in req [:parameters :query "db-id"])
                        db-bus (get-in @state
                                       [:db/by-id db-id :bus])
                        db (get-in @state
                                   [:db/by-id db-id :db])
                        to-client (stream)
                        sub (manifold.bus/subscribe db-bus "transaction")]
                    (s/connect sub to-client
                               {:timeout 1e5})
                    (manifold.stream/put! to-client {:type :schema
                                                     :schema (:schema @db)
                                                     :db-id db-id})
                    (manifold.stream/put! to-client {:type :transaction
                                                     :db-id db-id
                                                     :transaction (d/datoms @db :eavt)})
                    (manifold.stream/map #(str "data: " % "\n\n") to-client))
                  )}}})))
