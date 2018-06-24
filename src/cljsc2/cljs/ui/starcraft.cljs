(ns cljsc2.cljs.ui.starcraft
  (:require [fulcro.client.primitives :as prim :refer [defsc]]))

(defsc Observation [this _]
  {:query [{:feature-layer-data [{:minimap-renders []}
                                 {:renders []}]}
           {:render-data [:map :minimap]}
           :abilities
           :score
           :game-loop
           :db/id
           {:player-common [:minerals :vespene :food-used :food-cap]}
           {:raw-data [{:player [:camera]} :units]}
           {:abilities [:ability-id :requires-point]}]
   :ident [:observation/by-id :db/id]})

(def ui-observation (prim/factory Observation {:keyfn :game-loop}))
