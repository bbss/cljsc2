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
   :buff-ids :unit/buff-ids
   :cargo-space-max :unit/cargo-space-max
   :tag :unit/tag
   :is-flying :unit/is-flying
   :shield-max :unit/shield-max
   :owner :unit/owner
   })

(defn obs->facts [{:keys [game-loop player-common raw-data]}]
   (let [{:keys [minerals vespene food-cap food-used food-workers
                 idle-worker-count army-count player-id]} player-common
         game-loop-id (+ 1234000000 game-loop)]
     (concat [{:db/id game-loop-id
               :player-common/minerals minerals
               :player-common/vespene vespene
               :player-common/food-used food-used
               :player-common/food-workers food-workers
               :player-common/idle-worker-count idle-worker-count
               :player-common/army-count army-count}
              {:db/id -1 :meta/latest-game-loop game-loop}
              {:db/id -2 :meta/player-id player-id}]
             (map
              (fn [unit]
                (merge (clojure.set/rename-keys
                       (select-keys
                        unit
                        (vals unit-keymap))
                       unit-keymap)
                       {:db/id (:tag unit)
                        :unit/x (:x (:pos unit))
                        :unit/y (:y (:pos unit))
                        :unit/z (:z (:pos unit))
                        :unit/buff-ids (map #(+ 660000 %) (:buff-ids unit))}))
              (:units raw-data)))
     ))

(defn distance [x1 y1 x2 y2]
  (let [dx (- x2 x1), dy (- y2 y1)]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))
