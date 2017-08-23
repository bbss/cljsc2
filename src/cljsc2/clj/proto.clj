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
   "string" string?})

(comment((defn messages [proto]
           (map (fn [[proto-item & body]]
                  (case proto-item
                    :message (let [[message-name & body-items] body]
                               [(keyword (str spec-namespace "." sub-namespace) message-name)
                                (for [item body-items]
                                  (case (first item)
                                    :field (let [optional (some #{"optional"} item)
                                                 item-name (if optional
                                                             (nth item 3)
                                                             (nth item 2))
                                                 item-type (if optional
                                                             (nth item 2)
                                                             (nth item 1))
                                                 item-type (if (vector? item-type)
                                                             (second item-type)
                                                             item-type)]
                                             {:qualified-name (keyword (str spec-namespace "." sub-namespace)
                                                                       (casing/spear-case item-name))
                                              :optional optional
                                              :name item-name
                                              :type item-type}
                                             )
                                    :enum (let [enum-name (second item)
                                                [_ & enum-items] (rest item)]
                                            {:qualified-name (keyword (str spec-namespace "." sub-namespace)
                                                                      (casing/spear-case enum-name))
                                             :name enum-name
                                             :spec (set (map second enum-items))
                                             :exclude-from-keys-spec true
                                             })))])
                    ))
                (->> proto
                     rest
                     (filter (comp #{:message} first))
                     ((fn [coll] (subvec (vec coll) 4 5))))))
         sc2api-proto))

(def generated-specs-registry (atom {}))

((fn [for-spec]
              (let [spec-ns (namespace for-spec)
                    spec-name (name for-spec)
                    split-ns (clojure.string/split spec-ns #"\.")]
                (->
                 (str "("
                      (clojure.string/join "."
                                           (update split-ns
                                                   (dec (count split-ns))
                                                   try-find-class-name))
                      "$"
                      spec-name
                      "/newBuilder)")
                 read-string
                 eval)))
 :SC2APIProtocol.sc2api/ResponseCreateGame)

(defn try-find-class-name [st]
  (->> (clojure.string/split st #"(?<=[a-zA-Z])(?=\d)|(?<=\d)(?=[a-zA-Z])")
       (map clojure.string/capitalize)
       (clojure.string/join)))
(reset! generated-specs-registry {})
(do (defn read-proto-item
      [[item-type item-name & item-body] package path file-name generated-specs-registry]
      (case item-type
        :import (comment(read-protos path
                                     (->> (drop 2 item-body)
                                          (take-while (fn [[_ item]] (= :letter (first item))))
                                          (map (fn [char-or-letter] (second (second char-or-letter))))
                                          clojure.string/join)
                                     generated-specs-registry))
        :message (let [message-body-items-specs
                       (for [body-item item-body]
                         (let [[type message-name & body-items] body-item]
                           (case type
                             :field (let [optional (some #{"optional" "repeated"} body-item)
                                          item-name (if optional
                                                      (nth body-item 3)
                                                      (nth body-item 2))
                                          item-type (if optional
                                                      (nth body-item 2)
                                                      (nth body-item 1))
                                          item-type (if (vector? item-type)
                                                      (second item-type)
                                                      item-type)]
                                      (swap! generated-specs-registry
                                             assoc
                                             (keyword (str package "." file-name)
                                                      (casing/spear-case item-name))
                                             `(spec/def ~(keyword (str package "." file-name)
                                                                  (casing/spear-case item-name))
                                                ~(or (field-type->spec item-type)
                                                     (keyword (str package "." file-name)
                                                              ((comp casing/camel-case clojure.string/capitalize) item-name))
                                                     )))
                                      {(keyword (str package "." file-name)
                                                (casing/spear-case item-name))
                                       `(spec/def ~(keyword (str package "." file-name)
                                                            item-name)
                                           ~(or (field-type->spec item-type)
                                               (keyword (str package "." file-name)
                                                        ((comp casing/camel-case clojure.string/capitalize) item-name))
                                               ))})
                             :enum (let [enum-name message-name
                                         [_ & enum-items] (rest body-item)
                                         enum-key (keyword (str package "." file-name)
                                                           (casing/spear-case enum-name))]
                                     (swap! generated-specs-registry
                                            assoc
                                            enum-key `(spec/def ~enum-key ~(set (map second enum-items))))
                                     {})
                             :oneof (let [one-of-name (second (nth body-item 2))
                                          one-of-options (drop-last (subvec body-item 4))
                                          options (reduce (fn [acc option]
                                                            (let [spec-name (keyword (str package "." file-name)
                                                                                     (casing/spear-case (second
                                                                                                         (nth option 2))))
                                                                  spec-type (second (second option))]
                                                              (assoc acc
                                                                     spec-name
                                                                     `(spec/def ~spec-name :req ~[(keyword (str package "." file-name)
                                                                                                           spec-type)]))))
                                                          {}
                                                          one-of-options)]
                                      (doall (map (fn [[spec-key spec]]
                                                    (swap! generated-specs-registry assoc spec-key spec))
                                                  options))
                                      (swap! generated-specs-registry
                                             assoc
                                             (keyword (str package "." file-name)
                                                      (casing/spear-case one-of-name))
                                             `(spec/def ~(keyword (str package "." file-name)
                                                                  (casing/spear-case one-of-name))
                                                (spec/keys :opt ~(into [] (keys options)))))
                                      {(keyword (str package "." file-name)
                                                (casing/spear-case one-of-name))
                                       `(spec/def ~(keyword (str package "." file-name)
                                                            (casing/spear-case one-of-name))
                                          (spec/keys :opt ~(into [] (keys options))))})
                             :item-body-not-recognized)))
                       body-items-specs-map (reduce merge {} message-body-items-specs)
                       ]
                   (swap!
                    generated-specs-registry
                    assoc
                    (keyword (str package "." file-name)
                             item-name)
                    `(spec/def ~(keyword (str package "." file-name)
                                                                                                  item-name)
                                                                                (spec/keys :req ~(keys body-items-specs-map)))))

        :none))
    ((defn read-protos [path file-name generated-specs-registry]
       (let [parsed-proto (-> (str path file-name ".proto")
                              slurp
                              remove-comments
                              proto-parser)
             proto-items (rest parsed-proto)
             package-namespace (->> proto-items
                                    (filter (comp #{:package} first))
                                    first
                                    ((fn [pkg] (nth pkg 2))))]
         (for [proto-item (subvec (vec (filter (comp #{:message} first) proto-items)) 1 2)]
           (read-proto-item proto-item package-namespace path file-name generated-specs-registry))))
     "/Users/baruchberger/stah/cljsc2/resources/proto/"
     "sc2api"
     generated-specs-registry))

(let [] @generated-specs-registry)

(spec/def :SC2APIProtocol.sc2api/Race
  #{"terran" "protoss" "zerg" "random"})

(spec/def :SC2APIProtocol.sc2api/race
  :SC2APIProtocol.sc2api/Race)

(spec/def :SC2APIProtocol.sc2api/RequestCreateGame
  (spec/keys :req [:SC2APIProtocol.sc2api/race]))

(spec/def :SC2APIProtocol.sc2api/create-game
  :SC2APIProtocol.sc2api/RequestCreateGame)

(spec/def :SC2APIProtocol.sc2api/request
  (spec/keys :opt [:SC2APIProtocol.sc2api/create-game]))

(spec/def :SC2APIProtocol.sc2api/Request
  (spec/keys :req [:SC2APIProtocol.sc2api/request]))

(first (gen/sample (spec/gen :SC2APIProtocol.sc2api/Request)))
;; => #:SC2APIProtocol.sc2api{:request #:SC2APIProtocol.sc2api{:create-game #:SC2APIProtocol.sc2api{:race "zerg"}}}

(comment
  (defn read-protos ["/path/" "sc2api" generated-specs-registry]
    (proto-parser (remove-comments (slurp "/Users/baruchberger/stah/cljsc2/resources/proto/sc2api.proto")))
    (for [form forms]
      {:import (recur path import spec-atom)
       :message
       {:field spec-atom spec keys-of-field
        :enum spec-atom spec keys-of-enum}}))
  (defn create-specs [generated-specs-registry]
    eval within its NSs)
  (doall (map (fn [spec] (eval spec))
              (vals @generated-specs-registry)))
 ;;TODO inject generators here
  (gen-or-write :specname/request) => {}
  (defn create-builder [spec obj]
    (conform for class)))

((fn [for-spec]
   (let [spec-ns (namespace for-spec)
         spec-name (name for-spec)
         split-ns (clojure.string/split spec-ns #"\.")]
     (->
      (str "("
           (clojure.string/join "."
                                (update split-ns
                                        (dec (count split-ns))
                                        try-find-class-name))
           "$"
           spec-name
           "/newBuilder)")
      read-string
      eval)))
 :SC2APIProtocol.sc2api/ResponseCreateGame)

(def rcg (first (filter (complement empty?) (gen/sample (spec/gen :SC2APIProtocol.sc2api/ResponseCreateGame)))))

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
   eval))

((doto-form rcg) (SC2APIProtocol.Sc2Api$ResponseCreateGame/newBuilder))

(.setError (SC2APIProtocol.Sc2Api$ResponseCreateGame/newBuilder)
           (.? (SC2APIProtocol.Sc2Api$ResponseCreateGame$Error/valueOf "MissingMap")))
;;turn message-body-item into Class ValueOf multimethod

(doall
 (map (fn [[message-name fields]]
        (swap! generated-specs-registry
               assoc
               message-name
               `(spec/def ~message-name
                  (spec/keys :req ~(->> fields
                                           (filter (complement :optional))
                                           (filter (complement :exclude-from-keys-spec))
                                           (map :qualified-name)
                                           (into []))
                             :opt ~(->> fields
                                           (filter :optional)
                                           (filter (complement :exclude-from-keys-spec))
                                           (map :qualified-name)
                                           (into [])))))
        (doall (for [{:keys [qualified-name type name map-path spec]} fields]
                 (let [sp (get field-type->spec type spec)]
                   (when sp (swap! generated-specs-registry
                                   assoc
                                   qualified-name
                                   `(spec/def ~qualified-name ~sp)))))))
      (messages sc2api-proto)))
