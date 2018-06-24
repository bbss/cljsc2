(ns cljsc2.cljs.ui.process
  (:require [fulcro.client.dom :as dom]
            [cljsc2.cljs.material_ui :refer [ui-button]]
            [cljsc2.cljs.actions :refer [ui-available-actions]]
            [cljsc2.cljs.ui.run :refer [Run RunConfig ui-run-config MapConfig]]
            [cljsc2.cljs.ui.fulcro :refer [input-with-label]]
            [cljsc2.cljs.selectors :refer [latest-observation-from-runs]]
            [cljsc2.cljs.ui.layer_selection :refer [ui-draw-sizes]]
            [cljsc2.cljs.ui.canvas_drawing :refer [ui-canvas render-screen render-minimap]]
            [cljsc2.cljs.ui.resolution :refer [ui-game-config GameConfig]]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.ui.form-state :as fs]))

(defn ui-game-info [port runs camera food-used food-cap minerals vespene]
  (dom/div
   (dom/p "current camera position "
          (str camera))
   (dom/p "Supply: " food-used "/" food-cap)
   (dom/p "Minerals: " minerals " Gas: " vespene)))

(defn close-connection [this port]
  (prim/transact!
   this
   `[(cljsc2.cljc.mutations/send-request
      ~{:port port
        :request #:SC2APIProtocol.sc2api$RequestQuit{:quit {}}
        })]))

(defn paste-first-element [text]
  (.setValue (.-CodeMirror (aget (js/document.querySelectorAll ".CodeMirror") 0)) text))
(declare ui-process)

(defsc Process [this {:keys [db/id
                             process/port
                             process/runs
                             process/savepoint-at
                             process/latest-response
                             process/game-info
                             process/run-config]}
                {:keys [knowledge-base]}]
  {:query [{:process/runs (prim/get-query Run)}
           {:process/run-config (prim/get-query RunConfig)}
           :process/port
           :process/latest-response
           :process/game-info
           :process/savepoint-at
           :db/id]
   :initLocalState (fn [] {:selected-minimap-layer-path [:render-data :minimap]
                           :selected-render-layer-path [:render-data :map]
                           })
   :componentDidUpdate (fn [_ _]
                         (let [{:keys [draw-size
                                       draw-size-minimap
                                       selected-render-layer-path
                                       selected-minimap-layer-path]}
                               (prim/get-state this)
                               runs (:process/runs (prim/props this))
                               render-size (get-in (latest-observation-from-runs runs) [:render-data :map :size])
                               minimap-size (get-in (latest-observation-from-runs runs) [:render-data :minimap :size])]
                           (render-screen this
                                          (or draw-size (-> render-size
                                                            (update :x #(* 2 %))
                                                            (update :y #(* 2 %))))
                                          selected-render-layer-path)
                           (render-minimap this
                                           (or draw-size-minimap (->  minimap-size
                                                                     (update :x #(* 2 %))
                                                                     (update :y #(* 2 %))))
                                           selected-minimap-layer-path)))
   :componentDidMount (fn []
                        (let [{:keys [draw-size
                                      draw-size-minimap
                                      selected-render-layer-path
                                      selected-minimap-layer-path]}
                              (prim/get-state this)
                              runs (:process/runs (prim/props this))
                              render-size (-> (get-in (latest-observation-from-runs runs) [:render-data :map :size])
                                              (update :x #(* 2 %))
                                              (update :y #(* 2 %)))
                              minimap-size (-> (get-in (latest-observation-from-runs runs) [:render-data :minimap :size])
                                               (update :x #(* 2 %))
                                               (update :y #(* 2 %)))]
                          (prim/set-state!
                           this
                           (merge (prim/get-state this)
                                  (when render-size {:draw-size {:x (* 2 (:x render-size))
                                                                 :y (* 2 (:y render-size))}
                                                     :draw-size-minimap {:x (* 2 (:x minimap-size))
                                                                         :y (* 2 (:y minimap-size))}} {})))
                          (ui-draw-sizes this (prim/get-state this) render-size minimap-size)
                          (render-screen this
                                         (or draw-size render-size)
                                         selected-render-layer-path)
                          (render-minimap this
                                          (or draw-size-minimap minimap-size)
                                          selected-minimap-layer-path)))
   :ident [:process/by-id :db/id]}
  (let [latest-observation (latest-observation-from-runs runs)
        {:keys [x y] :as camera} (get-in latest-observation [:raw-data :player :camera])
        {:keys [food-used food-cap vespene minerals]} (:player-common latest-observation)
        {:keys [draw-size
                draw-size-minimap
                selected-minimap-layer-path
                selected-render-layer-path
                selected-ability
                ] :as local-state} (prim/get-state this)
        render-size (get-in latest-observation [:render-data :map :size])
        minimap-size (get-in latest-observation [:render-data :minimap :size])
        game-loop (:game-loop latest-observation)]
    (dom/div
     (ui-button #js {:style #js {"float" "right"}
                      :onClick #(close-connection this port)} "Close process")
     (ui-available-actions
      (:abilities latest-observation)
      knowledge-base
      selected-ability
      (fn [id ability-name requires-point]
        (prim/set-state! this (merge
                               local-state
                               {:selected-ability {:ability-id id
                                                   :ability-name ability-name
                                                   :requires-point requires-point}}))))
     (str selected-ability)
     (ui-run-config
      (prim/computed run-config
                     {:process/runs runs
                      :process/savepoint-at savepoint-at
                      :process/game-loop game-loop
                      :process/make-savepoint #(prim/transact!
                                                this
                                                `[(cljsc2.cljc.mutations/make-savepoint ~{:port port
                                                                    :game-loop game-loop})])
                      :process/load-savepoint #(prim/transact!
                                                this
                                                `[(cljsc2.cljc.mutations/load-savepoint ~{:port port})])}))
     (ui-button
      #js {:onClick
           (fn [e]
             (paste-first-element
              (str '(execute-plans))))} "Add inactive run")
     (ui-button
      #js {:onClick
           (fn [e]
             (paste-first-element
              (str '(execute-plans
                     (build "SCV" :until-count 25)
                     (build "SupplyDepot")
                     (build "Barracks" :until-count 5)
                     (build "Marine" :until-count 1000)
                     (select "Marine" :at-least 10
                             :whenever-goals-succeed
                             (add-plan (attack :at-location :enemy-base)))
                     ))))} "Add 5rax Marine build")
     (ui-button
      #js {:onClick
           (fn [e]
             (paste-first-element
              (str '(execute-plans
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
                     (camera-follow-army)))))} "Add marine/cyclones build")
     (ui-game-info port runs camera food-used food-cap minerals vespene)
     (ui-canvas this local-state port draw-size-minimap draw-size render-size
                minimap-size selected-ability selected-minimap-layer-path selected-render-layer-path x y ui-process))))

(def ui-process (prim/factory Process {:keyfn :db/id}))

(defsc ProcessStarter
  [this {:keys [process-starter/available-maps
                process-starter/game-config
                process-starter/map-config]} {:keys [processes]}]
  {:query [:process-starter/available-maps
           {:process-starter/map-config (prim/get-query MapConfig)}
           {:process-starter/game-config (prim/get-query GameConfig)}
           fs/form-config-join]
   :form-fields #{:process-starter/game-config}}
  (dom/div
   (dom/h4
    "New connections will load map: "
    (let [map (:map-config/path map-config)
            i (clojure.string/last-index-of
               (:map-config/path map-config) "/")
            map-name (subs map (inc i))]
      map-name))
   (dom/select
    #js {:value (:map-config/path map-config)
         :onChange (fn [e]
                     (prim/transact! this `[(cljsc2.cljc.mutations/update-map ~{:id (:db/id map-config)
                                                          :path (.-value (.-target e))})]))}
    (map (fn [{:keys [absolute-path file-name]}]
           (dom/option #js {:key absolute-path
                            :value absolute-path}
                       file-name))
         available-maps))
   (when (seq game-config) (ui-game-config game-config))))

(def ui-process-starter (prim/factory ProcessStarter))
