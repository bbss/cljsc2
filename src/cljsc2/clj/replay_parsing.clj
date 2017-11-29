(ns replay-parsing
  (:require [cljsc2.clj.core :as core]
            [manifold.stream :as s :refer [stream]]
            [clojure.data :refer [diff]]
            [taoensso.nippy :as nippy]
            [datascript.core :as ds]
            [clojure.spec.alpha :as spec]))

(->>
 (->
  "2 marines 3 marauders"
  (clojure.string/replace #" and" #"")
  (clojure.string/split #" ")
  )
 (partition 2)
 (map (fn [[amount unit-name]]
        (let [stripped-name (if (clojure.string/ends-with? unit-name "s")
                              (subs unit-name 0 (dec (count unit-name)))
                              unit-name)]
          [(Integer/parseInt "1") (get-unit-type-by-name stripped-name)]
          ))))

(def db
  (ds/create-conn
   {:unit/type {:db/cardinality :db.cardinality/many
                :db/valueType :db.type/ref}
    :unit-type/abilities {:db/cardinality :db.cardinality/many
                          :db/valueType :db.type/ref}
    }))

(count @db)

(def transactions (transient #{}))

(def result (persistent! transactions))

(def ts
  (ds/transact! db
                (->>
                 (map (fn [unit]
                        (let [{:keys [unit-type tag orders]} unit
                              {:keys [mineral-cost vespene-cost food-required food-provided
                                      health-max build-time ability-id name] :as unit-type-data}
                              (get-unit-type-by-id unit-type)]
                          (flatten (concat
                                    [{:db/id tag
                                      :unit/name (:name unit-type-data)
                                      :unit/type (+ 880000 unit-type)}
                                     (doall (into {}
                                                  (filter
                                                   (fn [[_ v]] (not (nil? v)))
                                                   {:db/id (+ 880000 unit-type)
                                                    :unit-type/health-max health-max
                                                    :unit-type/mineral-cost mineral-cost
                                                    :unit-type/vespene-cost vespene-cost
                                                    :unit-type/food-provided food-provided
                                                    :unit-type/name name
                                                    :unit-type/food-required food-required
                                                    :unit-type/build-time build-time
                                                    :unit-type/built-by-ability (when ability-id
                                                                                  (+ 990000 ability-id))})))]
                                    (map (fn [order]
                                           (let [ability (get-ability-type-by-id (:ability-id order))]
                                             [{:db/id (+ 990000 (:ability-id order))
                                               :ability/name (:friendly-name ability)}
                                              {:db/id (+ 880000 unit-type)
                                               :unit-type/abilities (+ 990000 (:ability-id order))}]))
                                         orders)
                                    ))))
                      (take 1000000 (drop 0 result)))
                 flatten
                 set
                 (into []))))

(ds/q '[:find ?name ?t-name
        :where
        [880018 :unit-type/build-time]
        [880018 :unit-type/name ?t-name]
        [?id :unit/name ?name]
        [?id :unit/type 880018]
        ]
      @db)

(ds/q '[:find ?builder-name ?built-name ?ability-name ?ability-id
        :where
        [?builder-id :unit-type/abilities ?ability-id]
        [?builder-id :unit-type/name ?builder-name]
        [?built-id :unit-type/built-by-ability ?ability-id]
        [?built-id :unit-type/name ?built-name]
        [?ability-id :ability/name ?ability-name]
        ]
      @cljsc2.clj.build-order/db)
