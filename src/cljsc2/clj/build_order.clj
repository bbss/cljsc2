(ns cljsc2.clj.build-order
  (:require
   [cljsc2.clj.core :refer [req]]
   [datascript.core :as ds]
   [taoensso.nippy :as nippy]
   [clojure.spec.alpha :as spec]))

(defn abilities [connection]
  (->
   (req connection
        #:SC2APIProtocol.sc2api$RequestData
        {:data #:SC2APIProtocol.sc2api$RequestData
         {:ability-id true}})
   :data
   :abilities))

(defn units [connection]
  (->
   (req connection
        #:SC2APIProtocol.sc2api$RequestData
        {:data #:SC2APIProtocol.sc2api$RequestData
         {:unit-type-id true}})
   :data
   :units))

(defn effects [connection]
  (->
   (req connection
        #:SC2APIProtocol.sc2api$RequestData
        {:data #:SC2APIProtocol.sc2api$RequestData
         {:effect-id true}})
   :data
   :effects))

(defn upgrades [connection]
  (->
   (req connection
        #:SC2APIProtocol.sc2api$RequestData
        {:data #:SC2APIProtocol.sc2api$RequestData
         {:upgrade-id true}})
   :data
   :upgrades))

(defn buffs [connection]
  (->
   (req connection
        #:SC2APIProtocol.sc2api$RequestData
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
  (spec/def ::amount int?)
  (spec/def ::unit-id int?)
  (spec/def ::units-string (spec/coll-of (spec/cat ::amount ::unit-id)))

  (spec/explain-data ::units-string
                     (->>
                      (partition 2)
                      (map (fn [[amount unit-name]]
                             (let [stripped-name (if (clojure.string/ends-with? unit-name "s")
                                                   (subs unit-name 0 (dec (count unit-name)))
                                                   unit-name)]
                               [(Integer/parseInt "1") (get-unit-type-by-name "marine")]
                               )))
                      flatten)))

(spec/def ::datascript-query
  coll
  :find-form
  then :in-form
  then :where-form
  )

(spec/def :find-form
  (spec/cat :find-keyword #{:find}
            :find-argument (spec/or
                            ::lvar
                            ::rule
                            (spec/+
                             ::destructured-find-argument
                             (s/+ ::many-lvars))))
  (or :lvar
      (zero-one :destructured-form
                (zero-one :many-lvars))
      (zero-one :many-lvars)
      )
  )

;;find argument can be

(spec/def ::destructured-find-argument
  (spec/cat (spec/* (spec/coll-of ::lvar :kind vector?))
            (spec/? ::many-lvars)))

(spec/def ::many-lvars #{'...})

(spec/explain-data
 ::destructured-find-argument
 '[?thing ?sup ...])

(spec/def :in-form
  one keyword = :in
  zero-one keyword = $ ;;db
  zero-one keyword = % ;;rules
  * :lvar
  )

(spec/def :where-form
  one keyword = :where
  * (spec/or
     :eav
     :rule
     :binding
     ))

(spec/def ::eav
  coll
  any
  (spec/or symbol? keyword?)
  any
  )

(spec/def ::rule
  (spec/cat :rule-name symbol?
            :arguments (spec/* (spec/or :symbol symbol? :value any?))))

(spec/def ::rule '(thing symboll nil))

(spec/def ::lvar
  (spec/and symbol? #(clojure.string/starts-with? % "?")))

(require '[datascript.core :as ds])
(require '[cljsc2.clj.game-parsing :refer [obs->facts knowledge-base ability-to-action]])
(require 'cljsc2.clj.rules)

(use 'cljsc2.clj.core)

(def sc-process (start-client))

(def connn (restart-conn))

(load-simple-map connn)

(do-sc2
 connn
 (fn [obs _] (def observation obs) [])
 {:run-until-fn (run-for 10)
  :stepsize 1})

(defn score [amount-unit-type-goal amount-unit-actual facts]
  (first (ds/q '[:find [(avg ?score)]
                 :with ?unit-type
                 :in [[?unit-type [?wanted ?actual]] ...] % ?clamp-at
                 :where
                 [(/ ?actual ?wanted) ?actual-over-wanted]
                 (clamp ?actual-over-wanted ?clamp-at ?score)]
               (merge (into {}
                            (map (fn [unit-type] [unit-type [1 0]])
                                 (map first amount-unit-type-goal)))
                      (into {}
                            (map (fn [[type wanted actual]] [type [wanted actual]])
                                 amount-unit-actual)))
               cljsc2.clj.rules/clamp-at-rule
               1)))

(let [facts (ds/db-with
             knowledge-base
             (obs->facts observation))
      amount-unit-goal (->>
                        (->
                         "13 SCVs"
                         (clojure.string/replace #" and" "")
                         (clojure.string/split #" "))
                        (partition 2)
                        (map (fn [[amount unit-name]]
                               (let [stripped-name (if (clojure.string/ends-with? unit-name "s")
                                                     (subs unit-name 0 (dec (count unit-name)))
                                                     unit-name)]
                                 [(Integer/parseInt amount) stripped-name]))))
      amount-unit-type-goal (ds/q '[:find ?id ?amount
                                    :in $ [[?amount ?unit-name] ...]
                                    :where
                                    [?unit-type-id :unit-type/name ?unit-name]
                                    [?unit-type-id :unit-type/unit-id ?id]
                                    ]
                                  facts
                                  amount-unit-goal)
      amount-unit-actual (ds/q '[:find ?unit-type ?wanted (count ?unit)
                                 :in $ [[?unit-type ?wanted] ...]
                                 :where
                                 [?unit :unit/unit-type ?unit-type]
                                 ]
                               facts
                               amount-unit-type-goal)
      scored (score amount-unit-type-goal amount-unit-actual facts)]
  )

;;we're gonna evolve an agent that decides what to build and when to build it, based of a goal we set ourselves we will rate how well it did.

;;We score a result by seeing how much of the stated goal has been achieved.
;;We'll try to code up a solution using the fact query mechanism ourselves first. Then we could get an overview of which parts of the solution we could arrive at using evolutionary algorithms.
;;The algorithm will take the input of the game in the form of a bunch of facts and a goal.

;;Let's pick an easy goal at first; just build one scv more than what we start with. We will get the unit type and amount from the string:

(defn get-goal-unit-types-amounts [goal facts]
  (let [amount-unit-goal (->>
                          (->
                           "13 SCVs"
                           (clojure.string/replace #" and" "")
                           (clojure.string/split #" "))
                          (partition 2)
                          (map (fn [[amount unit-name]]
                                 (let [stripped-name (if (clojure.string/ends-with? unit-name "s")
                                                       (subs unit-name 0 (dec (count unit-name)))
                                                       unit-name)]
                                   [(Integer/parseInt amount) stripped-name]))))
        amount-unit-type-goal (ds/q '[:find ?id ?amount
                                      :in $ [[?amount ?unit-name] ...]
                                      :where
                                      [?unit-type-id :unit-type/name ?unit-name]
                                      [?unit-type-id :unit-type/unit-id ?id]
                                      ]
                                    facts
                                    amount-unit-goal)]
    amount-unit-type-goal))


(let [facts (ds/db-with
             knowledge-base
             (obs->facts observation))
      goal "13 SCVs"
      goal-data (get-goal-unit-types-amounts goal facts)]
  goal-data)
;; => #{[45 13]}
;;That seems about right, 13 units of unit type 45. This should be enough pre-processing of our goal for the algorithm.

;;We need a way to refer to this data throughout our application, we can bind them to names like programmers do with variables! We can generate symbols and then use these later.

(let [facts (ds/db-with
             knowledge-base
             (obs->facts observation))
      goal "13 SCVs"
      goal-data (get-goal-unit-types-amounts goal facts)
      symbol-names (map (fn [data] (zipmap (repeatedly (count data) gensym) data))
                        goal-data)]
  symbol-names)
;; => ({G__53704 45, G__53705 13})

(let [facts (ds/db-with
             knowledge-base
             (obs->facts observation))
      goal "12 SCVs"
      goal-data (get-goal-unit-types-amounts goal facts)
      symbol-names (flatten (map (fn [data] (into [] (zipmap (repeatedly (count data) gensym) data)))
                                 goal-data))]
  `(let [~@symbol-names]
     ())
  goal-data)

(defn collect-timed-actions [observation]
  (let [facts (ds/db-with
               knowledge-base
               (obs->facts observation))
        goal "13 SCVs"
        goal-data (get-goal-unit-types-amounts goal facts)]
    (flatten (map (fn [[unit-type required-amount]]
                    (let [[unit-tag ability-id after-steps]
                          (first (ds/q '[:find ?unit-tag ?ability-id ?after-steps
                                         :in $ % ?build-unit-type
                                         :where
                                         (can-build-type ?build-unit-type ?ability-id ?unit-tag)
                                         [(+ 990000 ?build-unit-type) ?unit-e-type]
                                         [?unit-e-type :unit-type/build-time ?after-steps]]
                                       facts
                                       cljsc2.clj.rules/can-build-type-rule
                                       unit-type
                                       ))
                          timed-actions [{:after-steps after-steps :action (ability-to-action [unit-tag] ability-id)}]]
                      timed-actions))
                  goal-data))))

;;we have actions available that need to be run after a while, but those actions will be sent again when the function is run again
;;I might think I need to put the potato in the oven after it gets hot, but how would I remember to re-consider if enough time passed. The action is unique in the case of an oven, but in case of building an scv, that should be done whenever there is no queue and there is no priority on other units. Tracking what lead to a decision decides its identity. We can see in the future schedule whether action with same reasons is there, if not schedule too, need to make current plan input to fn and then make output a revised plan.
;;for now we can just schedule the action, and call new actions after a certain amount of steps or until previous actions are in effect.

(def stepsize 120)

(do-sc2
 connn
 (let [plan (atom {})]
   (fn [observation _]
     (let [actions (collect-timed-actions observation)]
       (cond
         (running-too-long observation) nil
         (or (actions-done-over-200-steps-ago plan)
             (over-500-step-since-last-plan plan))
         (do
           (swap! plan assoc :latest-plan actions)
           (swap! plan assoc :latest-plan-step plan)
           (actions-within-stepsize actions stepsize))
         :default [])
       )
     ))
 {:collect-observations true
  :stepsize stepsize})

(defn actions-within-stepsize [actions now stepsize]
  (->>
   actions
   (filter (fn [{:keys [at-step]}]
             (<= at-step (+ now stepsize))))
   (map :action)))

(do-sc2
 connn
 (let [plan (atom {})]
   (fn [observation _]
     (let [actions (collect-timed-actions observation)
           game-loop (:game-loop observation)
           latest-plan (:latest-plan @plan)]
       (cond
         (> game-loop 10000) nil
         (if latest-plan
           (- game-loop (:at-step (apply max-key :at-step (vec latest-plan))))
           true)
         (do
           (println "new-plan-addition" actions)
           (swap! plan update :latest-plan
                  (fn [plan] (concat latest-plan
                                     (map (fn [action] (dissoc (assoc action :at-step
                                                                      (+ (:after-steps action)
                                                                         game-loop))
                                                               :after-steps))
                                          actions))))
           (swap! plan assoc :latest-plan-step game-loop)
           (actions-within-stepsize (:latest-plan @plan) game-loop stepsize))
         :default [])
       )
     ))
 {:collect-observations true
  :stepsize stepsize})
