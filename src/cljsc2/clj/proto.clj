(ns cljsc2.clj.proto
  (:require
   [instaparse.core :as insta]
   [hara.string.case :as casing]
   [clojure.spec.alpha :as spec]
   [environ.core :refer [env]]
   [lucid.mind :refer [.?]]))

(def proto-parser
  (insta/parser (env :proto-grammar)
                :auto-whitespace
                :standard))

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
   "uint32" pos-int?
   "uint64" pos-int?
   "bytes" byte-array?
   "string" string?
   "uint8" pos-int?
   "int8" pos-int?
   "float" float?
   "bool" boolean?})

(def namespaces
  ["common" "debug" "spatial" "query" "sc2api" "data" "error" "raw" "score" "ui"])

(defn try-find-class-cased [st]
  (->> (clojure.string/split st #"(?<=[a-zA-Z])(?=\d)|(?<=\d)(?=[a-zA-Z])")
       (map clojure.string/capitalize)
       (clojure.string/join)))

(defn contains-enum-spec [type-name item-body package file-name class-name]
  (let [found (->>
               item-body
               (filter (comp #{:enum} first))
               (map second)
               (filter second)
               (filter #{type-name})
               first)]
    (when found (keyword (str package "." file-name "$" class-name)
                      type-name))))

(defn replace-ns-with-ns [for-path-key ns]
  (let [split-path (clojure.string/split (namespace for-path-key) #"\.")
        [java-namespace java-class] (clojure.string/split (last split-path) #"\$")
        ]
    (keyword (clojure.string/join "." (concat (drop-last split-path) [ns]))
             (name for-path-key))))

(defn resolve-existing-spec-kw [kw specs namespaces]
  (let [found (get specs (replace-ns-with-ns kw (first namespaces)))]
    (if found
      (replace-ns-with-ns kw (first namespaces))
      (when (not (empty? (rest namespaces)))
        (resolve-existing-spec-kw kw specs (rest namespaces))))))

(defn get-spec
  ([item-type]
   (field-type->spec item-type))
  ([item-type item-body package file-name class-name env]
   (or (get-spec item-type)
       (contains-enum-spec item-type item-body package file-name class-name)
       (resolve-existing-spec-kw
        (keyword (str package "." file-name)
                 item-type)
        env
        namespaces)
       (keyword (str package "." file-name)
                item-type))))

(defn process-message-field [package file-name item-name body-item item-body env]
  (let [optional (some #{"optional"} body-item)
        repeated (some #{"repeated"} body-item)
        field-name (if (or optional repeated)
                     (nth body-item 3)
                     (nth body-item 2))
        item-type (if (or optional repeated)
                    (nth body-item 2)
                    (nth body-item 1))
        item-type (if (vector? item-type)
                    (second item-type)
                    item-type)]
    {(keyword (str package "." file-name "$" item-name)
              (casing/spear-case field-name))
     {:spec `(spec/def ~(keyword (str package "." file-name "$" item-name)
                                 (casing/spear-case field-name))
               ~(if repeated
                  `(spec/coll-of ~(get-spec item-type item-body package file-name item-name env))
                  (get-spec item-type item-body package file-name item-name env)))
      :optional optional
      :repeated repeated}}))

(defn process-message-enum [enum-name package file-name body-item item-name]
  (let [[_ & enum-items] (rest body-item)
        enum-key (keyword (str package "." file-name "$" item-name)
                          enum-name)]
    {enum-key {:spec `(spec/def ~enum-key ~(set (map second enum-items)))}}))

(defn process-message-oneof
  [package file-name item-name body-item]
  (let [one-of-name (second (nth body-item 2))
        one-of-options (drop-last (subvec body-item 4))
        options (reduce (fn [acc option]
                          (let [spec-name (keyword (str package "." file-name "$" item-name)
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
     {(keyword (str package "." file-name "$" item-name)
               (casing/spear-case one-of-name))
      {:spec`(spec/def ~(keyword (str package "." file-name "$" item-name)
                                 (casing/spear-case one-of-name))
               (spec/keys :opt ~(into [] (keys options))))
       :attribute-of-root true}}
     options)))

(defn process-message [item-type item-name item-body package path file-name env]
  (let [message-body-items-specs
        (for [body-item item-body]
          (let [[type message-name & body-items] body-item]
            (case type
              :field (process-message-field package file-name item-name body-item item-body env)
              :enum (process-message-enum message-name package file-name body-item item-name)
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
   {:spec `(spec/def ~(keyword (str package "." file-name) item-name)
            ~(set (map second item-body)))}})

(declare read-protos)

(defn read-proto-item
  [[item-type item-name & item-body] package path file-name env]
  (case item-type
    :import (read-protos path
                         (->> (drop 19 (first item-body))
                              (take-while (fn [[_ item]] (= :letter (first item))))
                              (map (fn [char-or-letter] (second (second char-or-letter))))
                              clojure.string/join)
                         env)
    :message (merge env (process-message item-type item-name item-body package path file-name env))
    :enum (merge env (process-enum item-type item-name item-body package file-name))
    (update-in env [:not-found package file-name item-name] (fn [nf] (conj nf item-type)))))

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
    (reduce (fn [env proto-item]
              (read-proto-item proto-item package-namespace path file-name env))
            env
            proto-items
            )))

(def specs
  (into {}
        (filter
         (fn [[k v]] (:spec v))
         (read-protos (str (env :proto-dir) "/")
                      "sc2api"
                      {}
                      ))))

(defn try-eval-spec [kw k v namespaces]
  (let [for-namespace (first namespaces)]
    (try
      (eval `(do ~(:spec (get specs (replace-ns-with-ns kw for-namespace)))
                 ~(concat (drop-last (:spec v)) [(replace-ns-with-ns kw for-namespace)])))
      (catch Exception e (when (not (empty? (rest namespaces)))
                           (try-eval-spec kw k v (rest namespaces)))))))

(defn replace-last-kw [spec kw]
  (concat (drop-last spec)
          [(concat (drop-last (last spec)) [kw])])) ;;could use walk/zipper

(doall
 (map
  (fn [[k v]]
    (let [kw (nth (:spec v) 2)
          is-kw (keyword? kw)
          coll-of-kw (and (not is-kw)
                          (sequential? kw)
                          (not (= (first kw) 'spec-tools.core/spec))
                          (not (= (nth kw 1) :opt)) (nth kw 1))
          is-coll-of-kw (keyword? coll-of-kw)]
      (try (eval (:spec v))
           (catch Exception e
             (try (eval (if is-kw
                          `(do ~(:spec (get specs kw))
                               ~(:spec v))
                          (let [found-spec-kw (resolve-existing-spec-kw coll-of-kw specs namespaces)]
                            (if is-coll-of-kw
                              `(do ~(:spec (get specs found-spec-kw))
                                   ~(replace-last-kw (:spec v) found-spec-kw))
                              (:spec v)))))
                  ;;try catch hack because no namespace info in .proto
                  (catch Exception e (try-eval-spec kw k v namespaces)))))))
  specs))

(defn str-invoke [instance method-str & args]
  (clojure.lang.Reflector/invokeInstanceMethod
   instance
   method-str
   (to-array args)))

(defn create-builder [for-path-key]
  (let [split-path (clojure.string/split (namespace for-path-key) #"\.")
            [java-namespace java-class] (clojure.string/split (last split-path) #"\$")
            builder-java-string (str "("
                                     (clojure.string/join "." (drop-last split-path))
                                     "."
                                     (str (try-find-class-cased java-namespace) "$" java-class)
                                     "/newBuilder"
                                     ")")]
        (-> builder-java-string
            read-string
            eval)))

(defn builder-for-spec-key-with-attribute [spec-key]
  (let [for-spec (-> (get specs spec-key)
                     :spec
                     last)
        split-path (clojure.string/split (namespace for-spec) #"\.")
        [java-namespace java-class] (clojure.string/split (last split-path) #"\$")
        java-string (str (clojure.string/join "." (drop-last split-path))
                         "."
                         (str (try-find-class-cased java-namespace)
                              "$"
                              ((comp casing/camel-case clojure.string/capitalize)
                               (casing/spear-case (name for-spec))))
                         )]
    (try
      (clojure.lang.Reflector/invokeStaticMethod
       (resolve (symbol java-string))
       "newBuilder"
       (to-array []))
      (catch Error e
        (let [alternative-classname (str (clojure.string/join "." (drop-last split-path))
                                         "."
                                         (str (try-find-class-cased java-namespace)
                                              "$"
                                              (name for-spec))
                                         )]
          (clojure.lang.Reflector/invokeStaticMethod
           (resolve (symbol alternative-classname))
           "newBuilder"
           (to-array [])))))))

(defn hasMember [o member]
  (> (count (.? o member)) 0))

(defn resolve-without-class [spec-key]
  (let [split-path (clojure.string/split (namespace spec-key) #"\.")
        [java-namespace java-class] (clojure.string/split (last split-path) #"\$")
        java-string (str (clojure.string/join "." (drop-last split-path))
                         "."
                         (str (try-find-class-cased java-namespace) "$"
                              ((comp casing/camel-case clojure.string/capitalize)
                               (casing/spear-case (name spec-key))))
                         )]
    (resolve (symbol java-string))))

(defn resolve-with-inner-class [spec-key]
  (let [split-path (clojure.string/split (namespace spec-key) #"\.")
        [java-namespace java-class] (clojure.string/split (last split-path) #"\$")
        java-string (str (clojure.string/join "." (drop-last split-path))
                         "."
                         (str (try-find-class-cased java-namespace) "$"
                              java-class
                              "$"
                              ((comp casing/camel-case clojure.string/capitalize)
                               (casing/spear-case (name spec-key))))
                         )]
    (resolve (symbol java-string))))

(defn class-camel-case-name [kw]
  ((comp casing/camel-case clojure.string/capitalize)
   (casing/spear-case (name kw))))


(defn sub-namespace [spec-key]
  (let [split-path (clojure.string/split (namespace spec-key) #"\.")]
    (last split-path)))

(defn str-invoke-method [method builder spec-key val]
  (try (str-invoke builder
                   (str method (class-camel-case-name spec-key))
                   val)
       ;;try enums
       (catch Exception e
         (try (str-invoke builder
                          (str method (class-camel-case-name spec-key))
                          (clojure.lang.Reflector/invokeStaticMethod
                           (-> (get specs spec-key)
                               :spec
                               last
                               resolve-with-inner-class)
                           "valueOf"
                           (to-array [val])))
              (catch Exception e
                (try (str-invoke builder
                                 (str method (class-camel-case-name spec-key))
                                 (clojure.lang.Reflector/invokeStaticMethod
                                  (-> (get specs spec-key)
                                      :spec
                                      last
                                      resolve-without-class)
                                  "valueOf"
                                  (to-array [val])))
                     (catch Exception e (try (str-invoke builder
                                                         (str method (class-camel-case-name spec-key))
                                                         (clojure.lang.Reflector/invokeStaticMethod
                                                          (resolve-without-class
                                                           (resolve-existing-spec-kw
                                                            (-> (get specs spec-key)
                                                                :spec
                                                                last)
                                                            specs
                                                            (disj (set namespaces)
                                                                  (-> (get specs spec-key)
                                                                      :spec
                                                                      last
                                                                      sub-namespace
                                                                      ))))
                                                          "valueOf"
                                                          (to-array [val])))
                                             (catch Exception e (try (str-invoke builder
                                                                                 (str method (class-camel-case-name spec-key))
                                                                                 (clojure.lang.Reflector/invokeStaticMethod
                                                                                  (-> (get specs spec-key)
                                                                                      :spec
                                                                                      last
                                                                                      last
                                                                                      resolve-without-class)
                                                                                  "valueOf"
                                                                                  (to-array [val])))))))
                     ))))))

(defn find-spec [spec-key]
  (try (-> (get specs spec-key)
           :spec
           last
           last)
       (catch Exception e (try (let [kw (-> (get specs spec-key)
                                            :spec
                                            last
                                            )
                                     is-kw (keyword? kw)]
                                 (if is-kw kw spec-key))))))

(declare ugly-memo-make-protobuf)

(defn ugly-make-protobuf
   "Function to create protobufs from any namespaced object, uses the generated specs of the specs atom. Not nice code, but functional and will revisit later."
   ([spec-obj]
    (ugly-memo-make-protobuf (ffirst spec-obj) spec-obj false))
   ([spec-key spec-obj is-one-of]
    (let [b (if is-one-of
              (try (builder-for-spec-key-with-attribute spec-key)
                   (catch Exception e (create-builder spec-key)))
              (create-builder spec-key))]
      (cond
        (map? spec-obj)
        (doall (map (fn [[child-spec-key spec-val]]
                      (let [setters (.? b (str "set"
                                                (class-camel-case-name child-spec-key)))
                            has-setter (not (empty? setters))]
                        (cond
                          (and (map? spec-val)
                               (empty? spec-val))
                          nil
                          (and (not (map? spec-val))
                               (coll? spec-val))
                          (doall (map (fn [child-spec-obj]
                                        (let [built-val (cond
                                                          (and (not (string? child-spec-obj))
                                                               (map? child-spec-obj)
                                                               (empty? child-spec-obj))
                                                          (let [kw (resolve-existing-spec-kw
                                                                    (find-spec child-spec-key)
                                                                    specs namespaces)
                                                                found-kw (resolve-without-class kw)]
                                                            (.build
                                                             (clojure.lang.Reflector/invokeStaticMethod
                                                              found-kw
                                                              "newBuilder"
                                                              (to-array []))))
                                                          (not (coll? child-spec-obj)) child-spec-obj
                                                          (ffirst child-spec-obj) (ugly-memo-make-protobuf child-spec-obj)
                                                          :else :no-resp)]
                                          (str-invoke-method "add" b child-spec-key built-val)))
                                      spec-val))
                          (and (map? spec-val) (not has-setter))
                          (doall (map (fn [[map-spec-key map-spec-val]]
                                        (let [set-kw (if has-setter
                                                       spec-key
                                                       map-spec-key)]
                                          (if (map? map-spec-val)
                                            (str-invoke-method "set" b set-kw
                                                               (try (ugly-memo-make-protobuf (ffirst map-spec-val) map-spec-val (not has-setter))
                                                                    (catch Exception e (ugly-memo-make-protobuf map-spec-key map-spec-val (not has-setter)))))
                                            (str-invoke-method "set" b set-kw map-spec-val)
                                            )))
                                      spec-val))
                          :else (str-invoke-method "set" b child-spec-key (if (map? spec-val)
                                                                            (ugly-memo-make-protobuf spec-val)
                                                                            spec-val)))))
                    spec-obj))
        :else (str-invoke-method "set" b spec-key spec-obj))
      (.build b))))

(def ugly-memo-make-protobuf (memoize ugly-make-protobuf))
