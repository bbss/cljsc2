(ns cljsc2.cljs.ui.resolution
  (:require [cljsc2.cljs.material_ui :refer [ui-button ui-paper]]
            [cljsc2.cljs.ui.fulcro :refer [input-with-label]]
            [cljsc2.cljs.ui.player_setup :refer [ui-player-setup PlayerSetup]]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.client.dom :as dom]
            [fulcro.ui.form-state :as fs]))

(defsc Resolution [this {:keys [:SC2APIProtocol.common$Size2DI/x
                                :SC2APIProtocol.common$Size2DI/y
                                :ui/editting
                                :db/id]}]
  {:query [:SC2APIProtocol.common$Size2DI/x
           :SC2APIProtocol.common$Size2DI/y
           :db/id
           :ui/editting
           fs/form-config-join]
   :ident [:resolution/by-id :db/id]
   :form-fields #{:SC2APIProtocol.common$Size2DI/x
                  :SC2APIProtocol.common$Size2DI/y}}
  (dom/div
   (if editting
     (dom/div (ui-button #js {:onClick
                              #(prim/transact! this `[(cljsc2.cljc.mutations/abort-resolution ~{:id id})])}
                          "Abort resolution")
              (ui-button #js {:onClick
                              #(prim/transact! this `[(cljsc2.cljc.mutations/submit-resolution ~{:id id
                                                                            :diff (fs/dirty-fields (prim/props this) true)})])}
                          "Save resolution"))
     (ui-button #js {:onClick #(prim/transact! this `[(cljsc2.cljc.mutations/edit-resolution ~{:id id})])}
                 "Edit resolution"))
   (if editting
     (dom/div
      (dom/p "X: ")
      (input-with-label this :SC2APIProtocol.common$Size2DI/x x
                        "Rendered horizontal pixels"
                        {:type "number"
                         :min 1
                         :max 1024
                         :style #js {:width "100px"}})
      (dom/p "Y: ")
      (input-with-label this :SC2APIProtocol.common$Size2DI/y y
                        "Rendered vertical pixels"
                        {:type "number"
                         :min 1
                         :max 1024
                         :style #js {:width "100px"}}))
     (dom/div
      (dom/p (str "X: " x))
      (dom/p (str "Y: " y))))))

(def ui-resolution (prim/factory Resolution))

(defsc SpatialCameraSetup
  [this {:keys [SC2APIProtocol.sc2api$SpatialCameraSetup/width
                SC2APIProtocol.sc2api$SpatialCameraSetup/resolution
                SC2APIProtocol.sc2api$SpatialCameraSetup/minimap-resolution
                :db/id]}]
  {:query [:SC2APIProtocol.sc2api$SpatialCameraSetup/width
           {:SC2APIProtocol.sc2api$SpatialCameraSetup/resolution (prim/get-query Resolution)}
           {:SC2APIProtocol.sc2api$SpatialCameraSetup/minimap-resolution (prim/get-query Resolution)}
           :db/id
           fs/form-config-join]
   :ident [:spatial-camera-setup/by-id :db/id]
   :form-fields #{:SC2APIProtocol.sc2api$SpatialCameraSetup/width
                  :SC2APIProtocol.sc2api$SpatialCameraSetup/resolution
                  :SC2APIProtocol.sc2api$SpatialCameraSetup/minimap-resolution}}
  (dom/div
   (dom/h4 "Minimap resolution")
   (ui-resolution minimap-resolution)
   (dom/h4 "Map resolution")
   (ui-resolution resolution)))

(def ui-spatial-camera-setup (prim/factory SpatialCameraSetup))

(defsc InterfaceOptions
  [this {:keys [:SC2APIProtocol.sc2api$InterfaceOptions/raw
                :SC2APIProtocol.sc2api$InterfaceOptions/score
                :SC2APIProtocol.sc2api$InterfaceOptions/feature-layer
                :SC2APIProtocol.sc2api$InterfaceOptions/render
                :ui/editting
                :db/id]}]
  {:query [:SC2APIProtocol.sc2api$InterfaceOptions/raw
           :SC2APIProtocol.sc2api$InterfaceOptions/score
           {:SC2APIProtocol.sc2api$InterfaceOptions/feature-layer (prim/get-query SpatialCameraSetup)}
           {:SC2APIProtocol.sc2api$InterfaceOptions/render (prim/get-query SpatialCameraSetup)}
           :ui/editting
           :db/id
           fs/form-config-join]
   :ident [:interface-options/by-id :db/id]
   :form-fields #{:SC2APIProtocol.sc2api$InterfaceOptions/raw
                  :SC2APIProtocol.sc2api$InterfaceOptions/score
                  :SC2APIProtocol.sc2api$InterfaceOptions/feature-layer
                  :SC2APIProtocol.sc2api$InterfaceOptions/render}}
  (dom/div (if editting
             (dom/div
              (ui-button #js {:onClick
                               (fn [] (prim/transact!
                                       this
                                       `[(cljsc2.cljc.mutations/abort-interface-options
                                          ~{:id id})]))}
                          "Abort raw/score")
              (ui-button #js {:onClick
                               (fn [] (prim/transact!
                                       this
                                       `[(cljsc2.cljc.mutations/submit-interface-options
                                          ~{:id id
                                            :diff (fs/dirty-fields (prim/props this) true)})]))}
                          "Save raw/score"))
             (ui-button #js {:onClick (fn [] (prim/transact! this `[(cljsc2.cljc.mutations/edit-interface-options ~{:id id})]))}
                         "Edit raw/score"))
           (if editting
             (dom/div
              #js {:style #js {:display "flex"}}
              (dom/p "Raw data: ")
              (input-with-label this :SC2APIProtocol.sc2api$InterfaceOptions/raw
                                raw "" {:type "checkbox"
                                        :checked raw})
              (dom/p "Score: ")
              (input-with-label this :SC2APIProtocol.sc2api$InterfaceOptions/score
                                score "" {:type "checkbox"
                                          :checked score}))
             (dom/div
              #js {:style #js {:display "flex"}}
              (dom/div
               (dom/p (str "Raw data: " raw))
               (dom/p (str "Score: " score)))
              ))
           (dom/div
            #js {:style #js {:display "flex"}}
            (dom/div #js {:style #js {:margin "10px"}}
             (dom/h3 "Feature layers")
             (ui-spatial-camera-setup feature-layer))
            (dom/div #js {:style #js {:margin "10px"}}
             (dom/h3"Full RGB layers")
             (ui-spatial-camera-setup render)))))

(defn update-interface-options-field [this id field]
  (fn [e]
    (prim/transact!
     this
     `[(cljsc2.cljc.mutations/update-interface-setup
        ~{:field field
          :id id
          :value (.-value (.-target e))})])))

(def ui-interface-options (prim/factory InterfaceOptions))

(defsc GameConfig [this {:keys [db/id
                                game-config/interface-options
                                game-config/player-setups]}]
  {:query [:db/id
           {:game-config/interface-options (prim/get-query InterfaceOptions)}
           {:game-config/player-setups (prim/get-query PlayerSetup)}
           fs/form-config-join]
   :ident [:game-config/by-id :db/id]
   :form-fields #{:game-config/player-setups}}
  (dom/div #js {:style #js {:display "flex"}}
           (map #(ui-player-setup %)
                (or player-setups []))
           (ui-interface-options interface-options)))

(def ui-game-config (prim/factory GameConfig))
