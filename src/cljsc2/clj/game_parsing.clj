(ns cljsc2.clj.game-parsing
  (:require [cljsc2.clj.core :as core]
            [manifold.stream :as s :refer [stream]]
            [clojure.data :refer [diff]]
            [datascript.core :as ds]
            [clojure.spec.alpha :as spec]
            )
  )

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

(defn run-for [n]
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

(comment
  (start-client "/home/bb/cljsc2/StarCraftII/Versions/Base59877/SC2_x64")

  (send-request-and-get-response-message
   connection
   #:SC2APIProtocol.sc2api$RequestCreateGame
   {:create-game #:SC2APIProtocol.sc2api$RequestCreateGame
    {:map #:SC2APIProtocol.sc2api$LocalMap
     {:local-map
      {:map-path "/home/bb/cljsc2/StarCraftII/Maps/Simple64.SC2Map"}}
     :player-setup
     [#:SC2APIProtocol.sc2api$PlayerSetup
      {:race "Terran" :type "Participant"}
      #:SC2APIProtocol.sc2api$PlayerSetup
      {:race "Protoss" :type "Computer"}]}})

  (send-request-and-get-response-message
   connection
   #:SC2APIProtocol.sc2api$RequestJoinGame
   {:join-game
    #:SC2APIProtocol.sc2api$RequestJoinGame
    {:participation
     #:SC2APIProtocol.sc2api$RequestJoinGame
     {:race "Terran"}
     :options #:SC2APIProtocol.sc2api$InterfaceOptions
     {:raw true
      :feature-layer #:SC2APIProtocol.sc2api$SpatialCameraSetup
      {:width 24
       :resolution #:SC2APIProtocol.common$Size2DI{:x 84 :y 84}
       :minimap-resolution #:SC2APIProtocol.common$Size2DI{:x 64 :y 64}
       }
      :render #:SC2APIProtocol.sc2api$SpatialCameraSetup
      {:width 24
       :resolution #:SC2APIProtocol.common$Size2DI{:x 480 :y 270}
       :minimap-resolution #:SC2APIProtocol.common$Size2DI{:x 64 :y 64}
       }
      }}}))

(defn run-loop-recur-collect-obs-actions
  ([connection step-fn]
   (run-loop-recur-collect-obs-actions connection step-fn {}))
  ([connection step-fn {:keys [collect-actions
                               collect-observations
                               run-until-fn]
                        :or {collect-actions false
                             collect-observations false
                             run-until-fn (run-for 500)}}]
   (flush-incoming-responses)
   (let [loops (atom 0)
         observations-transient (transient [])
         actions-transient (transient [])
         initial-observation (send-request-and-get-response-message
                              connection
                              #:SC2APIProtocol.sc2api$RequestObservation
                              {:observation {}})
         run-until-pred (run-until-fn (get-in initial-observation
                                              [:observation :observation]))]
     {:run-loop
      (future
        (loop [request-observation initial-observation
               observations observations-transient
               actions actions-transient]
          (let [observation (get-in request-observation
                                    [:observation :observation])
                ended? (or (identical? (:status request-observation) :ended)
                           (run-until-pred observation @loops))
                step-actions (step-fn observation connection)]
            (swap! loops inc)
            (if (and (not ended?)
                     step-actions)
              (do
                (send-request-and-get-response-message
                 connection
                 #:SC2APIProtocol.sc2api$RequestAction
                 {:action #:SC2APIProtocol.sc2api$RequestAction
                  {:actions
                   step-actions}})
                (send-request-and-get-response-message
                 connection
                 #:SC2APIProtocol.sc2api$RequestStep{:step {}})
                (let [after-obs (send-request-and-get-response-message
                                 connection
                                 #:SC2APIProtocol.sc2api$RequestObservation
                                 {:observation {}})]
                  (println "stepcount" @loops)
                  (recur after-obs
                         (if collect-observations
                           (conj! observations observation)
                           observations)
                         (if collect-actions
                           (conj! actions step-actions)))))
              [observations actions]))))})))

(defn do-sc2
  ([step]
   (do-sc2 step {}))
  ([step opts]
   (run-loop-recur-collect-obs-actions connection step opts)))

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

(defn select-army
  ([]
   #:SC2APIProtocol.sc2api$Action
   {:action-ui #:SC2APIProtocol.ui$ActionUI
    {:action #:SC2APIProtocol.raw$ActionUI
     {:select-army #:SC2APIProtocol.ui$ActionSelectArmy
      {:selection-add false}}}}))

(def get-minerals-for-supply-depot
  (do-sc2
     (fn [obs _]
       [])
     {:run-until-fn (run-for 220)}))

(def build-supply
  (do-sc2
     (let [did-once (atom false)]
       (fn [obs conn]
         (if @did-once
           []
           (do (reset! did-once true)
               [(-> [(select-one-of-type obs 45)]
                    (ability-to-action 319 60.8 24.4))]))))
     {:run-until-fn (run-for 520)}))

(def get-minerals-for-cc
  (do-sc2
   (fn [obs _]
     [])
   {:run-until-fn (run-for 400)}))

(def build-cc
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs conn]
       (if @did-once
         []
         (do (reset! did-once true)
             [(-> [(select-one-of-type obs 45)]
                  (ability-to-action 318 25 25))]
             ))))
   {:run-until-fn (run-for 200)}))

(def build-refinery
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs conn]
       (if @did-once
         []
         (do (reset! did-once true)
             [(-> [(select-one-of-type obs 45)]
                  (ability-to-action
                   320
                   (->>
                    obs
                    :raw-data
                    :units
                    (filter (comp #{342} :unit-type))
                    last
                    :tag
                    )))]))))
   {:run-until-fn (run-for 600)}))

(def saturate-gas
  (do-sc2
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
                              ))))))))
   {:run-until-fn (run-for 20)}))

(def build-engibay
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs conn]
       (if @did-once
         []
         (do (reset! did-once true)
             [(-> [(select-one-of-type obs 45)]
                  (ability-to-action 322 45.158936 25.784424))]))))))

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
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs conn]
       (let [command-centers (select-all-units-of-types obs 130 132 18)]
         (if @did-once
           (build-scvs obs)
           (do (reset! did-once true)
               (map #(set-closest-rally-point obs %
                                              341 483 146 147)
                    command-centers))))))
   {:run-until-fn (run-for 1)}))


(def more-scvs
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs conn]
       (build-scvs obs)))))

(def more-scvs
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs conn]
       (build-scvs obs)))))


(do-sc2 (fn [_ _] []))

(def build-planetary
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs conn]
       (if @did-once
         []
         [(do (reset! did-once true)
              (-> [(->>
                    obs
                    :raw-data
                    :units
                    (filter (comp #{1} :owner))
                    (filter (comp #{18} :unit-type))
                    (sort-by (comp :x :pos))
                    first
                    :tag
                    )]
                  (ability-to-action 1450)))])))))

(def more-scvs
  (do-sc2
   (let [did-once (atom false)]
           (fn [obs conn]
             (build-scvs obs)))))

(def more-scvs
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs conn]
       (build-scvs obs)))))

(def more-scvs
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs conn]
       (build-scvs obs)))))

(def build-barracks
  (do-sc2
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
  (do-sc2
   (let [did-once (atom false)]
           (fn [obs conn]
             (let [buildings (select-all-units-of-types obs 21)]
               (if @did-once
                 (build-marines obs)
                 (do (reset! did-once true)
                     (map #(set-rally-point % 30 25)
                          buildings))))))
   {:run-until-fn (run-for 1)}))

(def build-more-supply
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs conn]
       (if @did-once
         (build-marines obs)
         (do (reset! did-once true)
             (map-indexed (fn [index unit-tag]
                            (-> [unit-tag]
                                (ability-to-action 319 55 (+ 20 (* index 2)))))
                          (take 10 (select-all-of-types obs 45)))))))))

(do-sc2 (fn [_ _] []))

(def build-more-supply-somewhere-else
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs conn]
       (if @did-once
         (build-marines obs)
         (do (reset! did-once true)
             (map-indexed (fn [index unit-tag]
                            (-> [unit-tag]
                                (ability-to-action 319 58 (+ 20 (* index 2)))))
                          (take 10 (select-all-of-types obs 45)))))))))


(def build-factory
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs _]
       (if @did-once
         []
         [(do (reset! did-once true)
              (ability-to-action [(select-one-of-type obs 45)]
                                328
                                26 30))])))))

(def more-marines
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs conn]
       (build-marines obs)))))

(def more-scvs
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs conn]
       (build-scvs obs)))))

(def build-starport
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs _]
       (if @did-once
         []
         [(do (reset! did-once true)
              (ability-to-action [(select-one-of-type obs 45)]
                                 329
                                 26 34))])))))

(def build-factory-2
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs _]
       (if @did-once
         []
         [(do (reset! did-once true)
              (ability-to-action [(select-one-of-type obs 45)]
                                 328
                                 31 30))])))))

(def build-starport-2
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs _]
       (if @did-once
         []
         [(do (reset! did-once true)
              (ability-to-action [(select-one-of-type obs 45)]
                                 329
                                 31 34))])))))

(def send-army-to-middle
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs conn]
       (if @did-once
         (map #(ability-to-action
                [%]
                1
                40
                35)
              (->> obs
                   :raw-data
                   :units
                   (filter :is-selected)
                   (map :tag)))
         (do (reset! did-once true)
             [(select-army)]))))
   {:run-until-fn (run-for 2)}))

(def more-marines
  (do-sc2
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

(defn get-closest-mineral [obs unit]
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
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs conn]
       (if @did-once
         (map #(ability-to-action
                [%]
                1
                (get-closest-mineral
                 obs
                 (let [command-centers (select-all-units-of-types obs 130 132 18)]
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
             [(idle-workers)]))))
   {:run-until-fn (run-for 2)}))

(def build-reactor-on-starport
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs _]
       (if @did-once
         []
         [(do (reset! did-once true)
              (ability-to-action [(select-one-of-type obs 28)]
                                 3682
                                 ))])))))

(def build-tech-lab-on-starport
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs _]
       (if @did-once
         []
         [(do (reset! did-once true)
              (ability-to-action [(:tag (second (sort-by
                                                 (comp :x :pos)
                                                 (select-all-units-of-types obs 28))))]
                                 3683
                                 ))])))))

(def build-tech-lab-on-factory
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs _]
       (if @did-once
         []
         [(do (reset! did-once true)
              (ability-to-action [(:tag (first (sort-by
                                                 (comp :x :pos)
                                                 (select-all-units-of-types obs 27))))]
                                 3682
                                 ))])))))

(def build-reactor-on-factory
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs _]
       (if @did-once
         []
         [(do (reset! did-once true)
              (ability-to-action [(:tag (second (sort-by
                                                 (comp :x :pos)
                                                 (select-all-units-of-types obs 27))))]
                                 3683
                                 ))])))))

(def build-reactor-on-barracks
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs _]
       (if @did-once
         []
         [(do (reset! did-once true)
              (ability-to-action [(:tag (first (sort-by
                                                 (comp :y :pos)
                                                 (select-all-units-of-types obs 21))))]
                                 3683
                                 ))])))))

(def build-tech-lab-on-barracks
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs _]
       (if @did-once
         []
         [(do (reset! did-once true)
              (ability-to-action [(:tag (nth (sort-by
                                                (comp :y :pos)
                                                (select-all-units-of-types obs 21))
                                             1))]
                                 3682
                                 ))])))))

(def build-tech-lab-on-barracks-2
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs _]
       (if @did-once
         []
         [(do (reset! did-once true)
              (ability-to-action [(:tag (nth (sort-by
                                              (comp :y :pos)
                                              (select-all-units-of-types obs 21))
                                             2))]
                                 3682
                                 ))])))))

(def build-sensor-tower
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs _]
       (if @did-once
         []
         [(do (reset! did-once true)
              (ability-to-action [(select-one-of-type obs 45)]
                                 326
                                 24 30))])))))

(def build-missile-turret
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs _]
       (if @did-once
         []
         [(do (reset! did-once true)
              (ability-to-action [(select-one-of-type obs 45)]
                                 323
                                 24 29))])))))

(def build-bunker
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs _]
       (if @did-once
         []
         [(do (reset! did-once true)
              (ability-to-action [(select-one-of-type obs 45)]
                                 324
                                 22 33))])))))

(def build-armory
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs _]
       (if @did-once
         []
         [(do (reset! did-once true)
              (ability-to-action [(select-one-of-type obs 45)]
                                 331
                                 19 33))])))))

(def build-fusion-core
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs _]
       (if @did-once
         []
         [(do (reset! did-once true)
              (ability-to-action [(select-one-of-type obs 45)]
                                 333
                                 19 30))])))))

(def build-ghost-academy
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs _]
       (if @did-once
         []
         [(do (reset! did-once true)
              (ability-to-action [(select-one-of-type obs 45)]
                                 327
                                 60 35))])))))


(do-sc2 (fn [obs _] (def obs obs) []) {:run-until-fn (run-for 1)})
(defn build-unit-name-with-name [obs build with]
   (let [build-ability-id (:ability-id (cljsc2.clj.build-order/get-unit-type-by-name build))
         builder-type-id (:unit-id (cljsc2.clj.build-order/get-unit-type-by-name with))
         builder-tags (map :tag (select-all-units-of-types obs builder-type-id))]
     (map (fn [builder-tag] (ability-to-action [builder-tag] build-ability-id))
          builder-tags)))

(defn build [build with]
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs _]
       (if @did-once
         []
         (do (reset! did-once true)
             (build-unit-name-with-name obs build with)))))
   {:run-until-fn (run-for 200)}))

(build "siegetank" "factory")
(build "hellion" "factory")
(build "widowmine" "factory")
(build "marauder" "barracks")
(build "raven" "starport")
(build "banshee" "starport")
(build "medivac" "starport")
(build "vikingfighter" "starport")
(build "reaper" "barracks")
(build "ghost" "barracks")


(def build-more-supply-somewhere-else-2
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs conn]
       (if @did-once
         (build-marines obs)
         (do (reset! did-once true)
             (map-indexed (fn [index unit-tag]
                            (-> [unit-tag]
                                (ability-to-action 319 60 (+ 20 (* index 2)))))
                          (take 10 (select-all-of-types obs 45)))))))))

(build "liberator" "starport")
(build "helliontank" "factory")
(build "thor" "factory")
(build "cyclone" "factory")
(build "battlecruiser" "starport")

(defn do-ability-name-with-name [obs build with]
  (let [build-ability-id (:ability-id (cljsc2.clj.build-order/get-ability-type-by-name build))
        builder-type-id (:unit-id (cljsc2.clj.build-order/get-unit-type-by-name with))
        builder-tags (map :tag (select-all-units-of-types obs builder-type-id))]
    (map (fn [builder-tag] (ability-to-action [builder-tag] build-ability-id))
         builder-tags)))

(defn do-research [build with]
  (do-sc2
   (let [did-once (atom false)]
     (fn [obs _]
       (if @did-once
         []
         (do (reset! did-once true)
             (do-ability-name-with-name obs build with)))))
   {:run-until-fn (run-for 200)}))

(do-research "research stim" "barrackstechlab")

(do-research "research combatshield" "barrackstechlab")

(do-research "research concussive" "barrackstechlab")

(do-research "research drill" "factorytechlab")

(do-research "research rapid fire" "factorytechlab")

(do-research "research preigniter" "factorytechlab")

(do-sc2
 (fn [obs _] (def obs obs) [])
 {:run-until-fn (run-for 100)})

(cljsc2.clj.build-order/get-unit-types-by-name "barrackstechlab")

(cljsc2.clj.build-order/get-unit-type-by-name "hell")

(cljsc2.clj.build-order/get-ability-type-by-name "research preigniter")

(cljsc2.clj.build-order/get-ability-types-by-name "rally")

(cljsc2.clj.build-order/get-unit-type-by-id 40)

(require '[taoensso.nippy :as nippy])

(comment (nippy/freeze-to-file "unit-type-abilities.nippy" @unit-type-data))

(nippy/thaw-from-file "unit-type-abilities.nippy")

(def unit-type-data (atom {}))

(def ability-type-data (atom {}))

(let [unit-types (->>
                  obs
                  :raw-data
                  :units
                  (filter (comp #{1} :owner))
                  (filter :unit-type)
                  (map :unit-type)
                  set)]
  (map (fn [unit-type-id]
         (swap! unit-type-data
                update-in
                [unit-type-id :abilities]
                (fn [type-data]
                  (set (clojure.set/union type-data (-> (request-abilities
                                                         [(select-one-of-type obs unit-type-id)] true)
                                                        :query
                                                        :abilities
                                                        first
                                                        :abilities
                                                        set))))))
       unit-types))

(map (fn [[unit-type-id {:keys [abilities]}]]
       (let []
         (merge
          {:db/id unit-type-id
           :unit-type/abilities (map :ability-id abilities)
           }
          ability-info)))
     (take 2 @unit-type-data))

(->> @unit-type-data
     vals
     (map :abilities)
     (apply clojure.set/union)
     (map :ability-id)
     (map cljsc2.clj.build-order/get-ability-type-by-id)
     (mapcat keys)
     set
     )

(def db
  (ds/create-conn
   {:unit/type {:db/cardinality :db.cardinality/many
                :db/valueType :db.type/ref}
    :unit-type/abilities {:db/cardinality :db.cardinality/many
                          :db/valueType :db.type/ref}
    }))

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



(ds/transact! db
              (mapcat (fn [[unit-type-id {:keys [abilities]}]]
                (let [ability-tx (map (fn [{:keys [ability-id requires-point]}]
                                        (let [ability-info (cljsc2.clj.build-order/get-ability-type-by-id ability-id)]
                                          (merge (clojure.set/rename-keys
                                                  (select-keys ability-info
                                                               (keys ability-type-keymap))
                                                  ability-type-keymap)
                                                 (if requires-point
                                                   {:db/id (+ 880000 ability-id)
                                                    :ability-type/requires-point requires-point}
                                                   {:db/id (+ 880000 ability-id)}
                                                   ))))
                                      abilities)
                      unit-type-info (cljsc2.clj.build-order/get-unit-type-by-id unit-type-id)]
                  (concat [(merge (clojure.set/rename-keys
                                  (select-keys unit-type-info
                                               (keys unit-type-keymap))
                                  unit-type-keymap)
                                 {:db/id (+ 990000 unit-type-id)
                                  :unit-type/abilities (map :db/id ability-tx)})
                           ]
                          ability-tx)))
                      (take 1000 @unit-type-data)))



(merge (clojure.set/rename-keys
        ability-data
        ability-type-keymap)
       {:db/id (+ 880000 ability-id)})


(merge (clojure.set/rename-keys
        {:add-on-tag :unit/add-on-tag,
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
         :owner :unit/owner}
        {:db/id tag}))

(ds/q '[:find ?name
        :in $ ?includes
        :where
        [?id :unit-type/name ?name]
        [?id :unit-type/abilities ?ability-id]
        [?ability-id :ability-type/name ?ability-name]
        [(?includes ?ability-name "Cloak")]]
      @db
      clojure.string/includes?)
