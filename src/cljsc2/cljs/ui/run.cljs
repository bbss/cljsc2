(ns cljsc2.cljs.ui.run
  (:require [d3 :as d3]
            [fulcro.client.dom :as dom]
            [cljsc2.cljs.material_ui :refer [ui-button]]
            [cljsc2.cljs.ui.starcraft :refer [Observation ui-observation]]
            [cljsc2.cljs.ui.fulcro :refer [input-with-label]]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.ui.form-state :as fs]))

(defn ui-timeline [runs run-size step-size]
  (let [scale (d3.scaleLinear)
        total-runs-size (reduce (fn [total {:keys [:run/started-at :run/ended-at]}]
                                  (+ total (- (or ended-at 0) (or started-at 0))))
                                0
                                runs)]
    (doto scale
      (.domain #js [0 (+ total-runs-size run-size)])
      (.range #js [0 400]))
    (apply (partial dom/div #js {:style #js {:margin "10px"
                                             :display "flex"
                                             :width "400px"
                                             :height "4px"
                                             :justifyContent "flex-end"
                                             :boxShadow "0 3px 6px rgba(0,0,0,0.16), 0 3px 6px rgba(0,0,0,0.23)"
                                             }})
           (concat
            (map (fn [{:keys [run/started-at run/ended-at] :as run}]
                   (dom/div #js {:style #js {:width (str (- (scale (or ended-at
                                                                       total-runs-size))
                                                            (scale started-at)) "px")
                                             :height "6px"
                                             :borderBottom "1px solid black"
                                             :borderRight "1px solid black"}}))
                 runs)
            [(apply (partial dom/div #js {:style #js {:display "flex"
                                                           :width (str (- (scale (+ total-runs-size run-size))
                                                                          (scale total-runs-size)) "px")
                                                           :height "6px"
                                                           :borderBottom "1px dashed green"
                                                           :borderRight "1px solid green"
                                                           }})
                         (map (fn [step] (dom/div #js {:style
                                                       #js {:width (str (- (scale step)
                                                                           (scale (- step step-size))) "px")
                                                            :borderRight "1px solid orange"
                                                            :height "4px"}}))
                              (take (scale run-size)
                                    (range total-runs-size
                                           (+ total-runs-size run-size)
                                           step-size))))]))))


(defsc RunConfig [this {:keys [db/id
                               run-config/step-size
                               run-config/run-size
                               run-config/restart-on-episode-end
                               ui/editting]}
                  {:keys [process/runs
                          process/game-loop
                          process/savepoint-at
                          process/make-savepoint
                          process/load-savepoint]}]
  {:query [:db/id
           :ui/editting
           :run-config/step-size
           :run-config/run-size
           :run-config/restart-on-episode-end
           fs/form-config-join]
   :ident [:run-config/by-id :db/id]
   :form-fields #{:run-config/step-size :run-config/run-size
                  :run-config/restart-on-episode-end}}
  (dom/div
   (ui-button #js {:onClick #(make-savepoint)}
               (str "Set the time-travel savepoint at " game-loop))
   (when savepoint-at
     (ui-button #js {:onClick #(load-savepoint)}
                 (str "Load savepoint at game-loop " savepoint-at)))
   (when editting
     (dom/div
              (ui-button #js {:onClick #(prim/transact!
                                          this
                                          `[(cljsc2.cljc.mutations/abort-run-config ~{:id id})])}
                          "Abort")
              (ui-button #js {:onClick #(prim/transact!
                                          this
                                          `[(cljsc2.cljc.mutations/submit-run-config
                                             ~{:id id
                                               :diff (fs/dirty-fields (prim/props this) true)})])}
                          "Save")))
   (if editting
     (dom/div
      #js {:style #js {:display "flex"}}
      (dom/div "Step size: ")
      (input-with-label this :run-config/step-size step-size
                        "Step size for the run should be a whole number"
                        {:type "number"
                         :min 1
                         :style #js {:width "100px"}})
      (dom/div "Run size :")
      (input-with-label this :run-config/run-size run-size
                        "Run size for the run should be a whole number"
                        {:type "number"
                         :min 1
                         :style #js {:width "100px"}})
      (dom/div "Restart ended episodes and keep running:")
      (input-with-label this :run-config/restart-on-episode-end restart-on-episode-end
                        "Run size for the run should be a whole number"
                        {:type "checkbox"
                         :checked restart-on-episode-end
                         :style #js {:width "100px"}}))
     (ui-button #js {:onClick (fn [_] (prim/transact! this `[(cljsc2.cljc.mutations/edit-run-config ~{:id id})]))}
                 "Adjust run settings"))
   (ui-timeline runs
                run-size
                step-size)))

(def ui-run-config (prim/factory RunConfig))

(defsc Run [this {:keys [db/id :run/observations]}]
   {:query [{:run/observations (prim/get-query Observation)}
            {:run/run-config (prim/get-query RunConfig)}
            :run/ended-at
            :run/started-at
            :db/id]
    :ident [:run/by-id :db/id]})

(defsc MapConfig [this _]
   {:query [:map-config/path :db/id]
    :ident [:map-config/by-id :db/id]})
