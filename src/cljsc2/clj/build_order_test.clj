(ns cljsc2.clj.build-order-test
  (:require
   [datascript.core :as ds]
   [taoensso.timbre :as timbre]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as st]
   [clojure.spec.gen.alpha :as gen])
  (:use
   cljsc2.clj.core
   cljsc2.clj.rules
   cljsc2.clj.notebook.core
   cljsc2.clj.game-parsing
   cljsc2.clj.build-order))

(defn test-action-planner [planner-fn goal-reached]
  (let [conn (get-conn server-db 5000)]
    (run-on-conn server-db 5000
                 conn
                 planner-fn
                 500 5000
                 {:run-until-fn (fn [starting-obs]
                                  (let [goal-reached-pred (goal-reached starting-obs)
                                        run-for-pred ((run-for 15000) starting-obs)]
                                    (fn [next-obs _]
                                      (or (goal-reached-pred next-obs _)
                                          (run-for-pred next-obs _)))))
                  :run-ended-cb (juxt (fn [_ _]
                                        (quick-load conn))
                                      (run-ended server-db (->> @server-db
                                                                :run/by-id
                                                                keys
                                                                (reduce max)) 5000))})))

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

(defn remove-occupied-casters [{:keys [actions] :as env} _]
  (let [occupied-casters (set (map #(get-in % [:SC2APIProtocol.sc2api$Action/action-raw
                                               :SC2APIProtocol.raw$ActionRaw/action
                                               :SC2APIProtocol.raw$ActionRaw/unit-command
                                               :SC2APIProtocol.raw$ActionRawUnitCommand/unit-tags])

                                   actions))]
    (update env :available-casters (fn [casters]
                                     (clojure.set/difference casters
                                                             occupied-casters)))))

(defn add-ability-already-issued [{:keys [ability-id] :as env}]
  (->> env
       (map (fn [[unit-tag orders]]
              [unit-tag (count (filter (comp #{ability-id}
                                             :ability-id)
                                       (if (coll? orders)
                                         orders
                                         [orders])))]))
       (filter (fn [[_ building-count]]
                 (> 0 building-count)))))

(defn update-build-actions [{:keys [ability-id connection do-times available-casters
                                    available-building-positions footprint-radius
                                    is-building
                                    is-refinery
                                    knowledge] :as env} _]
  (def knowledge knowledge)
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

(defmethod goal-evaluator :additional-count
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
                         :else (assoc env assoc-key value))))
                   env
                   (partition 2 plan-spec))])

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
    (let [starting-knowledge (ds/db-with
                              knowledge-base
                              (obs->facts starting-observation))]
      (fn [latest-observation connection]
        (let [initialized-plans (initialize-active-plans (:plans @planner) starting-knowledge)
              latest-knowledge (ds/db-with
                                knowledge-base
                                (obs->facts latest-observation))
              env (do (swap! planner update :env #(merge {:actions []
                                                          :available-building-positions (cljsc2.clj.game-parsing/positions-near-unit-type "CommandCenter" latest-knowledge)
                                                          :connection connection
                                                          :knowledge latest-knowledge}
                                                         %))
                      (:env @planner))]
          (let [env (if (seq? initialized-plans)
                      (second (->> initialized-plans
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
                            initialized-plans)]
            (swap!
             planner (fn [p]
                       {:plans (concat (->> initialized-plans
                                            (filter
                                             (fn [{:keys [plan initialized-goals]}]
                                               (not (every? true?
                                                            (map (fn [goal]
                                                                   (goal {:next-knowledge latest-knowledge
                                                                          :env env}))
                                                                 initialized-goals)))))
                                            (remove (fn [{:keys [plan initialized-goals]}]
                                                      (when (and (every? true? (map (fn [goal] (goal {:next-knowledge latest-knowledge
                                                                                                      :env env}))
                                                                                    initialized-goals))
                                                                 (:remove-after-goal-attained plan))
                                                        (timbre/debug "removing "
                                                                      plan
                                                                      (every? true? (map (fn [goal] (goal {:next-knowledge latest-knowledge
                                                                                                           :env env}))
                                                                                         initialized-goals))
                                                                      (:remove-after-goal-attained plan)))
                                                      (and (every? true? (map (fn [goal] (goal latest-knowledge))
                                                                              initialized-goals))
                                                           (:remove-after-goal-attained plan))))
                                            (map :plan))
                                       (or (first (:new-plans p)) []))
                        :env (-> env
                                 (dissoc :knowledge)
                                 (dissoc :actions))}))
            (vec (set (:actions env)))))))))

(defn execute-plans [plans]
  (let [planner (atom {:plans plans
                       :env {}})
        connection (cljsc2.clj.notebook.core/get-conn cljsc2.clj.notebook.core/server-db 5000)
        executor-start-step-fn (plans-executor planner)
        executor-step-fn (executor-start-step-fn
                          (get-in (cljsc2.clj.core/request-observation connection) [:observation :observation])
                          connection)]
    (test-action-planner
     executor-step-fn
     (fn [_] (fn [_ _]
               (not (> (count (:plans @planner)) 0)))))))

(defn build [unit-type & {:keys [additional-count
                                 near
                                 remove-after-goal-attained]
                          :or {additional-count 1}}]
  [{:action-spec [:unit-type unit-type
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
                  :available-casters remove-occupied-casters
                  :food-available-query {:query
                                         '[[?l :player-common/food-used ?food-used]
                                           [?l :player-common/food-cap ?food-cap]
                                           [(- ?food-cap ?food-used) ?food-available]]
                                         :from ['[?food-available]]}
                  :ability-order-already-issued-query {:query '[[?unit-tag :unit/orders ?orders]]
                                                       :find '[?unit-tag ?orders]
                                                       :transform-result add-ability-already-issued}
                  :do-times (if (= unit-type "SupplyDepot")
                              (fn [{:keys [food-available ability-order-already-issued] :as env} _]
                                (assoc env :do-times (inc (- (int (/ (- 13 food-available) 10))
                                                             (count ability-order-already-issued)))))
                              (fn [{:keys [is-building] :as env} _]
                                (assoc env :do-times (if is-building 1 5))))
                  :actions update-build-actions]
    :action-goals [{:unit-type unit-type
                    :type :additional-count
                    :amount (if (= unit-type "SupplyDepot") 21 additional-count)}]
    :remove-after-goal-attained (if (= unit-type "SupplyDepot") false remove-after-goal-attained)
    :goals-succeeded-action (fn [_ _]
                              (println "finished " unit-type))}])

(defn select [unit-type & {:keys [at-least whenever-goals-succeed remove-after-goal-attained] :or {at-least 1
                                                                         whenever-goals-succeed (fn [& _])
                                                                         remove-after-goal-attained true}}]
  [{:action-spec [:unit-type unit-type
                  :available-selected-query {:in '{?unit-type :unit-type}
                                             :query '[[?ut-e :unit-type/name ?unit-type]
                                                      [?ut-e :unit-type/unit-id ?ut-id]
                                                      [?unit-tag :unit/unit-type ?ut-id]]
                                             :find ['[?unit-tag ...]]
                                             :as [:available-selected unit-type]
                                             :transform-result set}]
    :action-goals [{:type :env
                    :unit-type unit-type
                    :at-least-count at-least
                    :evaluator (fn [{:keys [env]}]
                                 (>= (or (count (get-in env [:available-selected unit-type])) 0)
                                     at-least))}]
    :goals-succeeded-action whenever-goals-succeed
    :remove-after-goal-attained remove-after-goal-attained}])

(defn update-attack-actions [{:keys [attack-at attack-with] :as env} _]
  (let [attack-actions (map #(ability-to-action [%] 23 (first attack-at) (second attack-at) {})
                            attack-with)]
    (update env :actions (fn [actions-so-far] (concat actions-so-far attack-actions)))))

(defn attack [& {:keys [with-units at-location remove-after-goal-attained] :or
                 {remove-after-goal-attained true}}]
  [{:action-spec [:attack-at at-location
                  :attack-with with-units
                  :actions update-attack-actions]
    :action-goals [{:type :env
                    :evaluator (fn [_] true)}]
    :goals-succeeded-action (fn [env _] (println  "attack succeeded"))
    :remove-after-goal-attained remove-after-goal-attained}])

(execute-plans
 (concat
  (build "SCV" :additional-count 20)
  (build "SupplyDepot")
  (build "Barracks" :additional-count 5)
  (build "Marine" :additional-count 40)
  (select "Marine" :at-least 15
          :whenever-goals-succeed (fn [env planner]
                                    (swap! planner
                                           (fn [p]
                                             (update p :new-plans conj
                                                     (attack :at-location [28 62]
                                                              :with-units (apply clojure.set/union (vals (:available-selected env)))
                                                              :remove-after-goal-attained false))))))))
