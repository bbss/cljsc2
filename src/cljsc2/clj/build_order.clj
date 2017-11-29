(ns cljsc2.clj.build-order
  (:require
   [cljsc2.clj.core :refer [req]]
   [datascript.core :as ds]
   [taoensso.nippy :as nippy]
   [clojure.spec.alpha :as spec]))

(def abilities
  (->
   (req #:SC2APIProtocol.sc2api$RequestData
        {:data #:SC2APIProtocol.sc2api$RequestData
         {:ability-id true}})
   :data
   :abilities))

(def units
  (->
   (req #:SC2APIProtocol.sc2api$RequestData
        {:data #:SC2APIProtocol.sc2api$RequestData
         {:unit-type-id true}})
   :data
   :units))

(def effects
  (->
   (req #:SC2APIProtocol.sc2api$RequestData
        {:data #:SC2APIProtocol.sc2api$RequestData
         {:effect-id true}})
   :data
   :effects))

(def upgrades
  (->
   (req #:SC2APIProtocol.sc2api$RequestData
        {:data #:SC2APIProtocol.sc2api$RequestData
         {:upgrade-id true}})
   :data
   :upgrades))

(def buffs
  (->
   (req #:SC2APIProtocol.sc2api$RequestData
        {:data #:SC2APIProtocol.sc2api$RequestData
         {:buff-id true}})
   :data
   :buffs))

(defn str-len-distance
  ;; normalized multiplier 0-1
  ;; measures length distance between strings.
  ;; 1 = same length
  [s1 s2]
  (let [c1 (count s1)
        c2 (count s2)
        maxed (max c1 c2)
        mined (min c1 c2)]
    (double (- 1
               (/ (- maxed mined)
                  maxed)))))

(def MAX-STRING-LENGTH 1000.0)

(defn clean-str
  [s]
  (.replaceAll (.toLowerCase s) "[ \\/_]" ""))

(defn score
  [oquery ostr]
  (let [query (clean-str oquery)
        str (clean-str ostr)]
    (loop [q (seq (char-array query))
           s (seq (char-array str))
           mult 1
           idx MAX-STRING-LENGTH
           score 0]
      (cond
       ;; add str-len-distance to score, so strings with matches in same position get sorted by length
       ;; boost score if we have an exact match including punctuation
       (empty? q) (+ score
                     (str-len-distance query str)
                     (if (<= 0 (.indexOf ostr oquery)) MAX-STRING-LENGTH 0))
       (empty? s) 0
       :default (if (= (first q) (first s))
                  (recur (rest q)
                         (rest s)
                         (inc mult) ;; increase the multiplier as more query chars are matched
                         (dec idx) ;; decrease idx so score gets lowered the further into the string we match
                         (+ mult score)) ;; score for this match is current multiplier * idx
                  (recur q
                         (rest s)
                         1 ;; when there is no match, reset multiplier to one
                         (dec idx)
                         score))))))

(comment
  (map (comp :data first
             #(fuzzy-search %
                            units
                            :unit-id 1))
       #{20 27 24 21 22 41 29 6 28 25 23 132 26 30 18 37})

  (fuzzy-search 27 units :unit-id)
  (fuzzy-search "marine" units :friendly-name 10)
  (def mineral-cent-per-second
    (-> (* 100 (/ 500 12 60))
        double
        Math/round
        int))

  (run 4 [q]
    (fresh [minerals seconds minerals-earned workers]
      (== minerals 100)
      (fd/in seconds (fd/interval 0 2))
      (fd/* mineral-cent-per-second seconds minerals-earned)
      (fd/+ minerals-earned minerals q)))

  (run 10 [q]
    (fresh [minerals seconds mined-seconds minerals-earned workers total-minerals]
      (== minerals 10000)
      (fd/in seconds (fd/interval 0 100))
      (fd/in workers (fd/interval 0 80))
      (fd/* workers seconds mined-seconds)
      (fd/* mineral-cent-per-second mined-seconds minerals-earned)
      (fd/+ minerals-earned minerals total-minerals)
      (fd/>= total-minerals 20000)
      (== [seconds workers total-minerals] q)))

  (run 10 [q]
    (fresh [starting-minerals game-seconds mined-seconds minerals-earned workers total-minerals]
      (== starting-minerals 10000)
      (fd/in game-seconds (fd/interval 0 100))
      (fd/in workers (fd/interval 0 80))
      (fd/* workers game-seconds mined-seconds)
      (fd/* mineral-cent-per-second mined-seconds minerals-earned)
      (fd/+ minerals-earned starting-minerals total-minerals)
      (fd/>= total-minerals 20000)
      (== [game-seconds workers total-minerals] q)))


  (fuzzy-search 595 abilities :ability-id))

(defn fuzzy-search
  ([query col get-against]
   (fuzzy-search query col get-against 1))
  ([query col get-against result-count]
   (let [query (clojure.string/lower-case query)]
     (take result-count
           (sort-by :score
                    (comp - compare)
                    (filter #(< 0 (:score %))
                            (for [doc col]
                              {:data doc
                               :score (score query (clojure.string/lower-case (or (get-against doc) "")))})))))))


(def memoized-search (memoize cljsc2.clj.build-order/fuzzy-search))

(defn get-unit-type-by-id [id]
  (:data (first (memoized-search id
                                 cljsc2.clj.build-order/units
                                 :unit-id
                                 1))))

(defn get-unit-type-by-name [id]
  (:data (first (memoized-search id
                                 cljsc2.clj.build-order/units
                                 :name
                                 1))))

(defn get-ability-type-by-id [id]
  (:data (first (memoized-search
                 id
                 cljsc2.clj.build-order/abilities
                 :ability-id
                 1
                 ))))

(defn get-ability-type-by-name [name]
  (:data (first (memoized-search
                 name
                 cljsc2.clj.build-order/abilities
                 :friendly-name
                 1
                 ))))

(defn get-ability-types-by-name [name]
  (map :data (memoized-search
                 name
                 cljsc2.clj.build-order/abilities
                 :friendly-name
                 10
                 )))

(defn get-unit-types-by-name [name]
  (map :data (memoized-search
              name
              cljsc2.clj.build-order/units
              :name
              10
              )))

(comment
  (i-have command-center 8 scv)

  (i-want scv)

  (i-earn tasked-scvs seconds)

  (i-spent creator-queues)

  (spec/def ::amount int?)
  (spec/def ::unit-id int?)
  (spec/def ::units-string (spec/coll-of (spec/cat ::amount ::unit-id)))

  (spec/explain-data ::units-string
                     (->>
                      (->
                       "2 marines and 3 marauders"
                       (clojure.string/replace #" and" "")
                       (clojure.string/split #" "))
                      (partition 2)
                      (map (fn [[amount unit-name]]
                             (let [stripped-name (if (clojure.string/ends-with? unit-name "s")
                                                   (subs unit-name 0 (dec (count unit-name)))
                                                   unit-name)]
                               [(Integer/parseInt "1") (get-unit-type-by-name "marine")]
                               )))
                      flatten)))
