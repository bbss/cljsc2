(ns cljsc2.clj.build-order-test
  (:require
   [datascript.core :as ds]
   [taoensso.timbre :as timbre])
  (:use
   cljsc2.clj.core
   cljsc2.clj.rules
   cljsc2.clj.notebook.core
   cljsc2.clj.game-parsing
   cljsc2.clj.build-order))

(comment
  :scv build one scv
  (build :scvs)
  (build "supply-depot")
  (build 'barracks)
  (build "marines")

  (expand)
  ;; Max per amount of expands and strategy
  (rally barracks)

  (attack-at 50 supp))

(defn unit-type-for-name [name]
  (first (ds/q `[:find [?id ...]
                :where
                [?e :unit-type/name ~name]
                [?e :unit-type/unit-id ?id]]
              knowledge-base)))

(defn test-action-planner [planner-fn goal-reached]
  (let [conn (get-conn server-db 5000)]
    (run-on-conn server-db 5000
                 conn
                 planner-fn
                 200 5000
                 {:run-until-fn (fn [starting-obs]
                                  (let [goal-reached-pred (goal-reached starting-obs)
                                        run-for-pred ((run-for 5000) starting-obs)]
                                    (fn [next-obs _]
                                      (or (goal-reached-pred next-obs _)
                                          (run-for-pred next-obs _)))))
                  :run-ended-cb (juxt (fn [_ _]
                                        (quick-load conn))
                                      (run-ended server-db (->> @server-db
                                                                :run/by-id
                                                                keys
                                                                (reduce max)) 5000))})))

(defn more-unit-type-goal-reached [unit-type]
  (fn [starting-obs]
   (let [starting-count (->> starting-obs
                                 :raw-data
                                 :units
                                 (filter (comp #{1} :owner))
                                 (filter (comp #{unit-type} :unit-type))
                                 count)]
     (fn [next-obs _]
       (> (->> next-obs
               :raw-data
               :units
               (filter (comp #{1} :owner))
               (filter (comp #{unit-type} :unit-type))
               count)
          starting-count)))))

(test-action-planner
 (fn [observation connection]
   (create-actions knowledge-base [build-scvs] observation connection))
 (more-unit-type-goal-reached (unit-type-for-name "SCV")))

(test-action-planner
 (fn [observation connection]
   (create-actions knowledge-base [build-supply-depots] observation connection))
 (more-unit-type-goal-reached (unit-type-for-name "SupplyDepot")))

(test-action-planner
 (fn [observation connection]
   (create-actions knowledge-base [build-supply-depots
                                   build-barracks] observation connection))
 (more-unit-type-goal-reached (unit-type-for-name "Barracks")))

(test-action-planner
 (fn [observation connection]
   (create-actions knowledge-base [build-supply-depots
                                   build-barracks
                                   build-marines] observation connection))
 (more-unit-type-goal-reached (unit-type-for-name "Marine")))
