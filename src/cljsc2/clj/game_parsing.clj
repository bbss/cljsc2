(ns cljsc2.clj.game-parsing
  (:require [cljsc2.clj.core :as core]
            [manifold.stream :as s :refer [stream]]
            [clojure.data :refer [diff]]
            [datascript.core :as ds]
            [datascript.transit :refer [read-transit-str write-transit-str]]
            [taoensso.nippy :as nippy]
            [clojure.spec.alpha :as spec]
            ))

(def schema
  {:unit/type {:db/cardinality :db.cardinality/many
               :db/valueType :db.type/ref}
   :unit/buff-ids {:db/cardinality :db.cardinality/many
                   :db/valueType :db.type/ref}
   :unit/orders {:db/cardinality :db.cardinality/many
                 :db/valueType :db.type/ref}
   :order/ability-id {:db/cardinality :db.cardinality/many
                      :db/valueType :db.type/ref}
   :order/target-unit-tag {:db/cardinality :db.cardinality/many
                           :db/valueType :db.type/ref}
   :unit-type/abilities {:db/cardinality :db.cardinality/many
                         :db/valueType :db.type/ref}
   :upgrade-type/ability-id {:db/cardinality :db.cardinality/many
                             :db/valueType :db.type/ref}})

(def knowledge-base
  (read-transit-str
   (nippy/thaw-from-file "resources/static-terran-transit-str.nippy")))

(def upgrade-keymap
  {:upgrade-id :upgrade-type/id
   :name :upgrade-type/name
   :ability-id :upgrade-type/ability-id
   :research-time :upgrade-type/research-time
   :mineral-cost :upgrade-type/mineral-cost
   :vespene-cost :upgrade-type/vespene-cost})

(def order-type-keymap
  {:ability-id :order/ability-id
   :progress :order/progress
   :target-unit-tag :order/target-unit-tag
   :target-world-space-pos :order/target-world-space-pos})

(def unit-type-keymap
  {:movement-speed :unit-type/movement-speed
   :weapons :unit-type/weapons
   :food-provided :unit-type/food-provided
   :race :unit-type/race
   :name :unit-type/name
   :ability-id :unit-type/ability-id
   :build-time :unit-type/build-time
   :unit-id :unit-type/unit-id
   :sight-range :unit-type/sight-range
   :tech-alias :unit-type/tech-alias
   :armor :unit-type/armor
   :available :unit-type/available
   :attributes :unit-type/attributes
   :mineral-cost :unit-type/mineral-cost
   :vespene-cost :unit-type/vespene-cost})

(def ability-type-keymap
  {:allow-minimap :ability-type/allow-minimap
   :is-instant-placement :ability-type/is-instant-placement
   :is-building :ability-type/is-building
   :ability-id :ability-type/id
   :allow-autocast :ability-type/allow-autocast
   :footprint-radius :ability-type/footprint-radius
   :remaps-to-ability-id :ability-type/remaps-to-ability-id
   :available :ability-type/available
   :target :ability-type/target
   :friendly-name :ability-type/name
   :cast-range :ability-type/cast-range})

(def unit-keymap
  {:add-on-tag :unit/add-on-tag
   :orders :unit/orders
   :passengers :unit/passengers
   :is-blip :unit/is-blip
   :assigned-harvesters :unit/assigned-harvesters
   :unit-type :unit/unit-type
   :shield :unit/shield
   :is-selected :unit/is-selected
   :health-max :unit/health-max
   :weapon-cooldown :unit/weapon-cooldown
   :facing :unit/facing
   :is-powered :unit/is-powered
   :is-on-screen :unit/is-on-screen
   :pos :unit/pos
   :ideal-harvesters :unit/ideal-harvesters
   :energy :unit/energy
   :radius :unit/radius
   :build-progress :unit/build-progress
   :cloak :unit/cloak
   :cargo-space-taken :unit/cargo-space-taken
   :alliance :unit/alliance
   :detect-range :unit/detect-range
   :is-burrowed :unit/is-burrowed
   :display-type :unit/display-type
   :vespene-contents :unit/vespene-contents
   :energy-max :unit/energy-max
   :radar-range :unit/radar-range
   :health :unit/health
   :cargo-space-max :unit/cargo-space-max
   :tag :unit/tag
   :is-flying :unit/is-flying
   :shield-max :unit/shield-max
   :owner :unit/owner
   :mineral-contents :unit/mineral-contents})

(def knowledge-layout
  {:datascript-schema schema
   :ability-type-attributes (vals ability-type-keymap)
   :unit-type-attributes (vals unit-type-keymap)
   :unit-attributes (vals unit-keymap)
   :upgrade-type-attributes (vals upgrade-keymap)
   :order-attributes (vals order-type-keymap)})

(defn obs->facts
  ([{:keys [minerals vespene food-cap food-used food-workers
            idle-worker-count army-count player-id food-cap] :as raw-data}]
   (obs->facts raw-data {}))
  ([{:keys [game-loop player-common raw-data]} {{:keys [start-raw]} :game-info}]
   (let [{:keys [minerals vespene food-cap food-used food-workers
                 idle-worker-count army-count player-id food-cap]} player-common
         game-loop-id (+ 1234000000 game-loop)]
     (concat [{:db/id game-loop-id
               :player-common/minerals minerals
               :player-common/vespene vespene
               :player-common/food-used food-used
               :player-common/food-cap food-cap
               :player-common/food-workers food-workers
               :player-common/idle-worker-count idle-worker-count
               :player-common/army-count army-count}
              {:db/id -1 :meta/latest-game-loop game-loop}
              {:db/id -2 :meta/player-id player-id}]
             (map
              (fn [unit]
                (let [namespaced-unit (merge (clojure.set/rename-keys
                                              (select-keys
                                               unit
                                               (vals unit-keymap))
                                              unit-keymap)
                                             {:db/id (:tag unit)
                                              :unit/x (:x (:pos unit))
                                              :unit/y (:y (:pos unit))
                                              :unit/z (:z (:pos unit))
                                              :unit/buff-ids (map #(+ 660000 %)
                                                                  (:buff-ids unit))})]
                  (if (:unit/orders namespaced-unit)
                    (update
                     namespaced-unit
                     :unit/orders
                     (fn [orders]
                       (map (fn [order] (* (:tag unit)
                                           (:ability-id order)))
                            orders)))
                    namespaced-unit)))
              (:units raw-data))
             (mapcat
                   (fn [unit]
                     (if (:orders unit)
                       (map (fn [order]
                              (merge (clojure.set/rename-keys
                                      (select-keys
                                       order
                                       (vals order-type-keymap))
                                      order-type-keymap)
                                     {:db/id (* (:tag unit) (:ability-id order))}))
                            (:orders unit))
                       []))
                   (:units raw-data))
             (when-let [{:keys [playable-area start-locations]} start-raw]
               [{:db/id -1
                 :game-info/playable-area playable-area
                 :game-info/start-locations (or start-locations [10 10])}]))
     )))

(defn distance [x1 y1 x2 y2]
  (let [dx (- x2 x1), dy (- y2 y1)]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))

(defn ability-to-action
  ([tag-or-tags ability-id]
    (ability-to-action tag-or-tags ability-id {}))
  ([tag-or-tags ability {:keys [queue-command] :or {queue-command false}}]
   #:SC2APIProtocol.sc2api$Action
   {:action-raw #:SC2APIProtocol.raw$ActionRaw
    {:action #:SC2APIProtocol.raw$ActionRaw
     {:unit-command #:SC2APIProtocol.raw$ActionRawUnitCommand
      {:unit-tags (if (coll? tag-or-tags) tag-or-tags [tag-or-tags])
       :queue-command queue-command
       :ability-id ability
       :target #:SC2APIProtocol.raw$ActionRawUnitCommand{}}}}})
  ([tag-or-tags ability target-unit-tag {:keys [queue-command] :or {queue-command false}}]
   #:SC2APIProtocol.sc2api$Action
   {:action-raw #:SC2APIProtocol.raw$ActionRaw
    {:action #:SC2APIProtocol.raw$ActionRaw
     {:unit-command #:SC2APIProtocol.raw$ActionRawUnitCommand
      {:queue-command queue-command
       :unit-tags (if (coll? tag-or-tags) tag-or-tags [tag-or-tags])
       :ability-id ability
       :target #:SC2APIProtocol.raw$ActionRawUnitCommand
       {:target-unit-tag target-unit-tag}}}}})
  ([tag-or-tags ability x y {:keys [queue-command] :or {queue-command false}}]
   #:SC2APIProtocol.sc2api$Action
   {:action-raw #:SC2APIProtocol.raw$ActionRaw
    {:action #:SC2APIProtocol.raw$ActionRaw
     {:unit-command #:SC2APIProtocol.raw$ActionRawUnitCommand
      {:unit-tags (if (coll? tag-or-tags) tag-or-tags [tag-or-tags])
       :queue-command queue-command
       :ability-id ability
       :target #:SC2APIProtocol.raw$ActionRawUnitCommand
       {:target-world-space-pos #:SC2APIProtocol.common$Point2D
        {:x x
         :y y}}}}}}))

(defn can-place?
  ([conn ability-id x y]
   (when (and x y)
     (identical? (-> (core/send-request-and-get-response-message
                      conn
                      #:SC2APIProtocol.query$RequestQuery
                      {:query #:SC2APIProtocol.query$RequestQuery
                       {:placements
                        [#:SC2APIProtocol.query$RequestQueryBuildingPlacement
                         {:ability-id ability-id
                          :target-pos #:SC2APIProtocol.common$Point2D
                          {:x x
                           :y y}}]}})
                     :query :placements first :result)
                 :success)))
  ([conn ability-id builder-tag x y]
   (when (and x y)
     (identical? (-> (core/send-request-and-get-response-message
                      conn
                      #:SC2APIProtocol.query$RequestQuery
                      {:query #:SC2APIProtocol.query$RequestQuery
                       {:placements
                        [#:SC2APIProtocol.query$RequestQueryBuildingPlacement
                         {:ability-id ability-id
                          :placing-unit-tag builder-tag
                          :target-pos #:SC2APIProtocol.common$Point2D
                          {:x x
                           :y y}}]}})
                     :query :placements first :result)
                 :success))))

(defn find-location [connection builder-tag ability-id positions]
  (loop [remaining-positions positions
         [x y] (first positions)]
    (let [can-place? (can-place? connection ability-id builder-tag x y)]
      (cond
        can-place? [[x y] remaining-positions]
        (not (empty? remaining-positions)) (recur (rest remaining-positions)
                                                  (first remaining-positions))
        :else [nil (rest remaining-positions)]))))

(defn positions-around
  ([initial-x initial-y in-radius]
   (positions-around initial-x initial-y in-radius 1))
  ([initial-x initial-y in-radius step]
   (for [x (take in-radius
                 (interleave (range 0 10) (reverse (range -10 0 step))))
         y (take in-radius
                 (interleave (range 0 10) (reverse (range -10 0 step))))]
     [(+ initial-x x) (+ initial-y y)])))

(defn build-scvs [latest-knowledge _]
  (->> (ds/q '[:find ?unit-tag ?build-ability
               :where
               [?build-me :unit-type/name "SCV"]
               [?build-me :unit-type/ability-id ?build-ability]
               [(+ 880000 ?build-ability) ?ability-e-id]
               [?builder-type-id :unit-type/abilities ?ability-e-id]
               [?builder-type-id :unit-type/unit-id ?unit-id]
               [?unit-tag :unit/unit-type ?unit-id]
               ]
             latest-knowledge)
       (map (fn [[unit-tag ability-id]]
              (ability-to-action [unit-tag] ability-id)))))

(defn build-marines [latest-knowledge _]
  (->> (ds/q '[:find ?unit-tag ?build-ability
               :where
               [?build-me :unit-type/name "Marine"]
               [?build-me :unit-type/ability-id ?build-ability]
               [(+ 880000 ?build-ability) ?ability-e-id]
               [?builder-type-id :unit-type/abilities ?ability-e-id]
               [?builder-type-id :unit-type/unit-id ?unit-id]
               [?unit-tag :unit/unit-type ?unit-id]
               ]
             latest-knowledge)
       (map (fn [[unit-tag ability-id]]
              (ability-to-action [unit-tag] ability-id)))))

(defn assoc-result [key f o]
  (assoc o key (f o)))

(defn build-ability-name-to-id [{:keys [latest-knowledge unit/type]}]
  (first
   (ds/q '[:find [?build-ability]
           :in $ ?name
           :where
           [?build-me :unit-type/name ?name]
           [?build-me :unit-type/ability-id ?build-ability]
           ]
         latest-knowledge
         type)))

(defn footprint-radius-for-ability [{:keys [latest-knowledge order/ability-id]}]
  (first
   (ds/q '[:find [?radius]
           :in $ ?ability-id
           :where
           [?a :ability-type/id ?ability-id]
           [?a :ability-type/footprint-radius ?radius]]
         latest-knowledge
         ability-id)))

(defn able-casters [{:keys [latest-knowledge order/ability-id]}]
  (ds/q '[:find [?unit-tag ...]
          :in $ ?ability-id
          :where
          [?unit-tag :unit/unit-type ?built-unit-type]
          [?t :unit-type/unit-id ?built-unit-type]
          [?t :unit-type/name ?name]
          [(+ 880000 ?ability-id) ?ab-e-id]
          [?t :unit-type/abilities ?ab-e-id]
          ]
        latest-knowledge
        ability-id))

(defn ability-order-already-issued-units [{:keys [order/ability-id latest-knowledge]}]
  (->> (ds/q '[:find ?unit-tag ?orders
               :where
               [?unit-tag :unit/orders ?orders]]
             latest-knowledge)
       (map (fn [[unit-tag orders]]
              [unit-tag (count (filter (comp #{ability-id}
                                             :ability-id)
                                       (if (coll? orders)
                                         orders
                                         [orders])))]))
       (filter (fn [[_ building-count]]
                 (> 0 building-count)))))

(defn do-times-count-supply-depots [{:keys [latest-knowledge order/ability-already-issued]}]
  (let [food-available (first (ds/q '[:find [?food-available]
                                      :where
                                      [?l :player-common/food-used ?food-used]
                                      [?l :player-common/food-cap ?food-cap]
                                      [(- ?food-cap ?food-used) ?food-available]]
                                    latest-knowledge))
        build-new-count (int (/ (- 13 food-available) 10))]
    (- build-new-count (count ability-already-issued))))

(defn do-times-count [unit-type]
  (if (= type "SupplyDepot")
    do-times-count-supply-depots
    (fn [{:keys [latest-knowledge order/ability-already-issued]}]
      (- 1 (count ability-already-issued)))))

;;env continuously take actions, stop after n steps or finish goals.
(defn get-supply-depot-actions [conn latest-knowledge available-positions]
  (let [{:keys [order/ability-id
                order/footprint-radius
                order/casters
                order/count] :as order}
        (->>
         {:unit/type "SupplyDepot"
          :latest-knowledge latest-knowledge}
         (assoc-result :order/ability-id build-ability-name-to-id)
         (assoc-result :order/footprint-radius footprint-radius-for-ability)
         (assoc-result :order/casters able-casters)
         (assoc-result :order/ability-already-issued ability-order-already-issued-units)
         (assoc-result :order/count do-times-count-supply-depots)
         ((fn [o] (dissoc o :latest-knowledge))))]
    (reduce
     (fn [{:keys [positions actions] :as acc} caster-tag]
       (let [[[found-x found-y :as found-position] remaining-positions]
             (find-location conn caster-tag ability-id positions)]
         (if found-position
           (-> acc
               (assoc :actions (conj actions (ability-to-action [caster-tag] ability-id found-x found-y {})))
               (assoc :positions (clojure.set/difference
                                  (set remaining-positions)
                                  (set (positions-around
                                        found-x
                                        found-y
                                        (Math/round footprint-radius))))))
           (reduced acc))))
     {:positions available-positions
      :actions []}
     (take count casters))))

(defn is-building? [ability-id latest-knowledge]
  (first (ds/q '[:find [?is-building]
                 :in $ ?ability-id
                 :where
                 [?e :ability-type/is-building ?is-building]
                 [?e :ability-type/id ?ability-id]]
               latest-knowledge
               ability-id)))

(defn get-unit-type-build [conn latest-knowledge {:keys [unit-type available-building-positions amount]}]
  (if (is-building? (build-ability-name-to-id {:unit/type unit-type
                                               :latest-knowledge latest-knowledge})
                    latest-knowledge)
    (let [{:keys [order/ability-id
                  order/footprint-radius
                  order/casters
                  order/count] :as order}
          (->>
           {:unit/type unit-type
            :latest-knowledge latest-knowledge}
           (assoc-result :order/ability-id build-ability-name-to-id)
           (assoc-result :order/footprint-radius footprint-radius-for-ability)
           (assoc-result :order/casters able-casters)
           (assoc-result :order/ability-already-issued ability-order-already-issued-units)
           (assoc-result :order/count (do-times-count unit-type))
           ((fn [o] (dissoc o :latest-knowledge))))]
      (reduce
       (fn [{:keys [positions actions] :as acc} caster-tag]
         (let [[[found-x found-y :as found-position] remaining-positions]
               (find-location conn caster-tag ability-id positions)]
           (if found-position
             (-> acc
                 (assoc :actions (conj actions (ability-to-action [caster-tag] ability-id found-x found-y {})))
                 (assoc :positions (clojure.set/difference
                                    (set remaining-positions)
                                    (set (positions-around
                                          found-x
                                          found-y
                                          (Math/round footprint-radius))))))
             (reduced acc))))
       {:positions available-building-positions
        :actions []}
       (take count casters)))
    (let [{:keys [order/ability-id
                           order/casters
                           order/count] :as order}
                   (->>
                    {:unit/type unit-type
                     :latest-knowledge latest-knowledge}
                    (assoc-result :order/ability-id build-ability-name-to-id)
                    (assoc-result :order/casters able-casters)
                    (assoc-result :order/ability-already-issued ability-order-already-issued-units)
                    (assoc-result :order/count (do-times-count unit-type))
                    ((fn [o] (dissoc o :latest-knowledge))))]
               {:actions (map #(ability-to-action [%] ability-id {})
                              (take count casters))
                :positions available-building-positions})))

(defn positions-near-unit-type [type latest-knowledge]
  (let [[near-x near-y] (or (ds/q '[:find [?x ?y]
                                   :in $ ?type
                                   :where
                                   [?id :unit/unit-type ?type-id]
                                   [(+ 990000 ?type-id) ?type-e-id]
                                   [?type-e-id :unit-type/name ?type]
                                   [?id :unit/x ?x]
                                   [?id :unit/y ?y]]
                                 latest-knowledge
                                 type) [10 10])
        positions (positions-around near-x near-y 15)]
    positions))


(defn build-barracks [latest-knowledge connection]
  (let [[builder-tag ability-id] (ds/q '[:find [?unit-tag ?build-ability]
                                         :where
                                         [?build-me :unit-type/name "Barracks"]
                                         [?build-me :unit-type/ability-id ?build-ability]
                                         [(+ 880000 ?build-ability) ?ability-e-id]
                                         [?builder-type-id :unit-type/abilities ?ability-e-id]
                                         [?builder-type-id :unit-type/unit-id ?unit-id]
                                         [?unit-tag :unit/unit-type ?unit-id]
                                         ]
                                       latest-knowledge)
        ordered (ds/q '[:find ?unit-tag ?orders
                        :where
                        [?unit-tag :unit/orders ?orders]]
                      latest-knowledge)
        building-already (->> ordered
                              (map (fn [[unit-tag orders]]
                                     [unit-tag (count (filter (comp #{ability-id}
                                                                    :ability-id)
                                                              (if (coll? orders)
                                                                orders
                                                                [orders])))]))
                              (filter (fn [[_ building-count]]
                                        (> 0 building-count))))
        [near-x near-y] (ds/q '[:find [?x ?y]
                                :where
                                [?id :unit/unit-type ?type-id]
                                [(+ 990000 ?type-id) ?type-e-id]
                                [?type-e-id :unit-type/name "CommandCenter"]
                                [?id :unit/x ?x]
                                [?id :unit/y ?y]]
                              latest-knowledge)
        currently-building-count (count building-already)
        positions (positions-around (+ 5 near-x) near-y 10)
        existing-barrack-count (count (ds/q '[:find ?unit-id
                                              :where
                                              [?id :unit-type/name "Barracks"]
                                              [?id :unit-type/unit-id ?unit-type-id]
                                              [?unit-id :unit/unit-type ?unit-type-id]]
                                            latest-knowledge))
        builders (ds/q '[:find [?builder-tag ...]
                         :where
                         [?builder-tag :unit/unit-type ?type-id]
                         [?type-e-id :unit-type/unit-id ?type-id]
                         [?type-e-id :unit-type/name "SCV"]
                         ]
                       latest-knowledge)]
    (get (reduce
          (fn [{:keys [actions positions]} unit-tag]
            (let [[position remaining-pos] (find-location connection unit-tag
                                                          ability-id positions)]
              (if position
                {:actions (conj actions
                                (ability-to-action [unit-tag] ability-id
                                                   (first position)
                                                   (second position)
                                                   {}))
                 :positions remaining-pos}
                (reduced {:actions actions
                          :positions remaining-pos}))))
          {:positions positions
           :actions []}
          (take (- 5
                   existing-barrack-count)
                builders))
         :actions)))

(defn attack-with [first-gather-amount unit-name x y]
    (fn [latest-knowledge _]
      (if (>= (count (ds/q '[:find ?tag
                           :in $ ?unit-name
                           :where
                           [?tag :unit/unit-type ?type-id]
                           [?type :unit-type/unit-id ?type-id]
                           [?type :unit-type/name ?unit-name]]
                         latest-knowledge
                         unit-name))
              first-gather-amount
              )
        [#:SC2APIProtocol.sc2api$Action
         {:action-ui #:SC2APIProtocol.ui$ActionUI
          {:action #:SC2APIProtocol.ui$ActionUI{:select-army {}}}}
         #:SC2APIProtocol.sc2api$Action
         {:action-raw #:SC2APIProtocol.raw$ActionRaw
          {:action #:SC2APIProtocol.raw$ActionRaw
           {:unit-command #:SC2APIProtocol.raw$ActionRawUnitCommand
            {:target #:SC2APIProtocol.raw$ActionRawUnitCommand
             {:target-world-space-pos #:SC2APIProtocol.common$Point2D
              {:x x :y y}}
             :ability-id 23}}}}]
        [])))

(defn create-actions [knowledge-base strategies obs connection]
  (let [latest-knowledge (ds/db-with knowledge-base
                                     (obs->facts obs))]
    (mapcat (fn [strategy]
              (strategy latest-knowledge connection))
            strategies)))


(defn obs->minimap-built-facts [observation]
    (map-indexed
     (fn [index minimap-point]
       (merge minimap-point
              {:db/id (* -1 (inc index))}))
     (let [pixel-rows (->>
                       observation
                       :feature-layer-data
                       :minimap-renders
                       :player-relative
                       :data
                       (map #(= 0 %))
                       (partition 64)
                       )]
       (for [[y-index pixel-row] (zipmap (range) pixel-rows)
             [x-index pixel] (zipmap (range) pixel-row)]
         {:minimap/x x-index
          :minimap/y y-index
          :minimap/available pixel}))))


(comment defn print-minimap-to-string [observation]
  (->>
   (ds/q '[:find ?y ?x ?av
           :in $ % positions-around can-place distance
           :where
           [?e :minimap/available ?av]
           [?e :minimap/x ?x]
           [?e :minimap/y ?y]
           ]
         (ds/db-with knowledge-base
                     (concat (obs->minimap-built-facts observation)
                             (obs->facts observation)))
         (concat can-build-rule
                 units-of-type-rule)
         positions-around
         (partial can-place? conn)
         distance)
   (sort-by (fn [[y x _]]  (+ (* 100 y) x)))
   (map (fn [[x y a]] (if a (str "  ") "x ")))
   (partition 64)
   (map (partial clojure.string/join ""))
   (clojure.string/join "\n")
   (println)
   ))
