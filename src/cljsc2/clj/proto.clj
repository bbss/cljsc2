(ns cljsc2.clj.proto
  (:require [instaparse.core :as insta]
            [hara.string.case :as casing]
            [clojure.spec.alpha :as spec]
            [clojure.spec.gen.alpha :as gen]
            [clojure.test.check]))

(def proto-parser (insta/parser "/Users/baruchberger/stah/cljsc2/resources/proto.ebnf" :auto-whitespace :standard))

(defn remove-comments [string]
  (clojure.string/join
   "\n"
   (map
    (fn [remove-comment-from-me]
      (if-let [i (clojure.string/index-of remove-comment-from-me "//")]
        (subs remove-comment-from-me 0 i)
        remove-comment-from-me))
    (clojure.string/split-lines string))))

(defn test-array
  [t]
  (let [check (type (t []))]
    (fn [arg] (instance? check arg))))

(def byte-array?
  (test-array byte-array))

(def field-type->spec
  {"int32" pos-int?
   "bytes" byte-array?
   "string" string?
   "uint32" pos-int?})

(defn try-find-class-name [st]
  (->> (clojure.string/split st #"(?<=[a-zA-Z])(?=\d)|(?<=\d)(?=[a-zA-Z])")
       (map clojure.string/capitalize)
       (clojure.string/join)))

(defn process-message-field [package file-name item-name body-item]
  (let [optional (some #{"optional"} body-item)
        repeated (some #{"repeated"} body-item)
        one-of-item-name (if (or optional repeated)
                           (nth body-item 3)
                           (nth body-item 2))
        item-type (if (or optional repeated)
                    (nth body-item 2)
                    (nth body-item 1))
        item-type (if (vector? item-type)
                    (second item-type)
                    item-type)]
    {(keyword (str package "." file-name "." item-name)
              (casing/spear-case one-of-item-name))
     {:spec `(spec/def ~(keyword (str package "." file-name "." item-name)
                                 (casing/spear-case one-of-item-name))
               ~(if repeated
                  `(spec/coll-of ~(or (field-type->spec item-type)
                                      (keyword (str package "." file-name)
                                               ((comp casing/camel-case clojure.string/capitalize) one-of-item-name))
                                      ))
                  (or (field-type->spec item-type)
                      (keyword (str package "." file-name)
                               ((comp casing/camel-case clojure.string/capitalize) one-of-item-name))
                      )))
      :optional optional
      :repeated repeated}}))

(defn process-message-enum [enum-name package file-name body-item]
  (let [[_ & enum-items] (rest body-item)
        enum-key (keyword (str package "." file-name)
                          (casing/spear-case enum-name))]
    {enum-key {:spec `(spec/def ~enum-key ~(set (map second enum-items)))}}))

(defn process-message-oneof
  [package file-name item-name body-item]
  (let [one-of-name (second (nth body-item 2))
        one-of-options (drop-last (subvec body-item 4))
        options (reduce (fn [acc option]
                          (let [spec-name (keyword (str package "." file-name "." item-name)
                                                   (casing/spear-case (second
                                                                       (nth option 2))))
                                spec-type (if (vector? (second option))
                                            (second (second option))
                                            (second option))]
                            (assoc acc
                                   spec-name
                                   {:spec `(spec/def ~spec-name ~(or (field-type->spec spec-type)
                                                                     (keyword (str package "." file-name)
                                                                              spec-type)))})))
                        {}
                        one-of-options)]
    (merge
     {(keyword (str package "." file-name "." item-name)
               (casing/spear-case one-of-name))
      {:spec`(spec/def ~(keyword (str package "." file-name "." item-name)
                                 (casing/spear-case one-of-name))
               (spec/keys :opt ~(into [] (keys options))))
       :attribute-of-root true}}
     options)))

(defn process-message [item-type item-name item-body package path file-name]
  (let [message-body-items-specs
        (for [body-item item-body]
          (let [[type message-name & body-items] body-item]
            (case type
              :field (process-message-field package file-name item-name body-item)
              :enum (process-message-enum message-name package file-name body-item)
              :oneof (process-message-oneof package file-name item-name body-item)
              :item-body-not-recognized)))
        body-items-specs-map (reduce merge {} message-body-items-specs)]
    (merge {(keyword (str package "." file-name)
                     item-name)
            {:spec `(spec/def ~(keyword (str package "." file-name)
                                        item-name)
                      ~(concat `(spec/keys )
                               `[:opt ~(->> body-items-specs-map
                                            (filter (fn [[spec-key spec-obj]]
                                                      (and (contains? spec-obj :optional)
                                                           (or (:optional spec-obj)
                                                               (:repeated spec-obj)))))
                                            (map first))
                                 :req ~(->> body-items-specs-map
                                            (filter (fn [[spec-key spec-obj]]
                                                      (:attribute-of-root spec-obj)))
                                            (map first))]))}}
           body-items-specs-map)))

(defn process-enum [item-type item-name item-body package file-name]
  {(keyword (str package "." file-name) item-name)
   `(spec/def ~(keyword (str package "." file-name) item-name)
      ~(set (map second item-body)))})

(defn read-proto-item
  [[item-type item-name & item-body] package path file-name env]
  (case item-type
    :import (comment
              (read-protos path
                           (->> (drop 2 (first item-body))
                                (take-while (fn [[_ item]] (= :letter (first item))))
                                (map (fn [char-or-letter] (second (second char-or-letter))))
                                clojure.string/join)
                           env))
    :message (merge env (process-message item-type item-name item-body package path file-name))
    :enum (merge env (process-enum item-type item-name item-body package file-name))
    (update-in env [:not-found package file-name item-name] (fn [nf] (conj nf item-type)))))

(read-protos "/Users/baruchberger/stah/cljsc2/resources/proto/"
                     "sc2api"
                     {})
(gen/sample (spec/gen :SC2APIProtocol.sc2api/Request))
(defn read-protos [path file-name env]
  (let [parsed-proto (-> (str path file-name ".proto")
                         slurp
                         remove-comments
                         proto-parser)
        proto-items (rest parsed-proto)
        package-namespace (->> proto-items
                               (filter (comp #{:package} first))
                               first
                               ((fn [pkg] (nth pkg 2))))]
    ;;(for [proto-item (subvec (vec (filter (comp #{:message} first) proto-items)) 0 10)]
    (reduce (fn [env proto-item]
              (read-proto-item proto-item package-namespace path file-name env))
            env
            proto-items
            )))

(defn doto-form [spec-def]
  "Creates a function that maps keynames to java methods for the specs builders. Needs to be partially applied because directly passing complex java obj to eval didn't work"
  (->>
   (str
    "(partial (fn [new-builder] (doto new-builder"
    (clojure.string/join
     (map (fn [[clss value]]
            (str "(.set"
                 ((comp casing/camel-case clojure.string/capitalize name) clss)
                 " \""
                 value
                 "\")"))
          spec-def))
    ")))")
   read-string
   ))

(comment(doto-form #:SC2APIProtocol.sc2api.Request{:request #:SC2APIProtocol.sc2api.Request{:step #:SC2APIProtocol.sc2api.RequestStep{:count 10}}})
        (use 'lucid.mind))
