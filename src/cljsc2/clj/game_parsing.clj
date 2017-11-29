(ns cljsc2.clj.game-parsing
  (:require [cljsc2.clj.core :as core]
            [manifold.stream :as s :refer [stream]]
            [clojure.data :refer [diff]]
            [datascript.core :as ds]
            [clojure.spec.alpha :as spec]))

(use 'cljsc2.clj.core)

(def db
  (ds/create-conn
   {:unit/type {:db/cardinality :db.cardinality/many
                :db/valueType :db.type/ref}
    :unit-type/abilities {:db/cardinality :db.cardinality/many
                          :db/valueType :db.type/ref}
    }))

;; I want to have all the data for StarCraft II near at hand so I can quickly set-up experiments

;; There are several ways to get data about the game from the API, but most of it is pretty low level and needs some manual wiring up to become useful.

;; Quick experimenting is key in getting the most out of this environment so getting very familiar with where to find what and building tools around it is a good start.

;; In order to find out what kind of static data is in the game (like how does a command center cost?) we can query the RequestData.

(start-client)

(load-simple-map)

(flush-incoming-responses)

(let [[abilities unit-types] ((juxt :abilities :units)
                              (->
                               (req #:SC2APIProtocol.sc2api$RequestData
                                    {:data #:SC2APIProtocol.sc2api$RequestData
                                     {:ability-id true
                                      :unit-type-id true}})
                               :data
                               ))]
  (def abilities abilities)
  (def unit-types unit-types)
  )

(count abilities)
;; => 3771
(count unit-types)
;; => 1922

;; note that many unit types and abilties are not in the 1v1 game, it includes assets from the single-player campaign

;; the only way so far to find out what is currently in the game is by playing it and studying the observations

;; it would be nice to be able to control the environment from code and then make a relational model so we can link abilities to unit types, let's do that.

;; we'll start of by seeing what kind of non static information is available by looking at the units available during the running game.

(declare last-running)

(defn do-in-starcraft [connection run-until-fn step-fn]
  (flush-incoming-responses)
  (when (or (not (bound? (find-var `last-running))) @(:results-promise last-running))
        (let [results (promise)
              starting-observation (request-observation connection)
              run-until-pred (run-until-fn (get-in starting-observation [:observation :observation]))
              observations (transient [])
              actions (transient [])
              runner (run-loop
                      connection
                      {:step-fn (fn [obs connection]
                                  (let [step-actions (step-fn
                                                      obs
                                                      connection)
                                        stop-running? (run-until-pred obs
                                                                      actions)]
                                    (conj! observations obs)
                                    (conj! actions step-actions)
                                    (if stop-running?
                                      (do (deliver results [(persistent! observations)
                                                            (persistent! actions)])
                                          nil)
                                      step-actions)
                                    ))}
                      {})]
          (s/on-closed (:incoming-step-observations-stream runner)
                       (fn [] (deliver results [(persistent! observations)
                                                (persistent! actions)])))
          (def last-running {:results-promise results
                             :run-loop runner}))))

(defn run-for-n-game-loops [n]
  (fn [starting-obs]
    (let [starting-game-loop (get-in starting-obs [:game-loop])]
      (fn [obs _]
        (let [current-game-loop (get-in obs [:game-loop])
              current-run-steps (- current-game-loop starting-game-loop)]
          (>= current-run-steps
              n))))))

(defn request-abilities [unit-tags ignore-resource-requirement]
  (req #:SC2APIProtocol.query$RequestQuery
       {:query #:SC2APIProtocol.query$RequestQuery
        {:abilities (map (fn [id]
                           #:SC2APIProtocol.query$RequestQueryAvailableAbilities
                           {:unit-tag id})
                         unit-tags)
         :ignore-resource-requirements ignore-resource-requirement}}))

(defn select-one-of-type [obs type]
  (->>
   obs
   :raw-data
   :units
   (filter (comp #{type} :unit-type))
   first
   :tag))

(defn select-one-unit-of-type [obs type]
  (->>
   obs
   :raw-data
   :units
   (filter (comp #{type} :unit-type))
   first
   ))

(defn select-one-of-types [obs & types]
  (->>
   obs
   :raw-data
   :units
   (filter (comp (set types) :unit-type))
   first
   :tag))

(defn select-all-units-of-types [obs & types]
  (->>
   obs
   :raw-data
   :units
   (filter (comp (set types) :unit-type))))

(defn select-all-of-types [obs & types]
  (->>
   obs
   :raw-data
   :units
   (filter (comp (set types) :unit-type))
   (map :tag)))

(comment
  (flatten (concat
            [(doall (into {}
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
            )))

;;since I need to play against an AI to step through the game on this map lets build a planetary quickly

(def get-obs
  (do-in-starcraft
   connection
   (run-for-n-game-loops 1)
   (fn [obs conn]
     (def obs obs)
     [])))

(defn play-for-n [n]
  (do-in-starcraft
     connection
     (run-for-n-game-loops n)
     (fn [obs conn]
       (def obs obs)
       [])))

(defn actions [action-list]
  #:SC2APIProtocol.sc2api$RequestAction
  {:action #:SC2APIProtocol.sc2api$RequestAction
   {:actions
    action-list}})

(defn ability-to-action
  ([tags ability-id]
   #:SC2APIProtocol.sc2api$Action
   {:action-raw #:SC2APIProtocol.raw$ActionRaw
    {:action #:SC2APIProtocol.raw$ActionRaw
     {:unit-command #:SC2APIProtocol.raw$ActionRawUnitCommand
      {:unit-tags tags
       :ability-id ability-id
       :target #:SC2APIProtocol.raw$ActionRawUnitCommand
       {}}}}})
  ([tags ability-id x y]
   #:SC2APIProtocol.sc2api$Action
   {:action-raw #:SC2APIProtocol.raw$ActionRaw
    {:action #:SC2APIProtocol.raw$ActionRaw
     {:unit-command #:SC2APIProtocol.raw$ActionRawUnitCommand
      {:unit-tags tags
       :ability-id ability-id
       :target #:SC2APIProtocol.raw$ActionRawUnitCommand
       {:target-world-space-pos #:SC2APIProtocol.common$Point2D
        {:x x
         :y y}}}}}})
  ([tags ability-id target-tag]
   #:SC2APIProtocol.sc2api$Action
   {:action-raw #:SC2APIProtocol.raw$ActionRaw
    {:action #:SC2APIProtocol.raw$ActionRaw
     {:unit-command #:SC2APIProtocol.raw$ActionRawUnitCommand
      {:unit-tags tags
       :ability-id ability-id
       :target #:SC2APIProtocol.raw$ActionRawUnitCommand
       {:target-unit-tag target-tag}}}}}))

(defn idle-workers
  ([]
   #:SC2APIProtocol.sc2api$Action
   {:action-ui #:SC2APIProtocol.ui$ActionUI
    {:action #:SC2APIProtocol.raw$ActionUI
     {:select-idle-worker #:SC2APIProtocol.ui$ActionSelectIdleWorker
      {:type "All"}}}}))

(play-for-n 200)

(def build-supply
  (do-in-starcraft
   connection
   (run-for-n-game-loops 1000)
   (let [did-once (atom false)]
     (fn [obs conn]
       (if @did-once
         []
         (do (reset! did-once true)
             (-> [(select-one-of-type obs 45)]
                 (ability-to-action 319 60.8 24.4))))))))

(def build-cc
  (do-in-starcraft
   connection
   (run-for-n-game-loops 1000)
   (let [did-once (atom false)]
     (fn [obs conn]
       (if @did-once
         []
         (do (reset! did-once true)
             (-> [(select-one-of-type obs 45)]
                 (ability-to-action 318 25.158936 25.784424))
             ))))))

(def build-refinery
  (do-in-starcraft
   connection
   (run-for-n-game-loops 1500)
   (let [did-once (atom false)]
     (fn [obs conn]
       (if @did-once
         []
         (do (reset! did-once true)
             (-> [(select-one-of-type obs 45)]
                 (ability-to-action 320 (->>
                                         obs
                                         :raw-data
                                         :units
                                         (filter (comp #{342} :unit-type))
                                         last
                                         :tag
                                         )))))))))

(def saturate-gas
  (do-in-starcraft
   connection
   (run-for-n-game-loops 500)
   (let [did-once (atom false)]
     (fn [obs conn]
       (if @did-once
         []
         (do (reset! did-once true)
             (doall (map
                     #(ability-to-action
                       [%] 1
                       (->>
                        obs
                        :raw-data
                        :units
                        (filter (comp #{20} :unit-type))
                        last
                        :tag
                        ))
                     (take 3 (->>
                              obs
                              :raw-data
                              :units
                              (filter (comp #{45} :unit-type))
                              (map :tag)
                              ))
                     ))))))))

(def build-engibay
  (do-in-starcraft
   connection
   (run-for-n-game-loops 1000)
   (let [did-once (atom false)]
     (fn [obs conn]
       (if @did-once
         []
         (do (reset! did-once true)
             (-> [(select-one-of-type obs 45)]
                 (ability-to-action 322 45.158936 25.784424))))))))

(def build-planetary
  (do-in-starcraft
   connection
   (run-for-n-game-loops 100)
   (let [did-once (atom false)]
     (fn [obs conn]
       (if @did-once
         []
         (do (reset! did-once true)
             (-> [(->>
                   obs
                   :raw-data
                   :units
                   (filter (comp #{1} :owner))
                   (filter (comp #{18} :unit-type))
                   first
                   :tag
                   )]
                 (ability-to-action 1450))))))))

(defn distance [[x1 y1] [x2 y2]]
  (let [dx (- x2 x1), dy (- y2 y1)]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))

(defn set-closest-rally-point [obs unit & located-unit-types]
  (let [closest-mineral-tag (->>
                             (apply (partial select-all-units-of-types obs) located-unit-types)
                             (map (fn [{:keys [pos] :as located-unit}]
                                    {:distance (distance [(:x pos) (:y pos)]
                                                         [(:x (:pos unit)) (:y (:pos unit))])
                                     :located-unit located-unit}))
                             (sort-by :distance)
                             first
                             :located-unit
                             :tag)]
    (ability-to-action [(:tag unit)] 1 closest-mineral-tag)))

(defn set-rally-point [unit x y]
  (ability-to-action [(:tag unit)] 1 x y))

(defn build-scvs [obs]
  (let [command-centers (select-all-units-of-types obs 130 132 18)]
    (doall (map
            (fn [{:keys [tag orders]}]
              (ability-to-action [tag] 524))
            (filter #(< (count (:orders %)) 2)
                    command-centers)))))

(def set-rallies-cc
  (do-in-starcraft
   connection
   (run-for-n-game-loops 20)
   (let [did-once (atom false)]
     (fn [obs conn]
       (let [command-centers (select-all-units-of-types obs 130 132 18)]
         (if @did-once
           (build-scvs obs)
           (do (reset! did-once true)
               (map #(set-closest-rally-point obs %
                                              341 483 146 147)
                    command-centers))))))))

(def more-scvs
  (do-in-starcraft
   connection
   (run-for-n-game-loops 1000)
   (let [did-once (atom false)]
     (fn [obs conn]
       (build-scvs obs)))))

(def build-barracks
  (do-in-starcraft
   connection
   (run-for-n-game-loops 1000)
   (let [did-once (atom false)]
     (fn [obs conn]
       (if @did-once
         []
         (do (reset! did-once true)
             (map-indexed (fn [index unit-tag]
                            (-> [unit-tag]
                                (ability-to-action 321 50 (+ 19 (* index 3)))))
                          (take 5 (select-all-of-types obs 45)))))))))

(defn build-marines [obs]
  (let [barracks (select-all-units-of-types obs 21)]
    (doall (map
            (fn [{:keys [tag orders]}]
              (ability-to-action [tag] 560))
            (filter #(< (count (:orders %)) 3)
                    barracks)))))

(def set-rallies-army-production
  (do-in-starcraft
   connection
   (run-for-n-game-loops 20)
   (let [did-once (atom false)]
     (fn [obs conn]
       (let [buildings (select-all-units-of-types obs 21)]
         (if @did-once
           (build-marines obs)
           (do (reset! did-once true)
               (map #(set-rally-point % 30 25)
                    buildings))))))))

(def build-more-supply
  (do-in-starcraft
   connection
   (run-for-n-game-loops 1000)
   (let [did-once (atom false)]
     (fn [obs conn]
       (if @did-once
         (build-marines obs)
         (do (reset! did-once true)
             (map-indexed (fn [index unit-tag]
                            (-> [unit-tag]
                                (ability-to-action 319 55 (+ 20 (* index 2)))))
                          (take 10 (select-all-of-types obs 45)))))))))

(def more-marines
  (do-in-starcraft
   connection
   (run-for-n-game-loops 1000)
   (let [did-once (atom false)]
     (fn [obs conn]
       (build-marines obs)))))

(defn smart-click-tag [tag]
  #:SC2APIProtocol.sc2api$Action
  {:action-raw #:SC2APIProtocol.raw$ActionRaw
   {:action #:SC2APIProtocol.raw$ActionRaw
    {:unit-command #:SC2APIProtocol.raw$ActionRawUnitCommand
     {:ability-id 1
      :target #:SC2APIProtocol.raw$ActionRawUnitCommand
      {:target-unit-tag tag}}}}})

(defn get-closest-mineral [unit]
  (->>
   (select-all-units-of-types obs 341 483 146 147)
   (map (fn [{:keys [pos] :as located-unit}]
          {:distance (distance [(:x pos) (:y pos)]
                               [(:x (:pos unit)) (:y (:pos unit))])
           :located-unit located-unit}))
   (sort-by :distance)
   first
   :located-unit
   :tag))

(def send-lazy-workers-to-mine
  (do-in-starcraft
   connection
   (run-for-n-game-loops 3)
   (let [did-once (atom false)]
     (fn [obs conn]
       (if @did-once
         (map #(ability-to-action
               [%]
               1
               (get-closest-mineral (let [command-centers (select-all-units-of-types obs 130 132 18)]
                                      (if (< (:x (:pos (first command-centers)))
                                             (:x (:pos (second command-centers))))
                                        (first command-centers)
                                        (second command-centers)))))
              (->> obs
                   :raw-data
                   :units
                   (filter :is-selected)
                   (map :tag)))
         (do (reset! did-once true)
             (idle-workers)))))))

(comment (->> (request-abilities [(select-one-of-type obs 45)] true)
             :query
             :abilities
             first
             :abilities
             (map :ability-id)
             (map cljsc2.clj.build-order/get-ability-type-by-id)))

(def all-units-abilities (atom {}))

(for [ability (filter (fn [it] (and (:is-building it)
                                    (:available it)))
                      '({:ability-id 4, :link-name "stop", :link-index 0, :button-name "Stop", :friendly-name "Stop Stop", :hotkey "S", :remaps-to-ability-id 3665, :available true, :target :none, :allow-minimap false} {:ability-id 16, :link-name "move", :link-index 0, :button-name "Move", :friendly-name "Move", :hotkey "M", :available true, :target :pointorunit, :allow-minimap true} {:ability-id 17, :link-name "move", :link-index 1, :button-name "MovePatrol", :friendly-name "Patrol", :hotkey "P", :available true, :target :pointorunit, :allow-minimap true} {:ability-id 18, :link-name "move", :link-index 2, :button-name "MoveHoldPosition", :friendly-name "HoldPosition", :hotkey "H", :available true, :target :none, :allow-minimap false} {:ability-id 23, :link-name "attack", :link-index 0, :button-name "Attack", :friendly-name "Attack Attack", :hotkey "A", :remaps-to-ability-id 3674, :available true, :target :pointorunit, :allow-minimap true} {:ability-id 26, :link-name "SprayTerran", :link-index 0, :button-name "Spray", :friendly-name "Effect Spray Terran", :hotkey "Y", :remaps-to-ability-id 3684, :available true, :target :point, :allow-minimap false, :allow-autocast false, :cast-range 1.0} {:ability-id 295, :link-name "SCVHarvest", :link-index 0, :button-name "Gather", :friendly-name "Harvest Gather SCV", :hotkey "G", :remaps-to-ability-id 3666, :available true, :target :unit, :allow-minimap false} {:ability-id 296, :link-name "SCVHarvest", :link-index 1, :button-name "ReturnCargo", :friendly-name "Harvest Return SCV", :hotkey "C", :remaps-to-ability-id 3667, :available true, :target :none, :allow-minimap false} {:ability-id 316, :link-name "Repair", :link-index 0, :button-name "Repair", :friendly-name "Effect Repair SCV", :hotkey "R", :remaps-to-ability-id 3685, :available true, :target :unit, :allow-minimap false, :allow-autocast true} {:ability-id 318, :link-name "TerranBuild", :link-index 0, :button-name "CommandCenter", :friendly-name "Build CommandCenter", :hotkey "BC", :available true, :target :point, :allow-minimap false, :is-building true, :footprint-radius 2.5} {:ability-id 319, :link-name "TerranBuild", :link-index 1, :button-name "SupplyDepot", :friendly-name "Build SupplyDepot", :hotkey "BS", :available true, :target :point, :allow-minimap false, :is-building true, :footprint-radius 1.0} {:ability-id 320, :link-name "TerranBuild", :link-index 2, :button-name "Refinery", :friendly-name "Build Refinery", :hotkey "BR", :available true, :target :unit, :allow-minimap false, :is-building true, :footprint-radius 1.5} {:ability-id 321, :link-name "TerranBuild", :link-index 3, :button-name "Barracks", :friendly-name "Build Barracks", :hotkey "BB", :available true, :target :point, :allow-minimap false, :is-building true, :footprint-radius 1.5} {:ability-id 322, :link-name "TerranBuild", :link-index 4, :button-name "EngineeringBay", :friendly-name "Build EngineeringBay", :hotkey "BE", :available true, :target :point, :allow-minimap false, :is-building true, :footprint-radius 1.5} {:ability-id 323, :link-name "TerranBuild", :link-index 5, :button-name "MissileTurret", :friendly-name "Build MissileTurret", :hotkey "BT", :available true, :target :point, :allow-minimap false, :is-building true, :footprint-radius 1.0} {:ability-id 326, :link-name "TerranBuild", :link-index 8, :button-name "SensorTower", :friendly-name "Build SensorTower", :hotkey "BN", :available true, :target :point, :allow-minimap false, :is-building true, :footprint-radius 0.5} {:ability-id 1, :link-name "", :link-index 255, :button-name "Smart", :friendly-name "Smart", :available true, :target :pointorunit, :allow-minimap true}))
      ]
  (do-in-starcraft connection (run-for-n-game-loops 2000)
                   (fn [obs conn]

                     )))

(cljsc2.clj.build-order/get-unit-types-by-name "mineral")

(cljsc2.clj.build-order/get-unit-type-by-name "barrack")

(cljsc2.clj.build-order/get-ability-type-by-name "gather")

(cljsc2.clj.build-order/get-ability-types-by-name "rally")


;; => ({:ability-id 196, :link-name "Rally", :link-index 1, :friendly-name "Rally_1", :hotkey "", :available true, :target :pointorunit, :allow-minimap true} {:ability-id 197, :link-name "Rally", :link-index 2, :friendly-name "Rally_2", :hotkey "", :available true, :target :pointorunit, :allow-minimap true} {:ability-id 198, :link-name "Rally", :link-index 3, :friendly-name "Rally_3", :hotkey "", :available true, :target :pointorunit, :allow-minimap true} {:ability-id 207, :link-name "RallyNexus", :link-index 0, :button-name "Rally", :friendly-name "Rally Nexus", :hotkey "Y", :remaps-to-ability-id 3690, :available true, :target :pointorunit, :allow-minimap true} {:ability-id 3673, :link-name "GeneralRallyUnits", :link-index 0, :button-name "Rally Units", :friendly-name "Rally Units", :hotkey "", :available true, :target :pointorunit, :allow-minimap true} {:ability-id 208, :link-name "RallyNexus", :link-index 1, :friendly-name "RallyNexus_1", :hotkey "", :available true, :target :pointorunit, :allow-minimap true} {:ability-id 209, :link-name "RallyNexus", :link-index 2, :friendly-name "RallyNexus_2", :hotkey "", :available true, :target :pointorunit, :allow-minimap true} {:ability-id 210, :link-name "RallyNexus", :link-index 3, :friendly-name "RallyNexus_3", :hotkey "", :available true, :target :pointorunit, :allow-minimap true} {:ability-id 3690, :link-name "GeneralRallyWorkers", :link-index 0, :button-name "Rally Workers", :friendly-name "Rally Workers", :hotkey "", :available true, :target :pointorunit, :allow-minimap true} {:ability-id 195, :link-name "Rally", :link-index 0, :button-name "Rally", :friendly-name "Rally Building", :hotkey "Y", :remaps-to-ability-id 3673, :available true, :target :pointorunit, :allow-minimap true})
