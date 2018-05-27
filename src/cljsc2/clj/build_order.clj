(ns cljsc2.clj.build-order
  (:require
   [datascript.core :as ds]
   [taoensso.timbre :as timbre]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as st]
   [clojure.spec.gen.alpha :as gen]
   [lambda-ml.clustering.dbscan :as dbscan]
   [lambda-ml.distance :as dist :refer [euclidean]])
  (:use
   cljsc2.clj.core
   cljsc2.clj.rules
   cljsc2.clj.notebook.core
   cljsc2.clj.game-parsing
   cljsc2.clj.build-order))

(defn run-query [env assoc-key {:keys [in query find transform-result]
                                :or {transform-result ffirst}}]
  (transform-result (apply (partial ds/q (concat
                                          (concat [:find] (or find [(symbol (str "?" (name assoc-key)))]))
                                          (concat '[:in $] (keys in))
                                          (concat [:where] query))
                                    (:knowledge env)
                                    )
                           (for [fun (vals in)]
                             (fun env)))))

(defn remove-occupied-casters [{:keys [actions ability-order-already-issued non-mining-scvs] :as env} _]
  (let [occupied-casters (set (flatten (map #(get-in % [:SC2APIProtocol.sc2api$Action/action-raw
                                                       :SC2APIProtocol.raw$ActionRaw/action
                                                       :SC2APIProtocol.raw$ActionRaw/unit-command
                                                       :SC2APIProtocol.raw$ActionRawUnitCommand/unit-tags])

                                            actions)))]
    (update env :available-casters (fn [casters]
                                     (clojure.set/difference casters
                                                             occupied-casters
                                                             ability-order-already-issued
                                                             non-mining-scvs)))))

(defn update-build-actions [{:keys [ability-id connection do-times available-casters
                                    available-building-positions footprint-radius
                                    is-building
                                    is-refinery
                                    knowledge
                                    connection] :as env} _]
  (let [{:keys [actions positions]}
        (reduce (fn [{:keys [positions actions] :as acc} caster-tag]
                  (if is-building
                    (if is-refinery
                      (let [free-vespenes (clojure.set/difference
                                           (set (ds/q '[:find [?target ...]
                                                        :where
                                                        [?target :unit/vespene-contents]
                                                        [?target :unit/unit-type 342]]
                                                      knowledge))
                                           (set (ds/q '[:find [?target ...]
                                                        :where
                                                        [?target :unit/vespene-contents]
                                                        [?target :unit/unit-type 342]
                                                        [?target :unit/x ?at-x]
                                                        [?other :unit/x ?at-x]
                                                        [(not= ?target ?other)]]
                                                      knowledge))
                                           )]
                        (when (not (empty? free-vespenes))
                          (assoc acc
                                 :actions
                                 (conj actions (ability-to-action [caster-tag] ability-id
                                                                  (first free-vespenes)
                                                                  {})))))
                      (let [[[found-x found-y :as found-position] remaining-positions]
                            (find-location connection caster-tag ability-id positions)]
                        (if (and found-position found-x found-y)
                          (-> acc
                              (assoc :actions (conj actions (ability-to-action [caster-tag] ability-id found-x found-y {})))
                              (assoc :positions (clojure.set/difference
                                                 (set remaining-positions)
                                                 (set (positions-around
                                                       found-x
                                                       found-y
                                                       (Math/round footprint-radius))))))
                          (reduced acc))))
                    (assoc acc :actions (conj actions (ability-to-action [caster-tag] ability-id {})))))
                {:positions available-building-positions
                 :actions []}
                (take do-times available-casters))]
    (-> env
        (update :actions (fn [actions-so-far] (concat actions-so-far actions)))
        (assoc :available-building-positions positions))))

(defn get-unit-type-count [knowledge unit-type]
  (ffirst (ds/q '[:find (count ?unit-tag)
                  :in $ ?unit-type
                  :where
                  [?ut-e :unit-type/name ?unit-type]
                  [?ut-e :unit-type/unit-id ?ut-id]
                  [?unit-tag :unit/unit-type ?ut-id]]
                knowledge
                unit-type)))

(defmulti goal-evaluator :type)

(defmethod goal-evaluator :until-count
  [{:keys [unit-type amount]}]
  (fn [starting-knowledge]
    (let [starting-count (get-unit-type-count starting-knowledge unit-type)]
      (fn [{:keys [next-knowledge]}]
        (>= (- (or (get-unit-type-count next-knowledge unit-type) 0)
               (or starting-count 0))
            amount)))))

(defmethod goal-evaluator :env
  [{:keys [unit-type amount evaluator]}]
  (fn [starting-knowledge]
    (fn [env]
      (apply evaluator [env]))))

(defn add-actions-for-plan-spec [[planner env] plan-spec]
  [planner (reduce (fn [env [process-step-key value]]
                     (let [assoc-key (keyword (clojure.string/replace (name process-step-key) "-query" ""))]
                       (cond
                         (map? value) (assoc-in env (or (:as value) [assoc-key]) (run-query env assoc-key value))
                         (fn? value) (value env planner)
                         (keyword? value) (assoc env assoc-key (get env value))
                         :else (assoc env assoc-key value))))
                   env
                   (partition 2 plan-spec))])

(def unit-has-order-rule
  '[[(unit-has-order ?unit-tag ?ability-id)
     [?unit-tag :unit/orders ?order]
     [?order :order/ability-id ?ability-id]
     ]])

(defn initialize-active-plans [plans starting-knowledge]
  (let [initialized-plans (->> plans
                               (map (fn [plan]
                                      {:initialized-goals (map (fn [goal]
                                                                 ((goal-evaluator goal) starting-knowledge))
                                                               (:action-goals plan))
                                       :plan plan})))]
    initialized-plans))

(defn plans-executor [planner]
  (fn [starting-observation connection]
    (let [game-info (cljsc2.clj.core/send-request-and-get-response-message
                     connection
                     #:SC2APIProtocol.sc2api$RequestGameInfo{:game-info {}})
          starting-knowledge (ds/db-with
                              knowledge-base
                              (obs->facts starting-observation game-info))]
      (swap! planner assoc :active-plans (initialize-active-plans (:plans @planner) starting-knowledge))
      (swap! planner assoc :plans [])
      (fn [latest-observation connection]
        (let [latest-knowledge (ds/db-with
                                knowledge-base
                                (obs->facts latest-observation game-info))
              env (do (swap! planner update :env #(merge {:actions []
                                                          :available-building-positions (cljsc2.clj.game-parsing/positions-near-unit-type "CommandCenter" latest-knowledge)
                                                          :connection connection
                                                          :knowledge latest-knowledge}
                                                         %))
                      (:env @planner))]
          (def knowledge latest-knowledge)
          (let [active-plans (:active-plans @planner)
                env (if (seq? active-plans)
                      (second (->> active-plans
                                   (map (comp :action-spec :plan))
                                   (reduce add-actions-for-plan-spec
                                           [planner env])))
                      env)
                env (reduce (fn [env {:keys [plan initialized-goals]}]
                              (if (and (every? true? (map (fn [goal] (goal {:next-knowledge latest-knowledge
                                                                            :env env}))
                                                          initialized-goals))
                                       (:goals-succeeded-action plan))
                                (do (apply (:goals-succeeded-action plan) [env planner])
                                    env)
                                env))
                            env
                            active-plans)]
            (swap!
             planner
             (fn [p]
               {:active-plans (concat (->> active-plans
                                    (filter
                                     (fn [{:keys [plan initialized-goals]}]
                                       (not (every? true?
                                                    (map (fn [goal]
                                                           (goal {:next-knowledge latest-knowledge
                                                                  :env env}))
                                                         initialized-goals)))))
                                    (remove (fn [{:keys [plan initialized-goals]}]
                                              (and (every? true? (map (fn [goal] (goal latest-knowledge))
                                                                      initialized-goals))
                                                   (:remove-after-goal-attained plan)))))
                                      (or (initialize-active-plans (:new-plans @planner) latest-knowledge) []))
                :env (-> env
                         (dissoc :knowledge)
                         (dissoc :actions)
                         (dissoc :available-building-positions))}))
            (vec (set (:actions env)))))))))

(defn finished? [planner]
  (fn [_ _]
    (not (> (count (remove (fn [p] (= :continuous (:remove-after-goal-attained p)))
                           (:active-plans @planner))) 0))))

(defn inactive-scvs [knowledge]
  (clojure.set/difference
   (set (ds/q '[:find [?scv ...]
                :where
                [?scv :unit/unit-type 45]]
              knowledge))
   (set (ds/q '[:find [?scv ...]
                :where
                [?scv :unit/unit-type 45]
                [?scv :unit/orders]]
              knowledge))))

(defn harvestable-units [knowledge]
  (ds/q '[:find ?unit ?ideal ?harvesters
          :where
          [?unit :unit/ideal-harvesters ?ideal]
          [?unit :unit/assigned-harvesters ?harvesters]
          [(not= ?ideal 0)]]
        knowledge))

(defn harvesting-units [knowledge]
  (set (ds/q '[:find [?worker ...]
               :in $ [?ability-id ...] %
               :where
               (unit-has-order ?worker ?ability-id)]
             knowledge
             [295 296]
             unit-has-order-rule)))

(defn execute-plans [& plans]
  (let [contained-planner (first (filter #(instance? clojure.lang.Atom %) plans))
        planner (if contained-planner
                  (do (swap! contained-planner assoc :plans (apply concat (remove #(instance? clojure.lang.Atom %) plans)))
                      contained-planner)
                  (atom {:plans (apply concat plans)
                         :env {}}))
        executor-start-step-fn (plans-executor planner)
        executor-step-fn (executor-start-step-fn
                          (get-in (cljsc2.clj.core/request-observation connection) [:observation :observation])
                          connection)]
    [(run-sc
      executor-step-fn
      (fn [_] (finished? planner))
      {}) planner]))


(defn update-keep-gas-mined [{:keys [knowledge actions] :as env} _]
  (let [occupied-casters (set (flatten (map #(get-in % [:SC2APIProtocol.sc2api$Action/action-raw
                                                       :SC2APIProtocol.raw$ActionRaw/action
                                                       :SC2APIProtocol.raw$ActionRaw/unit-command
                                                       :SC2APIProtocol.raw$ActionRawUnitCommand/unit-tags])

                                           actions)))
        available-workers (clojure.set/difference (harvesting-units knowledge)
                                                  occupied-casters)
        added-mine-actions (reduce
                            (fn [{:keys [actions available-workers]} [refinery-tag ideal assigned]]
                              {:actions (concat actions (map #(ability-to-action [%] 295 refinery-tag {})
                                                             (take (- ideal assigned) available-workers)))
                               :available-workers (drop (- ideal assigned) available-workers)})
                            {:actions actions
                             :available-workers available-workers}
                            (filter (fn [[_ ideal assigned]]
                                      (and (= ideal 3)
                                           (< assigned 3)))
                                    (harvestable-units knowledge)))]
    (assoc env :actions (:actions added-mine-actions))))

(defn keep-gas-mined []
  [{:action-spec [:keeping-gas-mined update-keep-gas-mined]
    :action-goals [{:type :env
                    :evaluator (fn [_] false)}]
    :remove-after-goal-attained :continuous}])

(defn build [unit-type & {:keys [until-count
                                 near
                                 remove-after-goal-attained
                                 whenever-goals-succeed]
                          :or {until-count 1 whenever-goals-succeed (fn [_ _])}}]
  [{:action-spec
    [:unit-type unit-type
     :current-count (fn [env _]
                      (assoc env :current-count
                             (or (get-unit-type-count (:knowledge env)
                                                      unit-type)
                                 0)))
     :non-mining-scvs (fn [{:keys [knowledge] :as env} _]
                        (let [mining-scvs (clojure.set/union
                                           (set (ds/q '[:find [?scv ...]
                                                        :in $ %
                                                        :where
                                                        (units-of-type "SCV" ?scv)
                                                        (currently-doing ?scv 296)]
                                                      knowledge
                                                      (concat units-of-type-rule
                                                              currently-doing-rule)))
                                           (set (ds/q '[:find [?scv ...]
                                                        :in $ %
                                                        :where
                                                        (units-of-type "SCV" ?scv)
                                                        (currently-doing ?scv 295)]
                                                      knowledge
                                                      (concat units-of-type-rule
                                                              currently-doing-rule))))
                              all-scvs (set (ds/q '[:find [?scv ...]
                                                    :in $ %
                                                    :where
                                                    (units-of-type "SCV" ?scv)]
                                                  knowledge
                                                  units-of-type-rule))]
                          (assoc env :non-mining-scvs (clojure.set/difference all-scvs mining-scvs))))
     :ability-id-query {:in '{?ability-name :unit-type}
                        :query '[[?build-me :unit-type/name ?ability-name]
                                 [?build-me :unit-type/ability-id ?ability-id]]}
     :is-building-query {:in '{?ability-id :ability-id}
                         :query '[[?ab-e-id :ability-type/is-building ?is-building]
                                  [?ab-e-id :ability-type/id ?ability-id]]}
     :is-refinery-query (fn [{:keys [ability-id] :as env} _]
                          (assoc env :is-refinery (= ability-id 320)))
     :footprint-radius-query {:in '{?ability-id :ability-id}
                              :query '[[?a :ability-type/id ?ability-id]
                                       [?a :ability-type/footprint-radius ?footprint-radius]]}
     :available-casters-query {:in '{?ability-id :ability-id}
                               :query '[[?unit-tag :unit/unit-type ?built-unit-type]
                                        [?t :unit-type/unit-id ?built-unit-type]
                                        [?t :unit-type/name ?name]
                                        [(+ 880000 ?ability-id) ?ab-e-id]
                                        [?t :unit-type/abilities ?ab-e-id]]
                               :find ['[?unit-tag ...]]
                               :transform-result set}
     :ability-order-already-issued-query {:in '{?ability-id :ability-id}
                                          :query '[[?unit-tag :unit/orders ?order]
                                                   [?unit-tag :unit/unit-type ?type-id]
                                                   [?order :order/ability-id ?ability-id]]
                                          :find '[?unit-tag ?order]
                                          :transform-result (fn [result] (set (map first result)))}
     :available-casters remove-occupied-casters
     :food-available-query {:query
                            '[[?l :player-common/food-used ?food-used]
                              [?l :player-common/food-cap ?food-cap]
                              [(- ?food-cap ?food-used) ?food-available]]
                            :from ['[?food-available]]}
     :do-times (if (= unit-type "SupplyDepot")
                 (fn [{:keys [food-available ability-order-already-issued] :as env} _]
                   (assoc env :do-times (- (int (/ (- 13 food-available) 10))
                                           (count ability-order-already-issued))))
                 (fn [{:keys [current-count is-building ability-order-already-issued] :as env} _]
                   (assoc env :do-times (- (- until-count current-count)
                                           (count ability-order-already-issued)))))
     :actions update-build-actions]
    :action-goals [{:unit-type unit-type
                    :type :until-count
                    :amount (if (= unit-type "SupplyDepot") 21 until-count)}]
    :remove-after-goal-attained (if (= unit-type "SupplyDepot") :continuous remove-after-goal-attained)
    :goals-succeeded-action whenever-goals-succeed}])

(defn select [unit-type & {:keys [and-do at-least whenever-goals-succeed remove-after-goal-attained]
                           :or {at-least 1
                                whenever-goals-succeed (fn [& _])
                                remove-after-goal-attained true}}]
  (concat
   [{:action-spec [:unit-type unit-type
                         :selected-query {:in '{?unit-type :unit-type}
                                          :query '[[?ut-e :unit-type/name ?unit-type]
                                                   [?ut-e :unit-type/unit-id ?ut-id]
                                                   [?unit-tag :unit/unit-type ?ut-id]]
                                          :find ['[?unit-tag ...]]
                                          :as [:selected unit-type]
                                          :transform-result set}]
           :action-goals [{:type :env
                           :unit-type unit-type
                           :at-least-count at-least
                           :evaluator (fn [{:keys [env]}]
                                        (>= (or (count (get-in env [:selected unit-type])) 0)
                                            at-least))}]
           :goals-succeeded-action whenever-goals-succeed
     :remove-after-goal-attained remove-after-goal-attained}]
   and-do))

(defn update-attack-actions [{:keys [attack-at attack-with] :as env} _]
  (let [attack-actions (mapcat (fn [[_ unit-tags]] (map #(ability-to-action [%] 23 (first attack-at) (second attack-at) {})
                                                        unit-tags))
                               attack-with)]
    (update env :actions (fn [actions-so-far] (concat actions-so-far attack-actions)))))

(defn resolve-location [location]
  (fn [{:keys [knowledge connection] :as env} _]
    (let [{:keys [x y]} (case location
                          :enemy-base (ffirst (ds/q '[:find [?locations ...]
                                                      :where [_ :game-info/start-locations ?locations]]
                                                    knowledge))
                          [15 15])]
      (assoc env :attack-at [x y]))))

(defn attack [& {:keys [at-location
                        remove-after-goal-attained
                        whenever-goals-succeed] :or
                 {remove-after-goal-attained true
                  whenever-goals-succeed (fn [_ _])}}]
  [{:action-spec [:attack-at (resolve-location at-location)
                  :attack-with :selected
                  :actions update-attack-actions]
    :action-goals [{:type :env
                    :evaluator (fn [_] true)}]
    :goals-succeeded-action whenever-goals-succeed
    :remove-after-goal-attained remove-after-goal-attained}])

(defn add-plan [plan]
  (fn [env planner]
    (swap! planner update :new-plans concat plan)))

(defn add-plans [& plans]
  (fn [env planner]
    (swap! planner update :new-plans concat (apply concat plans))))

(defn x-y-army-units [knowledge]
  (ds/q '[:find ?x ?y
          :where
          [?u :unit/x ?x]
          [?u :unit/y ?y]
          [?u :unit/unit-type ?unit-type]
          [(not= ?unit-type 45)]
          [?ut-e-id :unit-type/unit-id ?unit-type]
          [?ut-e-id :unit-type/weapons]
          ]
        knowledge))

(defn move-camera-to-coords-action [x y]
  #:SC2APIProtocol.sc2api$Action
  {:action-raw #:SC2APIProtocol.raw$ActionRaw
   {:action #:SC2APIProtocol.raw$ActionRaw
    {:camera-move #:SC2APIProtocol.raw$ActionRawCameraMove
     {:center-world-space #:SC2APIProtocol.common$Point{:x x :y y}}}}})

(defn update-camera-follow-actions [{:keys [actions knowledge] :as env} _]
  (let [xys (x-y-army-units knowledge)]
    (case (count xys)
      0 env
      1 (assoc env :actions (concat [(move-camera-to-coords-action
                                      (ffirst xys)
                                      (second (first xys)))]
                                    actions))
      (let [identified-clusters (dbscan/dbscan euclidean 5 2 xys)
            [x y] (if (not (empty? identified-clusters))
                    (->>
                     identified-clusters ;;identify clusters
                     (group-by val)      ;;group them by group id
                     (apply max-key (comp count val)) ;;find the biggest group
                     val ;;take the groups
                     (map (comp first)) ;;get their x-y
                     ((fn [c]
                        [(lambda-ml.core/mean (map first c))
                         (lambda-ml.core/mean (map second c))])) ;;calculate mean
                     )
                    [(ffirst xys) (second (first xys))])]
        (assoc env :actions (concat [(move-camera-to-coords-action x y)]
                                    actions))))))

(defn camera-follow-army [& {:keys
                             [remove-after-goal-attained
                              whenever-goals-succeed] :or
                             {remove-after-goal-attained true
                              whenever-goals-succeed (fn [_ _])}}]
  [{:action-spec [:actions update-camera-follow-actions]
    :action-goals [{:type :env
                    :evaluator (fn [_] false)}]
    :goals-succeeded-action whenever-goals-succeed
    :remove-after-goal-attained :continuous}])

#_(execute-plans
 (build "SCV" :until-count 25)
 (build "Refinery")
 (build "SupplyDepot")
 (build "Barracks"
        :whenever-goals-succeed (add-plans
                                 (build "Factory")
                                 (build "Refinery" :until-count 2)
                                 (build "Cyclone" :until-count 3)
                                 (select "Cyclone" :at-least 3
                                         :and-do (select "Marine" :at-least 15)
                                         :whenever-goals-succeed
                                         (add-plan (attack :at-location :enemy-base)))
                                 ))
 (build "Marine" :until-count 5)
 (keep-gas-mined)
 (camera-follow-army))
