(ns cljsc2.clj.build-order-test
  (:require
   [datascript.core :as ds])
  (:use
   cljsc2.clj.core
   cljsc2.clj.rules
   cljsc2.clj.notebook.core))

(def planning (atom {}))

(def conn (get-conn server-db 5000))

(load-simple-map conn)

(defn build
  ([unit] (build unit {}))
  ([unit config]
   ;;do -s from string
   ;;classcase, name on keyword
   ;;building scvs until a certain limit
   ;;not filling queue over 1
   ;; not too many at same time
   ;; one in the start
   ;; not when there is a big difference between food and cap
   ;; selecting builders from pool to avoid conflicting commands
   ;; decide between build new and wait for queue to finish
   ;; make sure build x command does not stack
   ))

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


;;Let's make some useful abstractions for creating a basic rule based AI
;;We need a system that can operate on some basic heuristics like
;;"build workers"
;;"build an army"
;;"prioritize workers over army until x per base have been built"
;;We further want to considers certain potential inefficiencies such as
;;using up resources unusefully (like having many built units in a queue)
;;There are more heuristics that we'll talk about later, let's first think of
;;a system that can do these.
;;In AI research there is the concept of classical planning. Which talks about:
;; - The state of an environment.
;; - Actions that are possible in this environment.
;; - The results the actions have on the state when made (called effects)
;; - The requirements and costs for an action.
;; - A goal to reach
;;With this information a planner algorithm has all it needs to search for a
;;desirable resulting state.
;;Let's pick the example of building a marine.
;;The state of the world we can pick in this case from the raw data of the game.
;;The potential actions we'll hand-pick and we'll write a function to consider its effects and costs.

(def plan (atom {}))

((defn build [build-unit-type]
   (let [])))
