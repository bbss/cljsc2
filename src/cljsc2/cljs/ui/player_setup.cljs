(ns cljsc2.cljs.ui.player_setup
  (:require [cljsc2.cljs.material_ui :refer [ui-button ui-paper]]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.client.dom :as dom]
            [fulcro.ui.form-state :as fs]))

(defn update-player-setup-field [this id field]
  (fn [e]
    (prim/transact!
     this
     `[(cljsc2.cljc.mutations/update-player-setup ~{:field field
                              :id id
                              :value (.-value (.-target e))})])))

(defsc PlayerSetup [this {:keys [:SC2APIProtocol.sc2api$PlayerSetup/difficulty
                                 :SC2APIProtocol.sc2api$PlayerSetup/type
                                 :SC2APIProtocol.sc2api$PlayerSetup/race
                                 :ui/editting
                                 :db/id]}]
  {:query [:SC2APIProtocol.sc2api$PlayerSetup/difficulty
           :SC2APIProtocol.sc2api$PlayerSetup/type
           :SC2APIProtocol.sc2api$PlayerSetup/race
           :ui/editting
           :db/id
           fs/form-config-join]
   :ident [:player-setup/by-id :db/id]
   :form-fields #{:SC2APIProtocol.sc2api$PlayerSetup/race}}
  (dom/div
   (if editting
     (dom/div (ui-button #js {:onClick (fn [] (prim/transact! this `[(cljsc2.cljc.mutations/abort-player-setup ~{:id id})]))}
                          "Abort player setup")
              (ui-button #js {:onClick (fn [] (prim/transact! this `[(cljsc2.cljc.mutations/submit-player-setup ~{:id id
                                                                                             :diff (fs/dirty-fields (prim/props this) true)})]))}
                          "Save player setup"))
     (ui-button #js {:onClick (fn [] (prim/transact! this `[(cljsc2.cljc.mutations/edit-player-setup ~{:id id})]))}
                 "Edit player setup"))
   (if editting
     (dom/div
      (dom/select #js {:value type
                       :onChange (update-player-setup-field this id :SC2APIProtocol.sc2api$PlayerSetup/type)}
                  (for [type ["Participant" "Computer"]]
                    (dom/option #js {:key type
                                     :value type}
                                type)))
      (dom/p "Race:")
      (dom/select #js {:value race
                       :onChange (update-player-setup-field this id :SC2APIProtocol.sc2api$PlayerSetup/race)}
                  (for [race ["Protoss" "Terran" "Zerg" "Random"]]
                    (dom/option #js {:key race
                                     :value race}
                                race)))
      (when (= type "Computer")
        (dom/div
         (dom/p "Difficulty")
         (dom/select
          #js {:value difficulty
               :onChange (update-player-setup-field this id :SC2APIProtocol.sc2api$PlayerSetup/difficulty)}
          (map (fn [v]
                 (dom/option #js {:key v
                                  :value v}
                             v))
               ["VeryEasy" "Easy" "Medium" "MediumHard" "Harder" "Hard"
                "VeryHard" "CheatVision" "CheatMoney" "CheatInsane"])))))
     (dom/div (dom/p "Race: " race)
              (when difficulty (dom/p "Difficulty: " difficulty))
              (dom/p "Type: " type)))))

(def ui-player-setup (prim/factory PlayerSetup {:keyfn :db/id}))
