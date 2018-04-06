(ns cljsc2.cljc.model
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.spec.gen.alpha :as gen]
   #?(:clj cljsc2.clj.proto)
   [fulcro.client.primitives :as om]))

(spec/def ::id int?)

(spec/def ::port (spec/and int?
                           pos?))

(spec/def ::observations (spec/every #?(:clj :SC2APIProtocol.sc2api/Observation
                                       :cljs any?)))

(spec/def ::from (spec/and int?
                           #(>= % 0)))

(spec/def ::till (spec/and int
                           #(>= % 0)))

(spec/def ::run
  (spec/keys :req [::id ::from ::till ::observations]))

(spec/def ::runs (spec/every ::run))

(spec/def ::process
  (spec/keys :req [::id ::port ::runs]))

(spec/def ::processes (spec/every ::process))

(spec/def ::available-maps (spec/coll-of (spec/cat :absolute-path string? :file-name string?)))

(spec/def ::process-starter (spec/keys :req [::available-maps]))
